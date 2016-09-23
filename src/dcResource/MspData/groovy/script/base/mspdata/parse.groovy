#!/usr/bin/env groovy
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.lang.Long;
import java.lang.Float;
import java.lang.Integer;

import com.iflytek.sid.SidParse;
import com.iflytek.geoip.model.AreaVo;
import com.iflytek.geoip.service.IPmem;
import com.iflytek.xmlparser.NlpXmlPaser;
import com.iflytek.model.msp.app.VoicePlus;
import com.iflytek.model.msp.app.VoiceAssistant;
import com.iflytek.model.customize.Locate;
import com.iflytek.model.customize.Locate.Location;
import com.iflytek.model.customize.MediaParse;
import com.iflytek.model.msplogex.Utils;

import com.iflytek.dc.util.ReadUtils;

import com.iflytek.dm.lib.Amr;
import com.iflytek.dm.model.DataAnalysisInfo.Age;
import com.iflytek.dm.model.DataAnalysisInfo.Gender;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;


/*
** 任何进入脚本函数的过程，表示该字段还没有取值，或者需要重置。
** 无需再次判断是否存在。
** 以下场景都是针对一次会话：
**
** track implememts java.util.set,具有set接口的查看和add操作，
** 为了安全起见，remove等操作不支持。
** 表示model文件中的字段是否已经获取。如果获取了，确保在返回值
** 的同时调用 track.add(key)方法,一方面防止值被覆盖，另一方面增
** 加性能。
** 
** collection implements java.util.map,具有map接口的查看和get操作，
** 为了安全起见，remove、add等操作不支持。
** 表示model文件中字段对应的值，该值由上层框架根据 该脚本方法返回
** 的值进行填充。
**
**
** context : java.util.HashMap
** context是一个比较灵活的对象，可以向其中加入临时信息，这不仅表现
** 在一个组件打印的日志中，主要是表现在跨组件的过程。context从一个
** 会话的开始被定义，一直到会话的所有日志都处理完才被销毁，所以用户
** 可尽情的操作，但是需要注意的是，map的key需要有一定的命名特性以确
** 保唯一，如 取 sid，那么 在get_sid函数中，可能保持的中间值的key
** 应该为sid_***的形式。
**
** log 为 组件打印的日志，一个组件打印的日志存在多条，log为其中一条
** 日志，对于一个组件的日志，框架必定是一起遍历，但是顺序无法保证。
** log只提供了 get(key) 接口。
*/

/*
 * @author 		bingli3
 * @brief		session id(unique)
 * @rule		extract from context(frame init).
 * @depend  	
 * @been-depend eng_ip, mss_ip, s_city, sub, timestamp, regtime, appid
*/
def get_sid(track, collection, context, log){
	sid = context.get("sid");
	if (!StringUtils.isEmpty(sid)){
		track.add("sid");
		return sid;
	}
	return;
}

/*
 * @author 		bingli3
 * @brief		application id
 * @rule		from msslog, key is appid or app_id which sub is sts.
 * @depend      sid
 * @been-depend va, vp, desc
*/
def get_appid(track, collection, context, log){
	if ("msslog".equals(context.get("logtype"))){
		sid = context.get("sid");
		appid = null;
		if (!StringUtils.isEmpty(sid) && sid.startsWith("sts")){
			appid = log.get("app_id");
		} else {
			appid = log.get("appid");	
		}
		if (!StringUtils.isEmpty(appid)){
			track.add("appid");
			return appid;
		}
	}
	return ;
}

/*
 * @author 		bingli3
 * @brief		user id
 * @rule		from msslog, key is uid, extract the context last appearance.
 * @depend      
 * @been-depend 
*/
def get_uid(track, collection, context, log){
	if ("msslog".equals(context.get("logtype"))){
		uid = log.get("uid");
		tmpcts = new Long(context.get("calllogtimestamp"));
		if (!StringUtils.isEmpty(uid)){
			cts = context.get("for_uid_ctimestamp");
			if (cts == null || tmpcts >= cts){
				context.put("uid", uid);
				context.put("for_uid_ctimestamp", tmpcts);
			}
		}
	}

	if (context.get("ispostprocess")){
		uid = context.get("uid");
		if (!StringUtils.isEmpty(uid)){
			track.add("uid");
			return uid;
		}
	}
	return ;
}

/*
 * @author 		bingli3
 * @brief		client internet protocol(ip)
 * @rule		from msslog, key is client_ip
 * @depend      
 * @been-depend city, province, country, isp, operator
*/
def get_client_ip(track, collection, context, log){
	if ("msslog".equals(context.get("logtype"))){
		client_ip = log.get("client_ip");
		if (!StringUtils.isEmpty(client_ip)){
			track.add("client_ip");
			return client_ip;
		}
	}
	return ;
}

/*
 * @author 		bingli3
 * @brief		audio rate and bits(16K,16bits)
 * @rule		from msslog, key is auf
 * @depend      
 * @been-depend auf_rate
*/
def get_auf(track, collection, context, log){
	if ("msslog".equals(context.get("logtype"))){
		auf = log.get("auf");
		if (!StringUtils.isEmpty(auf)){
			auf = auf.toLowerCase();//trans to low case.
			track.add("auf");
			return auf;
		}
	}
	return ;
}

/*
 * @author 		bingli3
 * @brief		audio rate(16K)
 * @rule		from msslog, parse from auf(audio/l16;rate=16000)
 * @depend      auf
 * @been-depend age, gender
*/
def get_auf_rate(track, collection, context, log){
	if (track.contains("auf")){
		auf = collection.get("auf").split("=");
		auf_rate = auf.length == 2 ? auf[1] : "";
		track.add("auf_rate");
		return auf_rate;
	}
	return ;
}

/*
 * @author 		bingli3
 * @brief		audio decode
 * @rule		from msslog, key is aue
 * @depend      
 * @been-depend age, gender
*/
def get_aue(track, collection, context, log){
	if ("msslog".equals(context.get("logtype"))){
		aue = log.get("aue");
		if (!StringUtils.isEmpty(aue)){
			track.add("aue");
			return aue;
		}
	}
	return ;
}

/*
 * @author 		bingli3
 * @brief		client version
 * @rule		from msslog, key is cver
 * @depend      
 * @been-depend 
*/
def get_cver(track, collection, context, log){
	if ("msslog".equals(context.get("logtype"))){
		cver = log.get("cver");
		if (!StringUtils.isEmpty(cver)){
			track.add("cver");
			return cver;
		}
	}
	return ;
}

//caller_name在日志中对应的Key为caller.name
/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_caller_name(track, collection, context, log){
	if ("msslog".equals(context.get("logtype"))){
		caller_name = log.get("caller.name");
		if (caller_name != null){
			track.add("caller_name");
			return caller_name;
		}
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_domain(track, collection, context, log){
	if ("msslog".equals(context.get("logtype"))){
		domain = log.get("domain");
		if (domain != null){
			track.add("domain");
			return domain;
		}
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_ent(track, collection, context, log){
	if ("msslog".equals(context.get("logtype"))){
		ent = log.get("ent");
		if (ent != null){
			track.add("ent");
			return ent;
		}
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_nbest(track, collection, context, log){
	if ("msslog".equals(context.get("logtype"))){
		nbest = log.get("nbest");
		if (nbest != null){
			track.add("nbest");
			nbest = Integer.parseInt(nbest);
			return nbest;
		}
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_prs(track, collection, context, log){
	if ("msslog".equals(context.get("logtype"))){
		prs = log.get("prs");
		if (prs != null){
			track.add("prs");
			prs = Integer.parseInt(prs);
			return prs;
		}
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_ptt(track, collection, context, log){
	if ("msslog".equals(context.get("logtype"))){
		ptt = log.get("ptt");
		if (ptt != null){
			track.add("ptt");
			ptt = Integer.parseInt(ptt);
			return ptt;
		}
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_rse(track, collection, context, log){
	if ("msslog".equals(context.get("logtype"))){
		rse = log.get("rse");
		if (rse != null){
			track.add("rse");
			return rse;
		}
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_rst(track, collection, context, log){
	if ("msslog".equals(context.get("logtype"))){
		rst = log.get("rst");
		if (rst != null){
			track.add("rst");
			return rst;
		}
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_sch(track, collection, context, log){
	if ("msslog".equals(context.get("logtype"))){
		sch = log.get("sch");
		if (!StringUtils.isEmpty(sch)){
			track.add("sch");
			sch = Integer.parseInt(sch.substring(0, 1));
			return sch;
		}
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_scn(track, collection, context, log){
	if ("msslog".equals(context.get("logtype"))){
		scn = log.get("scn");
		if (scn != null){
			track.add("scn");
			return scn;
		}
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_sent(track, collection, context, log){
	if ("msslog".equals(context.get("logtype"))){
		sent = log.get("sent");
		if (sent != null){
			track.add("sent");
			return sent;
		}
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend regtime, ivd
*/
def get_sub_ntt(track, collection, context, log){
	if ("msslog".equals(context.get("logtype"))){
		sub_ntt = log.get("net_subtype");
		if (sub_ntt != null){
			track.add("sub_ntt");
			return sub_ntt;
		}
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_vcn(track, collection, context, log){
	vcn = log.get("vcn");
	if (vcn != null){
		track.add("vcn");
		return vcn;
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_spd(track, collection, context, log){
	spd = log.get("spd");
	if (spd != null){
		track.add("spd");
		return spd;
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_txt_len(track, collection, context, log){
	txt_len = log.get("synth_text_len");
	if (txt_len != null){
		track.add("txt_len");
		txt_len = Integer.parseInt(txt_len);
		return txt_len;
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_sn(track, collection, context, log){
	if ("lgi".equals(context.get("biztype")) ||
		"ath".equals(context.get("biztype"))){
		if ("msslog".equals(context.get("logtype"))){
			sn = log.get("sn");
			if (!StringUtils.isEmpty(sn)){
				track.add("sn");
				return sn;
			}
		}
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_imei(track, collection, context, log){
	if ("msslog".equals(context.get("logtype"))){
		val = null;
		if (log.containsKey("imei")){
			val = log.get("imei");
		} else if (log.containsKey("os.imei")){
			val = log.get("os.imei");
		} else if (log.containsKey("vaimei")){
			val = log.get("vaimei");
		}
		if (val != null) {
			track.add("imei");
			return val;
		}
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_imsi(track, collection, context, log){
	if ("msslog".equals(context.get("logtype"))){
		val = null;
		if (log.containsKey("imsi")){
			val = log.get("imsi");
		} else if (log.containsKey("os.imsi")){
			val = log.get("os.imsi");
		} else if (log.containsKey("vaimsi")){
			val = log.get("vaimsi");
		}
		if (val != null) {
			track.add("imsi");
			return val;
		}
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_mac(track, collection, context, log){
	if ("msslog".equals(context.get("logtype"))){
		val = null;
		if (log.containsKey("mac")){
			val = log.get("mac");
		} else if (log.containsKey("os.mac")){
			val = log.get("os.mac");
		} else if (log.containsKey("net.mac")){
			val = log.get("net.mac");
		}
		if (val != null) {
			track.add("mac");
			return val;
		}
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_ntt(track,  collection, context, log){
	if ("msslog".equals(context.get("logtype"))){
		val = null;
		if (log.containsKey("ntt")){
			val = log.get("ntt");
		} else if (log.containsKey("net_type")){
			val = log.get("net_type");
		} else if (log.containsKey("nettype")){
			val = log.get("nettype");
		} else if (log.containsKey("wapproxy")){
			val = log.get("wapproxy");
		} else if (log.containsKey("wap_proxy")){
			val = log.get("wap_proxy");
		} else if (log.containsKey("vaapn")){
			val = log.get("vaapn");
		}
		if (val != null) {
			track.add("ntt");
			return val;
		}
	}
	return ;
}	

/*
 * @author 		bingli3
 * @brief		engine internet protocol(ip)
 * @rule		from parse session id.
 * @depend      sid
 * @been-depend 
*/
def get_eng_ip(track,  collection, context, log){
	if (track.contains("sid")){
		sid = collection.get("sid");
		track.add("eng_ip");
		return SidParse.sidToEngIp(sid);
	}
	return ;
}

/*
 * @author 		bingli3
 * @brief		mss internet protocol(ip)
 * @rule		from parse session id.
 * @depend      sid
 * @been-depend 
*/
def get_mss_ip(track,  collection, context, log){
	if (track.contains("sid")){
		sid = collection.get("sid");
		track.add("mss_ip");
		return SidParse.sidToMssIp(sid);
	}
	return ;
}

/*
 * @author 		bingli3
 * @brief		server city (hf, gz, bj or li cluster)
 * @rule		parse from sid
 * @depend      sid
 * @been-depend 
*/
def get_s_city(track,  collection, context, log){
	if (track.contains("sid")){
		sid = collection.get("sid");
		track.add("s_city");
		return SidParse.sidToArea(sid);
	}
	return ;
}

/*
 * @author 		bingli3
 * @brief		bussiness type(like iat, hcr, wfr, ivp)
 * @rule		parse from sid
 * @depend      sid
 * @been-depend 
*/
def get_sub(track, collection, context, log){
	if (track.contains("sid")){
		sid = collection.get("sid");
		track.add("sub");
		return sid.substring(0, 3);
	}
	return ;
}

/*
 * @author 		bingli3
 * @brief		session city
 * @rule		parse from client_ip or valong, valat
 * @depend      client_ip, va
 * @been-depend 
*/
def get_city(track, collection, context, log){
	if (track.contains("va") && !context.containsKey("location_for_city_province_country_isp")){
		//if (!context.containsKey("location_for_city_province_country_isp")){
		va = collection.get("va");
		location = Locate.setAreaByLongitudeAndLatitude(va.get("valong"),
			va.get("valat"), Locate.defClusterPoicodePath, Locate.defLocalPoicodePath);
		//在本次log中缓存location信息
		if ( location != null){
			context.put("location_for_city_province_country_isp", location);	
		}
		//}
		/*location = context.get("location_for_city_province_country_isp");
		if (location != null){
			track.add("city");
			return location.getCity();
		}*/
	}

	if (track.contains("client_ip") && !context.containsKey("av_for_city_province_country_isp")){
		//if (!context.containsKey("av_for_city_province_country_isp")){
		client_ip = collection.get("client_ip");
		av = IPmem.get().getAreaInfo(IPmem.get().loopUp(client_ip));
		//在本次log中缓存地域信息
		if (av != null){
			context.put("av_for_city_province_country_isp", av);
		}
		//}
		/*av = context.get("av_for_city_province_country_isp");
		if ( av != null){
			track.add("city");
			return av.getCity();
		}*/
	}

	if (context.get("ispostprocess")){
		location = context.get("location_for_city_province_country_isp");
		av = context.get("av_for_city_province_country_isp");
		track.add("city");
		if ( location == null && av == null){
			return null;
		} else if (location == null && av != null){
			return av.getCity();
		} else if (location != null && av == null){
			return location.getCity();
		} else {
			loWeight = 0;
			avWeight = 0;
			if (location.getCity() != null && !"".equals(location.getCity())){
				loWeight += 1;
			}
			if (location.getProvince() != null && !"".equals(location.getProvince())){
				loWeight += 1;
			}
			if (av.getCity() != null && !"".equals(av.getCity())){
				avWeight += 1;
			}
			if (av.getProvince() != null && !"".equals(av.getProvince())){
				avWeight += 1;
			}
			if (avWeight > loWeight){
				return av.getCity();
			} else if (avWeight <= loWeight){
				return location.getCity();
			}
		}
	}
	return ;
}

/*
 * @author 		bingli3
 * @brief		session country
 * @rule		parse from client_ip or valong, valat
 * @depend      client_ip, va
 * @been-depend 
*/
def get_country(track, collection, context, log){
	if (track.contains("va") && !context.containsKey("location_for_city_province_country_isp")){
		va = collection.get("va");
		location = Locate.setAreaByLongitudeAndLatitude(va.get("valong"),
			va.get("valat"), Locate.defClusterPoicodePath, Locate.defLocalPoicodePath);
		//在本次log中缓存location信息
		if (location != null){
			context.put("location_for_city_province_country_isp", location);	
		}
	}

	if (track.contains("client_ip") && !context.containsKey("av_for_city_province_country_isp")){
		client_ip = collection.get("client_ip");
		av = IPmem.get().getAreaInfo(IPmem.get().loopUp(client_ip));
		//在本次log中缓存地域信息
		if (av != null){
			context.put("av_for_city_province_country_isp", av);
		}
	}

	if (context.get("ispostprocess")){
		location = context.get("location_for_city_province_country_isp");
		av = context.get("av_for_city_province_country_isp");
		track.add("country");
		if ( location == null && av == null){
			return null;
		} else if (location == null && av != null){
			return av.getCountry();
		} else if (location != null && av == null){
			return location.getCountry();
		} else {
			loWeight = 0;
			avWeight = 0;
			if (location.getCity() != null && !"".equals(location.getCity())){
				loWeight += 1;
			}
			if (location.getProvince() != null && !"".equals(location.getProvince())){
				loWeight += 1;
			}
			if (av.getCity() != null && !"".equals(av.getCity())){
				avWeight += 1;
			}
			if (av.getProvince() != null && !"".equals(av.getProvince())){
				avWeight += 1;
			}
			if (avWeight > loWeight){
				return av.getCountry();
			} else if (avWeight <= loWeight){
				return location.getCountry();
			}
		}
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend      client_ip
 * @been-depend 
*/
def get_operator(track, collection, context, log){
	if (track.contains("client_ip")){
		if (!context.containsKey("av_for_city_province_country_isp")){
			client_ip = collection.get("client_ip");
			av = IPmem.get().getAreaInfo(IPmem.get().loopUp(client_ip));
			if (av != null){
				context.put("av_for_city_province_country_isp", av);
			}
		}
		av = context.get("av_for_city_province_country_isp");
		if ( av != null){
			track.add("operator");
			return av.getIsp();
		}
	}
	return ;
}

/*
 * @author 		bingli3
 * @brief		session province
 * @rule		parse from client_ip or valong, valat
 * @depend      client_ip, va
 * @been-depend 
*/
def get_province(track, collection, context, log){
	if (track.contains("va") && !context.containsKey("location_for_city_province_country_isp")){
		va = collection.get("va");
		location = Locate.setAreaByLongitudeAndLatitude(va.get("valong"),
			va.get("valat"), Locate.defClusterPoicodePath, Locate.defLocalPoicodePath);
		//在本次log中缓存location信息
		if (location != null){
			context.put("location_for_city_province_country_isp", location);	
		}
	}

	if (track.contains("client_ip") && !context.containsKey("av_for_city_province_country_isp")){
		client_ip = collection.get("client_ip");
		av = IPmem.get().getAreaInfo(IPmem.get().loopUp(client_ip));
		//在本次log中缓存地域信息
		if (av != null){
			context.put("av_for_city_province_country_isp", av);
		}
	}

	if (context.get("ispostprocess")){
		location = context.get("location_for_city_province_country_isp");
		av = context.get("av_for_city_province_country_isp");
		track.add("province");
		if ( location == null && av == null){
			return null;
		} else if (location == null && av != null){
			return av.getProvince();
		} else if (location != null && av == null){
			return location.getProvince();
		} else {
			loWeight = 0;
			avWeight = 0;
			if (location.getCity() != null && !"".equals(location.getCity())){
				loWeight += 1;
			}
			if (location.getProvince() != null && !"".equals(location.getProvince())){
				loWeight += 1;
			}
			if (av.getCity() != null && !"".equals(av.getCity())){
				avWeight += 1;
			}
			if (av.getProvince() != null && !"".equals(av.getProvince())){
				avWeight += 1;
			}
			if (avWeight > loWeight){
				return av.getProvince();
			} else if (avWeight <= loWeight){
				return location.getProvince();
			}
		}
	}
	return ;
}

/*
 * @author 		bingli3
 * @brief		nginx internet protocol(ip)
 * @rule		from msslog, and callname equals auw, key is nginx_ip
 * @depend
 * @been-depend 
*/
def get_nginx_ip(track, collection, context, log){
	if ("auw".equals(context.get("callname")) && log.containsKey("nginx_ip")){
		nginx_ip = log.get("nginx_ip").split(":")[0];
		track.add("nginx_ip");
		return nginx_ip;
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend ni, bizSubType, bizType, bizSource, focus
*/
def get_bizResult(track, collection, context, log){
    if ("ldeal".equals(context.get("callname")) && log.containsKey("biz_result")){
        track.add("biz_result");
        result = log.get("biz_result");
        //由于该信息包含\r\n\t等分隔符，对后处理较为麻烦，除去这些分隔符
        result = result.replaceAll("[\r\n\t]+", "");
        track.add("bizResult");
        return result;
    }
    return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend ni
*/
def get_nlpResult(track, collection, context, log){
	if ("tluarpcs".equals(context.get("callname")) && "nlic".equals(log.get("cn")) && 
		"NLICInterpret".equals(log.get("fn")) && log.containsKey("result")){
		nlpResult = log.get("result");
		//由于该信息包含\r\n\t等分隔符，对后处理较为麻烦，除去这些分隔符
		nlpResult = nlpResult.replaceAll("[\r\n\t]+", "");
		track.add("nlpResult");
		return nlpResult;
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_channelId(track, collection, context, log){
	if (!"msslog".equals(context.get("logtype"))) return ;
	
	val = null;
	if (log.containsKey("vadownfrom")){
		val = log.get("vadownfrom");
	} else if (log.containsKey("channel.id")){
		val = log.get("channel.id");
	} else if (log.containsKey("channelid")){
		val = log.get("channelid");
	} else if (log.containsKey("downfrom")){
		val = log.get("downfrom");
	}

	if (val != null) {
		track.add("channelId");
		return val;
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_appVer(track, collection, context, log){
	if (!"msslog".equals(context.get("logtype"))) return ;

	val = null;
	if (log.containsKey("vaclientver")){
		val = log.get("vaclientver");
	} else if (log.containsKey("caller.ver.code")){
		val = log.get("caller.ver.code");
	}

	if (val != null) {
		track.add("appVer");
		return val;
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_osModel(track, collection, context, log){
	if (!"msslog".equals(context.get("logtype"))) return ;
	osModel = log.get("os.model");
	if (osModel != null){
		track.add("osModel");
		return osModel;
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_osResolution(track, collection, context, log){
	if (!"msslog".equals(context.get("logtype"))) return ;
	osResolution = log.get("os.resolution");
	if (osResolution != null){
		track.add("osResolution");
		return osResolution;
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_osDensity(track, collection, context, log){
	if (!"msslog".equals(context.get("logtype"))) return ;
	osDensity = log.get("os.density");
	if (osDensity != null){
		track.add("osDensity");	
		return osDensity;
	}
	return ;
}

//os.system and os.release appear together
/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_osFullSystem(track, collection, context, log){
	system = context.get("for_osFullSystem_os.system");
	release = context.get("for_osFullSystem_os.release");
	if (!StringUtils.isEmpty(system) && !StringUtils.isEmpty(release)){
		track.add("osFullSystem");
		return system + release;
	} else if (context.get("ispostprocess")){
		if (!StringUtils.isEmpty(system)){
			track.add("osFullSystem");
			if (!StringUtils.isEmpty(release)){
				return system + release;
			} else {
				return system;
			}
		}
	}

	if (!"msslog".equals(context.get("logtype"))) return ;
	if (log.containsKey("os.system") && StringUtils.isEmpty(system)){
		system = log.get("os.system");
		if (!StringUtils.isEmpty(system))
			context.put("for_osFullSystem_os.system", system);
	}
	if (log.containsKey("os.release") && StringUtils.isEmpty(release)){
		release = log.get("os.release");
		if (!StringUtils.isEmpty(release))
			context.put("for_osFullSystem_os.release", release);
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend      jsonResult
 * @been-depend 
*/
def get_first_rst(track, collection, context, log){
	if (track.contains("jsonResult")){
		track.add("first_rst");
		jsonresult = collection.get("jsonResult");
		first_rst = false;
		if (jsonresult.containsKey(1)){
			first_rst = true;
			return first_rst;
		}
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend      jsonResult
 * @been-depend 
*/
def get_last_rst(track, collection, context, log){
	if (track.contains("jsonResult")){
		track.add("last_rst");
		jsonresult = collection.get("jsonResult");
		last_rst = false;
		for (Map.Entry<Integer, String> entry : jsonresult.entrySet()){
			temp = entry.getValue();
			index = temp.indexOf("\"ls\":");
			if ( index >= 0 && "true".equals(temp.substring(
				index+5, index+9))){
				last_rst = true;
				break;
			}
		}
		return last_rst;
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend recResult, first_rst, last_rst
*/
def get_jsonResult(track, collection, context, log){
	callname = context.get("callname");
	if ( "msslog".equals(context.get("logtype")) && 
		("auw".equals(callname) || "grs".equals(callname))){
		result = null;
		cmn_result = log.get("cmn_result");
		json_result = log.get("result");
		if (cmn_result != null && !"".equals(cmn_result)){
			result = cmn_result;
		} else if (json_result != null && !"".equals(json_result)
			&& "QISRGetResult".equals(log.get("fn"))){
			result = json_result;
		}

		if (result != null && result.length() > 5){
			index = result.indexOf("\"sn\":");
			if (index >= 0){
				jr = context.get("jsonresult");
				if (jr == null){
					jr = new HashMap<Integer, String>();
				}
				jr.put(Integer.parseInt(result.charAt(index+5).toString(), 10), result);
				context.put("jsonresult", jr);
			}
		}
	}

	if (context.get("lastlog")){
		length = context.get("len");
		if (length <= 0){
			if (context.containsKey("jsonresult")){
				track.add("jsonResult");
				return context.get("jsonresult");
			}
		}
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_json_cnt(track, collection, context, log){
	//无其他字段依赖于它
	if (context.get("ispostprocess")){
		if (context.containsKey("jsonresult")){
			track.add("json_cnt");
			return context.get("jsonresult").size();
		}
	}
	return ;
}

/*
 * @author 		bingli3
 * @brief		nlp raw content
 * @rule		
 * @depend      nlpResult   bizResult
 * @been-depend 
*/
def get_ni(track, collection, context, log){
	if ( !track.contains("bizResult") && !track.contains("nlpResult")) return ;
	ni = new HashMap<String, String>();
	if (track.contains("nlpResult")){
		nlpResult = collection.get("nlpResult");
		nlpResult = nlpResult.replaceAll("[\r\n\t]+", "");
		try {
			handler = NlpXmlPaser.getInstance().parse(nlpResult);
			ni.put("focus", handler.getNlpFocus());
			ni.put("topic", handler.getNlpTopic());
		} catch (IOException e){
			System.out.println(context.get("sid") + " nlpxml parse failed");
			return ;
		}
	}

	if (track.contains("bizResult")){
		track.add("ni");
		bizResult = collection.get("bizResult");

		pattern = Pattern.compile("<topic>(.*?)</topic>");
		matcher = pattern.matcher(bizResult);
		if (matcher.find()){
			ni.put("topic", matcher.group(1));
			pattern = Pattern.compile("\\[CDATA\\[(.*?)\\]\\]");
			matcher = pattern.matcher(ni.get("topic"));
			if (matcher.find()){
				ni.put("topic", matcher.group(1));
			}
		}

		pattern = Pattern.compile("<focus>(.*?)</focus>");
		matcher = pattern.matcher(bizResult);
		if (matcher.find()){
			ni.put("focus", matcher.group(1));
			pattern = Pattern.compile("\\[CDATA\\[(.*?)\\]\\]");
			matcher = pattern.matcher(ni.get("focus"));
			if (matcher.find()){
				ni.put("focus", matcher.group(1));
			}
		}

		pattern = Pattern.compile("<source>(.*?)</source>");
		matcher = pattern.matcher(bizResult);
		if (matcher.find()){
			ni.put("focus", matcher.group(1));
			pattern = Pattern.compile("\\[CDATA\\[(.*?)\\]\\]");
			matcher = pattern.matcher(ni.get("focus"));
			if (matcher.find()){	
				ni.put("focus", matcher.group(1));
				if ("ovs".equals(ni.get("focus"))){
					pattern = Pattern.compile("<nameType>(.*?)</nameType>");
					matcher = pattern.matcher(bizResult);
					if (matcher.find()){
						ni.put("topic", matcher.group(1));
						pattern = Pattern.compile("\\[CDATA\\[(.*?)\\]\\]");
						matcher = pattern.matcher(ni.get("topic"));
						if (matcher.find()){
							ni.put("topic", matcher.group(1));
						}
					} else {
						ni.put("topic", null);
					}
				}
			}
		}
		return ni.size() == 0 ? null : ni;
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_stat(track, collection, context, log){
	if (context.containsKey("callname")){
		st = null;
		if (context.containsKey("stat_status")){
			st = context.get("stat_status");
		} else {
			st = new HashMap<String, Integer>();
		}
		length = context.get("len");
		if (length <= 0){
			callname = context.get("callname");
			if (st.containsKey(callname)){
				st.put(callname, st.get(callname)+1);
			} else {
				st.put(callname, 1);
				context.put("stat_status", st);
			}
		}
	}

	if (context.get("lastlog") && context.get("len") <= 0){
		if (context.containsKey("stat_status")){
			track.add("stat");
			return context.get("stat_status");
		}
	}
	return ;
}

/*
 * @author 
 * @brief		voice assist
 * @rule		parse msslog, specified appid and callname equals ssb. 
 * @depend  	appid
 * @been-depend province, country, city
*/
def get_va(track, collection, context, log){
	if ("ssb".equals(context.get("callname")) && context.containsKey("ssb_msslog") 
		&& track.contains("appid")){
		appid = collection.get("appid");
		ssb_msslog = context.get("ssb_msslog");
		if (VoiceAssistant.check(appid)){
			va = VoiceAssistant.parse(ssb_msslog);
			if (va == null) return ;
			track.add("va");
			return ReadUtils.toMap(va);
		}
	}
	return ;
}

/*
 * @author 		bingli3
 * @brief		voice plus
 * @rule		parse msslog, specified appid and callname equals ssb. 
 * @depend      appid
 * @been-depend
*/
def get_vp(track, collection, context, log){
	if ("ssb".equals(context.get("callname")) && context.containsKey("ssb_msslog") 
		&& track.contains("appid")){
		appid = collection.get("appid");
		ssb_msslog = context.get("ssb_msslog");
		if (VoicePlus.check(appid)){
			vp = VoicePlus.parse(ssb_msslog);
			if (vp == null) return ;
			track.add("vp");
			return ReadUtils.toMap(vp);
		}
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_stime(track, collection, context, log){	
	if (log.containsKey("rat") && !"".equals(log.get("rat"))){
		sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss S");
		rat_date = sdf.parse(log.get("rat"));
		rat_time = rat_date.getTime();
		//keep max time current.
		if (context.containsKey("maxtime")){
			if (rat_time > context.get("maxtime")){
				context.put("maxtime", rat_time);
			}
		} else {
			context.put("maxtime", rat_time);
		}
		//keep min time current.
		if (context.containsKey("mintime")){
			if (rat_time < context.get("mintime")){
				context.put("mintime", rat_time);
			}
		} else {
			context.put("mintime", rat_time);
		}
	}

	if (context.get("lastlog") && context.get("len") <= 0){
		if (context.containsKey("maxtime") && context.containsKey("mintime") 
			&& context.get("maxtime") > context.get("mintime")){
			track.add("stime");
			return context.get("maxtime") - context.get("mintime");
		}
		return 0L;
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend      ret sub sid
 * @been-depend 
*/
def get_regtime(track, collection, context, log){
	if ( "msp_insert_anon".equalsIgnoreCase(log.get("fn"))){
		context.put("has_map_insert_anon", true);
	}
	if ("0".equals(collection.get("ret")) && ( "lgi".equals(collection.get("sub")) || 
			"ath".equals(collection.get("sub")) ) && (context.get("has_map_insert_anon") != null)){
		track.add("regtime");
		return SidParse.sidToTimestamp(collection.get("sid"));
	}
	if (context.containsKey("ispostprocess") && context.get("ispostprocess")){
		track.add("regtime");
		return Long.MAX_VALUE;
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend      sub
 * @been-depend 
*/
def get_ivd(track, collection, context, log){
	if ("msp_insert_invalid".equalsIgnoreCase(log.get("fn"))) {
		context.put("is_msp_insert_invalid", true);
	}
	if ("lgi".equals(collection.get("sub")) && context.get("is_msp_insert_invalid") != null){
		track.add("ivd");
		return true;
	}
	return ;
}

/*
 * @author 		bingli3
 * @brief		appid (which invoked by other appid in the same machine)
 * @rule		from msslog, key is caller.appid.
 * @depend      
 * @been-depend 
*/
def get_caller_appid(track, collection, context, log){
	if ("msslog".equals(context.get("logtype"))){
		caller_appid = log.get("caller.appid");
		if (!StringUtils.isEmpty(caller_appid)){
			track.add("caller_appid");
			return caller_appid;
		}
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_ssbTime(track, collection, context, log){
	if ("ssb".equals(context.get("callname"))){
		if (log.containsKey("calltime")){
			track.add("ssbTime");
			return Float.parseFloat(log.get("calltime"));
		}
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_totalTime(track, collection, context, log){
	if (log.containsKey("rat") && !"".equals(log.get("rat")) &&
		"msslog".equals(context.get("logtype"))){
		sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss S");
		rat_date = sdf.parse(log.get("rat"));
		rat_time = rat_date.getTime();

		if (context.containsKey("mss_maxtime")){
			if (rat_time > context.get("mss_maxtime")){
				context.put("mss_maxtime", rat_time);
			}
		} else {
			context.put("mss_maxtime", rat_time);
		}

		if (context.containsKey("mss_mintime")){
			if (rat_time < context.get("mss_mintime")){
				context.put("mss_mintime", rat_time);
			}
		} else {
			context.put("mss_mintime", rat_time);
		}
	}

	if (context.get("lastlog") && context.get("len") <= 0){
		if (context.containsKey("mss_maxtime") && context.containsKey("mss_mintime") 
			&& context.get("mss_maxtime") >= context.get("mss_mintime")){
			track.add("totalTime");
			return (Float)context.get("mss_maxtime") - context.get("mss_mintime"); 
		}
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_advid(track, collection, context, log){
	if ("msslog".equals(context.get("logtype")) 
		&& log.containsKey("advid")){
		track.add("advid");
		return log.get("advid");
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_dvcsid(track, collection, context, log){
	if ("msslog".equals(context.get("logtype"))
		&& "ssb".equals(context.get("callname"))) {
		sid = log.get("sid");
		if (!StringUtils.isEmpty(sid) && sid.indexOf("@")==-1 ){
			track.add("dvcsid");
			return sid;
		}
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_other(track, collection, context, log){
	if ("msslog".equals(context.get("logtype")) 
		&& log.containsKey("other")){
		track.add("other");
		return log.get("other");
	}
	return ;
}

/*
 * @author 		zachen2
 * @brief		session return value.(11011,0 and so on)
 * @rule		
 * @depend
 * @been-depend regtime
*/
def get_ret(track, collection, context, log){
	if ("msslog".equals(context.get("logtype"))){
		if(context.containsKey("callLogExMap")){
			callLogExMap = (HashMap)context.get("callLogExMap");
		}else{
			callLogExMap = new HashMap<String,ArrayList<Map>>();
			context.put("callLogExMap",callLogExMap);
		}
	
		pid = log.get("pid");
		if(pid != null){
			if(!callLogExMap.containsKey(pid)){
				callLogExList = new ArrayList<Map>();
				callLogExMap.put(pid,callLogExList);
			}else{
				callLogExList = callLogExMap.get(pid);
			}
			Map<String,String> tmp = new HashMap<String,String>();
			if(log.containsKey("nextsub")) tmp.put("nextsub",log.get("nextsub"));
			if(log.containsKey("fn")) tmp.put("fn",log.get("fn"));
			if(log.containsKey("cn")) tmp.put("cn",log.get("cn"));
			if(log.containsKey("ret")) tmp.put("ret",log.get("ret"));
			callLogExList.add(tmp);
		}
	
		// 获取rss=5的pid
		if("5".equals(log.get("rss")) && log.containsKey("pid")){
			pidofRssEqual5 = log.get("pid");
			timestampofPidofRssEqual5 = new Long(context.get("calllogtimestamp"));
			if(pidofRssEqual5 != null){
				context.put("pidofRssEqual5",pidofRssEqual5);
			}
			context.put("timestampofPidofRssEqual5",timestampofPidofRssEqual5);
		}
	
		//获取callName = ledal中含有bizResult且bizResult != null的pid
		if("ldeal".equals(context.get("callname"))){
			if(log.containsKey("biz_result") && log.containsKey("pid")){
				pidofLdealhasBizResult = log.get("pid");
				timestampofPidofLdealhasBizResult = new Long(context.get("calllogtimestamp"));
				if(pidofLdealhasBizResult != null){
					context.put("pidofLdealhasBizResult",pidofLdealhasBizResult);
				}
				context.put("timestampofPidofLdealhasBizResult",timestampofPidofLdealhasBizResult);
			}
		}	
	
		/*** 通用获取会话返回值的方法 ***/
		if("ERROR".equals(context.get("loglevel")) && !"0".equals(log.get("ret"))
			&& ("mss".equals(log.get("cn")) || "luacommon".equals(log.get("cn")))){
				oldret = (String)context.get("ret");
				curret = log.get("ret");
				oldtimestamp = (Long)context.get("oldtimestamp");
				curtimestamp = new Long(context.get("calllogtimestamp"));
				if(null != oldret && null != oldtimestamp){
					if(curtimestamp < oldtimestamp){
						context.put("ret",curret);
						context.put("oldtimestamp",curtimestamp);
					}
				}else{
					context.put("ret",curret);
					context.put("oldtimestamp",curtimestamp);
				}
		}	
	
	}	
	
	if (context.get("lastlog") && context.get("len") <= 0){
		//判断rss=5的会话
		pidofRssEqual5 = null;
		timestampofPidofRssEqual5 = Long.MAX_VALUE;
		if(context.containsKey("pidofRssEqual5")){
			pidofRssEqual5 = context.get("pidofRssEqual5");
		}
		if(context.containsKey("timestampofPidofRssEqual5")){
			timestampofPidofRssEqual5 = context.get("timestampofPidofRssEqual5");
		}
		if (null != pidofRssEqual5){
			callLogExList = context.get("callLogExMap").get(pidofRssEqual5);
			if(null != callLogExList){
				boolean flag_nextsub = true;
				boolean flag_network = true;
				for(Map tempCallLogEx:callLogExList){
					if(tempCallLogEx.containsKey("nextsub") && !"".equals(tempCallLogEx.get("nextsub"))){
						flag_nextsub = false;
					}
					if("nfl_deal".equals(tempCallLogEx.get("fn")) && "mss".equals(tempCallLogEx.get("cn"))){
						if(tempCallLogEx.containsKey("ret") && !"0".equals(tempCallLogEx.get("ret"))){
							flag_network = false;
						}
					}
				
				}
				
				if (flag_nextsub && flag_network){
					// 如果时间大于mde.errCallLog.timestamp >
					// timestampofPidofRssEqual5
					// 表示是后来发生的错误，不算会话错误，用特殊的7位错误码
					if(null != context.get("ret") && context.get("oldtimestamp") > timestampofPidofRssEqual5){
						ret = context.get("ret");
						ret = Utils.transSpecialRet(ret);
						context.put("ret",ret);
					}
				}
			}
		}
		
		//判断ldeal日志中含有bizResult会话的Pid
		pidofLdealhasBizResult = null;
		timestampofPidofLdealhasBizResult = Long.MAX_VALUE;
		if(context.containsKey("pidofLdealhasBizResult")){
			pidofLdealhasBizResult = context.get("pidofLdealhasBizResult");
		}
		if(context.containsKey("timestampofPidofLdealhasBizResult")){
			timestampofPidofLdealhasBizResult = context.get("timestampofPidofLdealhasBizResult");
		}
		if(null != pidofLdealhasBizResult){
			callLogExList = context.get("callLogExMap").get(pidofLdealhasBizResult);
			if(null != callLogExList){
				boolean flag_network = true;
				for(Map tempCallLogEx:callLogExList){
					if("nfl_deal".equals(tempCallLogEx.get("fn")) && "mss".equals(tempCallLogEx.get("cn"))){
						if(tempCallLogEx.containsKey("ret") && !"0".equals(tempCallLogEx.get("ret"))){
							flag_network = false;
						}
					}
				}
				if(flag_network){
					if(null != context.get("ret") && context.get("oldtimestamp") > timestampofPidofLdealhasBizResult){
						ret = context.get("ret");
						ret = Utils.transSpecialRet(ret);
						context.put("ret",ret);
					}
				}
			}
		}
		
		ret = context.get("ret");
		if(ret != null && !"".equals(ret)){
			track.add("ret");
			return Integer.parseInt(ret);
		}	
	}
	return;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_bd_len(track, collection, context, log){
	if ("atslog".equals(context.get("logtype"))){
        if (context.containsKey("mediadata")){
            mediadata = context.get("mediadata");
            length = mediadata.getLength();
            if(length > 0){
                track.add("bd_len");
                return length;
            }
        }
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_nameType(track, collection, context, log){
	if ("ldeal".equals(context.get("callname"))){
		if (log.containsKey("biz_result")){
			bizResult = log.get("biz_result");
			pattern = Pattern.compile("\\[CDATA\\[ovs\\]\\]");
			matcher = pattern.matcher(bizResult);
			if (matcher.find()){
				pattern = Pattern.compile("<nameType>(.*?)</nameType>");
				matcher = pattern.matcher("bizResult");
				if (matcher.find()){
					nameType = matcher.group(1);
					track.add("nameType");
					return nameType;
				}
			}
		}
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend      appid
 * @been-depend 
*/
def get_desc(track, collection, context, log){
	SESSION_ID_LEN = 32;
	if (track.contains("sid") && collection.get("sid").length() != SESSION_ID_LEN){
		exception_desc = context.get("for_desc_exception_desc");
		if (exception_desc == null){
			exception_desc = new ArrayList<String>();
		}
		if (!exception_desc.contains("invalid sid")){
			exception_desc.add("invalid sid");
			context.put("for_desc_exception_desc", exception_desc);
		}
	}

	if ("auw".equals(context.get("callname")) && log.containsKey("syncid")){
		syncid = Integer.parseInt(log.get("syncid"));
		con_syncid = context.get("for_desc_syncid");
		if (con_syncid == null){
			con_syncid = new ArrayList<Integer>();
		}

		if (!con_syncid.contains(syncid)){
			con_syncid.add(syncid);
			context.put("for_desc_syncid", con_syncid);
		} else {
			exception_desc = context.get("for_desc_exception_desc");
			if (exception_desc == null){
				exception_desc = new ArrayList<String>();
			}
			if (syncid < 1){
				exception_desc.add("invalid syncid");
			} else {
				exception_desc.add("repeated syncid");
			}
			context.put("for_desc_exception_desc", exception_desc);
		}
	}

	if (context.get("lastlog") && context.get("len") <= 0){
		exception_desc = context.get("for_desc_exception_desc");
		if (exception_desc != null){
			track.add("desc");
			return exception_desc.toString();
		}
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_lalr(track, collection, context, log){
	//无其他字段依赖于它
	if (context.get("ispostprocess")){
		if (context.containsKey("atsLRTime") && context.containsKey("audioLRTime")){
			atsLRTime = context.get("atsLRTime");
			audioLRTime = context.get("audioLRTime");
			lalr = atsLRTime.getTime() - audioLRTime.getTime();
			track.add("lalr");
			return (float)lalr;
		}
		return ;
	}

	if (!"auw".equals(context.get("callname")) && !"grs".equals(context.get("callname"))) return ;
	if (!"msslog".equals(context.get("logtype"))) return ;
	
	sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss S");
	if (log.containsKey("calltime")){
		context.put("lalr_calltime", log.get("calltime"));
	}
	if (log.containsKey("mat")){
		context.put("lalr_mat", log.get("mat"));
	}
	if (log.containsKey("putq_to_deal")){
		context.put("lalr_putq_to_deal", log.get("putq_to_deal"));
	}
	if (log.containsKey("rss")){
		context.put("lalr_rss", log.get("rss"));
	}
	if (log.containsKey("aus")){
		context.put("lalr_aus", log.get("aus"));
	}
	length = context.get("len");
	if (length <= 0){
		if (context.containsKey("lalr_rss")){
			lalr_rss = context.get("lalr_rss");
			if ( "5".equals(lalr_rss)){
				if ("auw".equals(context.get("callname"))){
					if (context.containsKey("lalr_calltime")){
						lalr = context.get("lalr_calltime");
						track.add("lalr");
						lalr = Float.parseFloat(lalr);
						return lalr;
					}
				} else if ("grs".equals(context.get("callname"))){
					if (context.containsKey("lalr_calltime") && context.containsKey("lalr_mat") &&
						context.containsKey("lalr_putq_to_deal")){
						mat = sdf.parse(context.get("lalr_mat")).getTime();
						callTime = Float.parseFloat(context.get("lalr_calltime"));
						putq_to_deal = Float.parseFloat(context.get("lalr_putq_to_deal"));
						atsLRTime = new Date();
						atsLRTime.setTime((long)(callTime+putq_to_deal+mat));
						context.put("atsLRTime", atsLRTime);
					}
				}
			}
		}

		if (context.containsKey("lalr_aus")){
			lalr_aus = context.get("lalr_aus");
			if ("4".equals(lalr_aus) || "5".equals(lalr_aus)){
				if (context.containsKey("lalr_mat")){
					mat = sdf.parse(context.get("lalr_mat"));
					context.put("audioLRTime", mat);
				}
			}
		}
		//为了避免重复执行本段逻辑，删除lalr_rss
		context.remove("lalr_rss");
		//为了避免重复执行本段逻辑，删除lalr_rss
		context.remove("lalr_aus");
		context.remove("lalr_calltime");
		context.remove("lalr_mat");
		context.remove("lalr_putq_to_deal");
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_rslt(track, collection, context, log){
	rslt = log.get("rslt");
	if (rslt != null){
		track.add("rslt");
		return rslt;
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_uuid(track, collection, context, log){
	if (!"msslog".equals(context.get("logtype"))) return ;
	uuid = log.get("uuid");
	if (!StringUtils.isEmpty(uuid)){
		track.add("uuid");
		return uuid;
	}
	return ;
}

//get from uuid, dvc, usr
//why uuid, dvc? conside about TV can not get imei...
//uuid > dvc > usr (priority level)
/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_dvc(track, collection, context, log){
	if (context.get("ispostprocess")){
		if(context.containsKey("for_dvc_uuid")){
			track.add("dvc");
			return context.get("for_dvc_uuid");
		}
		if(context.containsKey("for_dvc_dvc")){
			track.add("dvc");
			return context.get("for_dvc_dvc");
		}
		if(context.containsKey("for_dvc_usr")){
			track.add("dvc");
			return context.get("for_dvc_usr");
		}
	}

	if (!"msslog".equals(context.get("logtype"))) return ;
	usr = log.get("usr");
	if (!StringUtils.isEmpty(usr)){
		context.put("for_dvc_usr", usr);
	}
	dvc = log.get("dvc");
	if (!StringUtils.isEmpty(dvc)){
		context.put("for_dvc_dvc", dvc);
	}
	uuid = log.get("uuid");
	if (!StringUtils.isEmpty(uuid)){
		context.put("for_dvc_uuid", uuid);
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_fn(track, collection, context, log){
	if (log.containsKey("fn")){
		track.add("fn");
		return log.get("fn");
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_cn(track, collection, context, log){
	if (log.containsKey("cn")){
		track.add("cn");
		return log.get("cn");
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_has_nlp(track, collection, context, log){
	if ("nlplog".equals(context.get("logtype"))){
		track.add("has_nlp");
		return true;
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_has_ssb(track, collection, context, log){
	if ("ssb".equals(context.get("callname"))){
		track.add("has_ssb");
		return true;
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_has_mss(track, collection, context, log){
	if ("msslog".equals(context.get("logtype"))){
		track.add("has_mss");
		return true;
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_retList(track, collection, context, log){
	if (log.containsKey("ret") && !"0".equals(log.get("ret"))){
		retlist = null;
		if (context.containsKey("retlist")){
			retlist = context.get("retlist");
		} else {
			retlist = new ArrayList<String>();
		}
		if (!retlist.contains(log.get("ret"))){
			retlist.add(log.get("ret"));
			context.put("retlist", retlist);
		}
	}

	if (context.get("lastlog") && context.get("len") <= 0){
		track.add("retList");
		if (context.containsKey("retlist")){
			retList = context.get("retlist");
			return retList;
		}
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_atc_retList(track, collection, context, log){
	if (log.containsKey("atc_ret") && !"0".equals(log.get("atc_ret"))){
		atc_retlist = null;
		if (context.containsKey("atc_retlist")){
			atc_retlist = context.get("atc_retlist");
		} else {
			atc_retlist = new ArrayList<String>();	
		}
		if (!atc_retlist.contains(log.get("atc_ret"))){
			atc_retlist.add(log.get("atc_ret"));
			context.put("atc_retlist", atc_retlist);
		}
	}

	if (context.get("lastlog") && context.get("len") <= 0){
		track.add("atc_retList");
		if (context.containsKey("atc_retlist")){
			atc_retList = context.get("atc_retlist");
			return atc_retList;
		}
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend      jsonResult
 * @been-depend 
*/
def get_recResult(track, collection, context, log){
 	if (track.contains("jsonResult")){
 		jsonresult = collection.get("jsonResult");
 		recResult = "";
 		jsonresultInt = new ArrayList<Integer>();
 		jsonresultInt.addAll(jsonresult.keySet());
 		Collections.sort(jsonresultInt);
 		for (Integer key : jsonresultInt){
 			js = JSONObject.fromObject(jsonresult.get(key));
 			jsArrIn = js.getJSONArray("ws");
 			for (int i = 0; i < jsArrIn.size(); i ++){
 				jsObIn = jsArrIn.getJSONObject(i);
 				jsArray = jsObIn.getJSONArray("cw");
 				deepIn = jsArray.getJSONObject(0);
 				recResult += deepIn.getString("w");
 			}
 		}
 		track.add("recResult");
 		return recResult;
 	}
 	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend      bizResult
 * @been-depend 
*/
def get_bizSubType(track, collection, context, log){
	if (track.contains("bizResult")){
		bizResult = collection.get("bizResult");
		pattern = Pattern.compile("\\[CDATA\\[ovs\\]\\]");
		matcher = pattern.matcher(bizResult);
		if (matcher.find()){
			pattern = Pattern.compile("<nameType>(.*?)</nameType>");
			matcher = pattern.matcher(bizResult);
			if (matcher.find()){
				bizSubType = matcher.group(1);
				track.add("bizSubType");
				return bizSubType;
			}
		}

		pattern = Pattern.compile("\\[CDATA\\[nlp_(.*?)\\]\\]");
		matcher = pattern.matcher(bizResult);
		if (matcher.find()){
			bizSubType = matcher.group(1);
			track.add("bizSubType");
			return bizSubType;
		}
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend      bizResult
 * @been-depend 
*/
def get_bizType(track, collection, context, log){
	if (track.contains("bizResult")){
		bizResult = collection.get("bizResult");
		pattern = Pattern.compile("\\[CDATA\\[ovs\\]\\]");
		matcher = pattern.matcher(bizResult); 
		if (matcher.find()){
			bizType = "ovs";
			track.add("bizType");
			return bizType;
		}

		pattern = Pattern.compile("\\[CDATA\\[nlp_(.*?)\\]\\]");
		matcher = pattern.matcher(bizResult); 
		if (matcher.find()){
			bizType = "nlp";
			track.add("bizType");
			return bizType;
		}

		pattern = Pattern.compile("\\[CDATA\\[openqa\\]\\]");
		matcher = pattern.matcher(bizResult); 
		if (matcher.find()){
			bizType = "openqa";
			track.add("bizType");
			return bizType;
		}
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend      bizResult
 * @been-depend 
*/
def get_bizSource(track, collection, context, log){
	if (track.contains("bizResult")){
		bizResult = collection.get("bizResult");
		pattern = Pattern.compile("<sourceName><!\\[CDATA\\[(.*?)\\]\\]></sourceName>");
		matcher = pattern.matcher(bizResult);
		if (matcher.find()){
			bizSource = matcher.group(1);
			track.add("bizSource");
			return bizSource;
		}
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_open_nlp(track, collection, context, log){
	if ("msslog".equals(context.get("logtype")) && log.containsKey("open_nlp")){
		open = log.get("open_nlp");
		open = open.replaceAll("[{}\"]", "");
		params = open.split(",");
		tempNlpInfo = new HashMap<String, String>();
		for (String param : params){
			val = param.split(":");
			if (val.size() >= 2){
				key = val[0];
				value = val[1];
				tempNlpInfo.put(key, value);
			}
		}
		if (context.containsKey("open_nlp")){
			open_nlp = context.get("open_nlp");
			open_nlp.add(tempNlpInfo);
		} else {
			open_nlp = new ArrayList<HashMap<String, String>>();
			open_nlp.add(tempNlpInfo);
		}
		context.put("open_nlp", open_nlp);
	}

	if (context.get("lastlog") && context.get("len") <= 0){
		if (context.containsKey("open_nlp")){
			track.add("open_nlp");
			return context.get("open_nlp");
		}
		return ;
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_fafr(track, collection, context, log){
	//无其他字段依赖于它
	if (context.get("ispostprocess")){
		if (context.containsKey("audioFRTime") && context.containsKey("atsFRTime")){
			audioFRTime = context.get("audioFRTime");
			atsFRTime = context.get("atsFRTime");
			fafr = atsFRTime.getTime() - audioFRTime.getTime();
			track.add("fafr");
			return (float)fafr;
		}
	}
	if (!"msslog".equals(context.get("logtype"))) return ;
	if (!"auw".equals(context.get("callname")) && !"grs".equals(context.get("callname"))) return ;

	sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss S");
	if (log.containsKey("mat")){
		context.put("fafr_mat", log.get("mat"));
	}
	if (log.containsKey("putq_to_deal")){
		context.put("fafr_putq_to_deal", log.get("putq_to_deal"));
	}
	if (log.containsKey("calltime")){
		context.put("fafr_calltime", log.get("calltime"));
	}
	
	jsonresult = null;
	if (log.containsKey("cmn_result") && !"".equals(log.get("cmn_result"))){
		jsonresult = log.get("cmn_result");
	} else if (log.containsKey("result") && !"".equals(log.get("result"))){
		jsonresult = log.get("result");
	}

	if (jsonresult != null){
		pos = jsonresult.indexOf("\"sn\":");
		if ( pos >= 0){
			sn = jsonresult.charAt(pos+5).toString();
			if ("1".equals(sn)){
				context.put("fafr_track", true);
			}
		}
	}
	if ("1".equals(log.get("aus"))){
		context.put("audiofrtime_track", true);
	}

	length = context.get("len");
	if (length <= 0){
		if (context.containsKey("audiofrtime_track") && context.get("audiofrtime_track")){
			if (context.containsKey("fafr_mat")){
				audioFRTime = new Date();
				audioFRTime.setTime(sdf.parse(context.get("fafr_mat")).getTime());
				context.put("audioFRTime", audioFRTime);
			}
		}
		if (context.containsKey("fafr_track") && context.get("fafr_track")){
			if ( context.containsKey("fafr_mat") && context.containsKey("fafr_putq_to_deal") && context.containsKey("fafr_calltime")){
					atsFRTime = new Date();
					fa = Float.parseFloat(context.get("fafr_putq_to_deal"));
					fb = Float.parseFloat(context.get("fafr_calltime"));
					atsFRTime.setTime(sdf.parse(context.get("fafr_mat")).getTime() + (long)(fa+fb));
					context.put("atsFRTime", atsFRTime);
			}
		}
        context.remove("fafr_mat");
        context.remove("fafr_putq_to_deal");
        context.remove("fafr_calltime");
        context.remove("audiofrtime_track");
        context.remove("fafr_track");
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend      sid
 * @been-depend 
*/
def get_timestamp(track, collection, context, log){
	if (track.contains("sid")){
		sid = collection.get("sid");
		//SidParse.sidToTimestamp(session id) return long
		timestamp = SidParse.sidToTimestamp(sid);
		track.add("timestamp");
		return timestamp;
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_logtimestamp(track, collection, context, log){
	if (context.containsKey("logtimestamp")){
		track.add("logtimestamp");
		return Long.parseLong(context.get("logtimestamp"));
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend       auf_rate aue
 * @been-depend 
*/
def get_age(track, collection, context, log){
	if (context.get("ispostprocess") && context.containsKey("mediadata")){
		mediadata = context.get("mediadata");
		aue = collection.get("aue");
		auf_rate = collection.get("auf_rate");
		info = context.get("info_for_age_gender");//缓存解析时间消耗大的字段
		if (info == null && !StringUtils.isEmpty(auf_rate)){
			info = MediaParse.parse(mediadata, aue, auf_rate);
			context.put("info_for_age_gender", info);
		}
		age = null;
		if (info != null){
			String[] subs = info.split(",");
            if ("-1".equals(subs[0])) {
              age = Age.UNKNOWN.toString();
            } else if ("0".equals(subs[0])) {
              age = Age.CHILD.toString();
            } else if ("1".equals(subs[0])) {
              age = Age.YOUNG.toString();
            } else if ("2".equals(subs[0])) {
              age = Age.ELDER.toString();
            } else {
              age = Age.UNKNOWN.toString();
            }
            track.add("age");
            return age;
		}
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend      auf_rate aue
 * @been-depend 
*/
def get_gender(track, collection, context, log){
	if (context.get("ispostprocess") && context.containsKey("mediadata")){
		mediadata = context.get("mediadata");
		aue = collection.get("aue");
		auf_rate = collection.get("auf_rate");
		info = context.get("info_for_age_gender");
		if (info == null && !StringUtils.isEmpty(auf_rate)){
			info = MediaParse.parse(mediadata, aue, auf_rate);
			context.put("info_for_age_gender", info);
		}
		gender = null;
		if (info != null){
			String[] subs = info.split(",");
            if ("-1".equals(subs[1])) {
              gender = Gender.UNKNOWN.toString();
            } else if ("0".equals(subs[1])) {
              gender = Gender.MALE.toString();
            } else if ("1".equals(subs[1])) {
              gender = Gender.FEMALE.toString();
            } else {
              gender = Gender.UNKNOWN.toString();
            }
            track.add("gender");
            return gender;
		}
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_hmClientPerfModels(track, collection, context, log){
	//该字段需要遍历整个calllog才能得到最后结果，且没有其他字段依赖于它
	if (context.get("ispostprocess")){
		if (context.containsKey("hmclientlog_for_hmclientperfmodels")){
			track.add("hmClientPerfModels");
			return context.get("hmclientlog_for_hmclientperfmodels");
		}
	}
	//对于mss日志且有ssb字段才进行解析,只存字符串不作解析
	if ("msslog".equals(context.get("logtype")) && "ssb".equals(context.get("callname"))){
		if (log.containsKey("client_log")){
			List<String> hmClientLog = context.get("hmclientlog_for_hmclientperfmodels");
			if (hmClientLog == null){
				hmClientLog = new ArrayList<String>();
			}
			//Pattern p = Pattern.compile("^(.+)=(.*)\$");
			str_client_log = log.get("client_log");
			/*if (str_client_log != null){
				String[] parts = str_client_log.split("&");
				for (String temp : parts){
					Matcher matcher = p.matcher(temp);
					if (matcher.find()){
						hmClientLog.put(matcher.group(1), matcher.group(2));
					}
				}
			}*/
			if (str_client_log != null){
				hmClientLog.add(str_client_log);
			}
			context.put("hmclientlog_for_hmclientperfmodels", hmClientLog);
		}
	}
	return ;
}

//merge from user profile
/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_dvc_type(track, collection, context, log){
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_resolution(track, collection, context, log){
	resolution = log.get("resolution");
	if (resolution != null){
		track.add("resolution");
		return resolution;
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_osType(track, collection, context , log){
	osType = context.get("osType");
	if (osType != null){
		track.add("osType");
		return osType;
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_machineType(track, collection, context, log){
	mt = context.get("machineType");
	if (mt != null){
		track.add("machineType");
		return mt;
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_srcRet(track, collection, context, log){
	if (context.containsKey("srcret_for_srcret")){
		track.add("srcRet");
		return context.get("srcret_for_srcret");
	}

	if (!"ERROR".equals(context.get("loglevel"))) return ;
	if (!log.containsKey("ret") || !log.containsKey("err_fn") || !log.containsKey("err_cn")) return ;

	tmpRet = log.get("ret").trim();
	if (tmpRet == null || tmpRet.isEmpty() || "0".equals(tmpRet)) return ;

	mdri = context.get("srcret_for_timestamp");
	if ( mdri == null){
		mdri = Long.MIN_VALUE;
	}

	ltt = context.get("calllogtimestamp");
	if ( mdri == Long.MIN_VALUE || (ltt > 0 && mdri > ltt)){
		context.put("srcret_for_timestamp", ltt);
		context.put("srcret_for_srcret", log.get("err_ret"));
		context.put("srcret_for_srccn", log.get("err_cn"));
		context.put("srcret_for_srcfn", log.get("err_fn"));
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_retCn(track, collection, context, log){
	if (context.containsKey("srcret_for_srccn")){
		track.add("retCn");
		return context.get("srcret_for_srccn");
	}

	if (!"ERROR".equals(context.get("loglevel"))) return ;
	if (!log.containsKey("ret") || !log.containsKey("err_fn") || !log.containsKey("err_cn")) return ;

	tmpRet = log.get("ret").trim();
	if (tmpRet == null || tmpRet.isEmpty() || "0".equals(tmpRet)) return ;

	mdri = context.get("srcret_for_timestamp");
	if ( mdri == null){
		mdri = Long.MIN_VALUE;
	}

	ltt = context.get("calllogtimestamp");
	if ( mdri == Long.MIN_VALUE || (ltt > 0 && mdri > ltt)){
		context.put("srcret_for_timestamp", ltt);
		context.put("srcret_for_srcret", log.get("err_ret"));
		context.put("srcret_for_srccn", log.get("err_cn"));
		context.put("srcret_for_srcfn", log.get("err_fn"));
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_retFn(track, collection, context, log){
	if (context.containsKey("srcret_for_srcfn")){
		track.add("retFn");
		return context.get("srcret_for_srcfn");
	}

	if (!"ERROR".equals(context.get("loglevel"))) return ;
	if (!log.containsKey("ret") || !log.containsKey("err_fn") 
		|| !log.containsKey("err_cn")) return ;

	tmpRet = log.get("ret").trim();
	if (tmpRet == null || tmpRet.isEmpty() || "0".equals(tmpRet)) return ;

	mdri = context.get("srcret_for_timestamp");
	if ( mdri == null){
		mdri = Long.MIN_VALUE;
	}

	ltt = context.get("calllogtimestamp");
	if ( mdri == Long.MIN_VALUE || (ltt > 0 && mdri > ltt)){
		context.put("srcret_for_timestamp", ltt);
		context.put("srcret_for_srcret", log.get("err_ret"));
		context.put("srcret_for_srccn", log.get("err_cn"));
		context.put("srcret_for_srcfn", log.get("err_fn"));
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_act_ret(track, collection, context, log){
	if (!"msslog".equals(context.get("logtype"))) return ;
	if (!log.containsKey("ret")) return ;
	if (!"wbs".equals(log.get("call"))) return ;
	act_ret = log.get("errpos");
	if (act_ret != null){
		track.add("act_ret");
		return act_ret;
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_platform(track, collection, context, log){
	if (!"msslog".equals(context.get("logtype"))) return ;
	platform = log.get("errpos");
	if (platform != null){
		track.add("platform");
		return platform;
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_rec_id(track, collection, context, log){
	rec_id = log.get("rec_id");
	if (rec_id != null){
		track.add("rec_id");
		return rec_id;
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_tlua_calltime(track, collection, context, log){
	if (context.get("ispostprocess")){
		if (context.containsKey("for_tlua_calltime")){
			track.add("tlua_calltime");
			return (float)context.get("for_tlua_calltime");
		}
		return ;
	}
	if (!"msslog".equals(context.get("logtype"))) return ;
	if (!"ldeal".equals(context.get("callname"))) return ;
	if ("tlua".equals(log.get("call")) && log.containsKey("calltime")){
		tlua_calltime = context.get("for_tlua_calltime");
		if (tlua_calltime == null){
			tlua_calltime = 0f;
		}
		tempCallTime = log.get("calltime");
		tlua_calltime += Float.parseFloat(tempCallTime);
		context.put("for_tlua_calltime", tlua_calltime);
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_raw_text(track, collection, context, log){
	if (!"msslog".equals(context.get("logtype"))) return ;
	if (!"tluarpcs".equals(context.get("callname"))) return ;
	if (!log.containsKey("rpc_params")) return ;

	Pattern pattern = Pattern.compile("recresult=(.*)");
	rpc_params = log.get("rpc_params");
	parts = rpc_params.split(";");
	for (String part : parts){
		Matcher matcher = pattern.matcher(part);
		if (matcher.find()){
			track.add("raw_text");
			return matcher.group(1);
		}
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_book_title(track, collection, context, log){
	if (!"msslog".equals(context.get("logtype"))) return ;

	book_title = log.get("book_title");
	if (book_title != null){
		track.add("book_title");
		return book_title;
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_book_type(track, collection, context, log){
	if (!"msslog".equals(context.get("logtype"))) return ;

	book_type = log.get("book_type");
	if (book_type != null){
		track.add("book_type");
		return book_type;
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_ivp_detail_err(track, collection, context, log){
	if (context.get("ispostprocess")){
		if (context.containsKey("for_ivp_detail_err")){
			track.add("ivp_detail_err");
			return context.get("for_ivp_detail_err");
		}
		return ;
	}
	if (!"ivp".equals(context.get("biztype"))) {
		track.add("ivp_detail_err");
		return ;
	}
	if (!"msslog".equals(context.get("logtype"))) return ;

	ivp_errs = context.get("for_ivp_detail_err");
	if (ivp_errs == null){
		ivp_errs = new ArrayList<Integer>();
	}
	if (log.containsKey("detail_err")){
		ivp_errs.add(Integer.parseInt(log.get("detail_err")));
	}
	context.put("for_ivp_detail_err", ivp_errs);
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_ivpMap(track, collection, context, log){
	if (context.get("ispostprocess")){
		if (context.containsKey("for_ivpmap")){
			track.add("ivpMap");
			return context.get("for_ivpmap");
		}
	}
	if (!"ivp".equals(context.get("biztype"))) {
		track.add("ivpMap");
		return ;
	}
	if (!"msslog".equals(context.get("logtype"))) return ;

	ivpmap = context.get("for_ivpmap");
	if (ivpmap == null){
		ivpmap = new HashMap<String, String>();
	}
	if (log.containsKey("sst")){
		ivpmap.put("sst", log.get("sst"));
	}
	if (log.containsKey("rgn")){
		ivpmap.put("rgn", log.get("rgn"));
	}
	if (log.containsKey("work_mode")){
		ivpmap.put("work_mode", log.get("work_mode"));
	}
	context.put("for_ivpmap", ivpmap);
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_audio(track, collection, context, log){
	if (context.containsKey("mediadata")){
		audio = context.get("mediadata").data;
		if (audio != null && audio.limit() > 0){
			track.add("audio");
			return audio;
		}
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_maxtime(track, collection, context, log){
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_mintime(track, collection, context, log){
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_is_new(track, collection, context, log){
	//merge with userprofile
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_flag(track, collection, context, log){
	//confused and unuse.
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_is_new_caller(track, collection, context, log){
	//merge with userprofile
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_regtime_caller(track, collection, context, log){
	//merge with userprofile
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_auth_id(track, collection, context, log){
	if ("msslog".equals(context.get("logtype")) && 
		"ssb".equals(context.get("callname"))){
		auth_id = log.get("auth_id");
		if (auth_id != null){
			track.add("auth_id");
			return auth_id;
		}
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_itv_muuid(track, collection, context, log){
	if ("msslog".equals(context.get("logtype")) && 
		"ssb".equals(context.get("callname"))){
		itv_muuid = log.get("itv_muuid");
		if (itv_muuid != null){
			track.add("itv_muuid");
			return itv_muuid;
		}
	}
	return ;
}

/*
 * @author 		
 * @brief		
 * @rule		
 * @depend
 * @been-depend 
*/
def get_sst(track, collection, context, log){
	if ("msslog".equals(context.get("logtype"))){
		sid = context.get("sid");
		if (StringUtils.isNotEmpty(sid) && sid.startsWith("wfr")){
			sst = log.get("sst");
			if (StringUtils.isNotEmpty(sst)){
				track.add("sst");
				return sst;
			}
		}
	}
	return ;
}

/*
*	@author dwli
*	@brief	提取声纹业务的密码类型
*	@rule	直接获取
*	@depend sid
*   @been-depend 
*/
def get_pwdt(track, collection, context, log){
	if ("msslog".equals(context.get("logtype"))){
		sid = context.get("sid");
		if (sid != null && sid.startsWith("ivp")){
			val = null;
			val = log.get("pwdt");
			if (val != null){
				track.add("pwdt");
				return val;
			}
		}
	}
	return ;
}

/*
*	@author dwli
*	@brief	在登录日志中获取广告id
*	@rule	直接获取
*	@depend null
*	@been-depend null
*/
def get_idfa(track, collection, context, log){
	if ("lgi".equals(context.get("biztype")) ||
		"ath".equals(context.get("biztype"))){
		if ("msslog".equals(context.get("logtype"))){
			val = log.get("idfa");
			if (!StringUtils.isEmpty(val)){
				track.add("idfa");
				return val;
			}
		}
	}
	return ;
}

/*
*	@author dwli
*	@brief	在登录日志中获取设备id
*	@rule	直接获取
*	@depend null
*	@been-depend null
*/
def get_did(track, collection, context, log){
	if ("lgi".equals(context.get("biztype")) ||
		"ath".equals(context.get("biztype"))){
		if ("msslog".equals(context.get("logtype"))){
			val = log.get("did");
			if (!StringUtils.isEmpty(val)){
				track.add("did");
				return val;
			}
		}
	}
	return ;
}

/*
*	@author dwli
*	@brief	计算出屏幕尺寸
*	@rule	
*	@depend null
*	@been-depend null
*/
def get_screenSize(track, collection, context, log){
	if (!"msslog".equals(context.get("logtype"))) return ;
	if(track.contains("osDensity")&& track.contains("osResolution"))
	{
		float density = Float.valueOf(collection.get("osDensity"));
		osResolution = collection.get("osResolution");
		if (!StringUtils.isEmpty(osResolution)){
			int w = Integer.parseInt(osResolution.substring(0, osResolution.indexOf("*")));
			int h = Integer.parseInt(osResolution.substring(osResolution.indexOf("*") + 1));

			if (density > 0){
				float s = Math.sqrt(h * h + w * w) / (160 * density);
				screenSize=""+s;
				if (screenSize != null){
					track.add("screenSize");	
					return screenSize; 
				}
			}
		}
	}
	return ;	
}

/*
*	@author dwli
*	@brief	
*	@rule	
*	@depend null
*	@been-depend null
*/
def get_dvcother(track, collection, context, log){
	if ("msslog".equals(context.get("logtype"))){
		val = null;
		if (log.containsKey("other")){
			val = log.get("other");
		} else if (log.containsKey("dvcother")){
			val = log.get("dvcother");
		}
		if (val != null) {
			track.add("dvcother");
			return val;
		}
	}
	return ;
}

/*
*	@author dwli
*	@brief	ios设备信息
*	@rule	
*	@depend null
*	@been-depend null
*/
def get_openudid(track, collection, context, log){
	if ("msslog".equals(context.get("logtype"))){
		val = null;
		if (log.containsKey("openudid")){
			val = log.get("openudid");
		}
		if (val != null) {
			track.add("openudid");
			return val;
		}
	}
	return ;
}

/*
*	@author dwli
*	@brief	android设备ID
*	@rule	
*	@depend null
*	@been-depend null
*/
def get_android_id(track, collection, context, log){
	if ("msslog".equals(context.get("logtype"))){
		val = null;
		if (log.containsKey("os.android_id")){
			val = log.get("os.android_id");
		}
		if (val != null) {
			track.add("android_id");
			return val;
		}
	}
	return ;
}

/*
*	@author dwli
*	@brief	操作系统版本号
*	@rule	
*	@depend null
*	@been-depend null
*/
def get_osVersion(track, collection, context, log){
	if (!"msslog".equals(context.get("logtype"))) return ;
	val	= null;
	if (log.containsKey("os.version")){
		val = log.get("os.version");
	} else if (log.containsKey("osversion")){
		val = log.get("osversion");
	}
	if (val != null){
		track.add("osVersion");	
		return val;
	}
	return ;
}

/*
*	@author dwli
*	@brief	设备厂商(如“samsung”)
*	@rule	
*	@depend null
*	@been-depend null
*/	
def get_manufact(track, collection, context , log){
	if (!"msslog".equals(context.get("logtype"))) return ;
	val = null;
	if (log.containsKey("os.manufact")){
		val = log.get("os.manufact");
	} else if (log.containsKey("osmanufact")){
		val = log.get("osmanufact");
	}
	if (val != null){
		track.add("manufact");	
		return val;
	}
	return ;
}

/*
*	@author dwli
*	@brief	
*	@rule	
*	@depend null
*	@been-depend null
*/
def get_jailbreak(track, collection, context, log){
	if ("msslog".equals(context.get("logtype"))){
		val = null;
		if (log.containsKey("os.jailbreak")){
			val = log.get("os.jailbreak");
		}
		if (val != null) {
			track.add("jailbreak");
			return val;
		}
	}
	return ;
}