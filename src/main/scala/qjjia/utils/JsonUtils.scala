package qjjia.utils

import java.util

import net.minidev.json.JSONObject
import net.minidev.json.parser.JSONParser

import scala.collection.mutable

/**
  * Created by Administrator on 2016/9/26.
  */
object JsonUtils {
  /**
    * 将map转为json
    *
    * @param map 输入格式 mutable.Map[String,Object]
    * @return
    **/
  def map2Json(map: util.Map[String, Object]): String = {

    val jsonString = JSONObject.toJSONString(map)

    jsonString
  }

  /**
    * 将json转化为Map
    *
    * @param json 输入json字符串
    * @return
    **/
  def json2Map(json: String): mutable.HashMap[String, Object] = {

    val map: mutable.HashMap[String, Object] = mutable.HashMap()

    val jsonParser = new JSONParser()

    //将string转化为jsonObject
    val jsonObj: JSONObject = jsonParser.parse(json).asInstanceOf[JSONObject]

    //获取所有键
    val jsonKey = jsonObj.keySet()

    val iter = jsonKey.iterator()

    while (iter.hasNext) {
      val field = iter.next()
      val value = jsonObj.get(field).toString

      if (value.startsWith("{") && value.endsWith("}")) {
        //        val value = mapAsScalaMap(jsonObj.get(field).asInstanceOf[util.HashMap[String, String]])
        val value = jsonObj.get(field).asInstanceOf[util.HashMap[String, String]]
        map.put(field, value)
      }else {
        map.put(field, value)
      }
    }
    map
  }
}
