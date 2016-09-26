package qjjia.main

import java.io.{File, FileOutputStream}
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util
import java.util.Date

import edu.buaa.qjjia2
import edu.buaa.qjjia2.{BizConstants, GroovyCommonUtil}
import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord
import qjjia.model.log.{MediaData, RawLog, SvcLog}
import qjjia.utils.{JsonUtils, ReadAvro, SvcLogAvroUtil}

/**
  * Created by Administrator on 2016/8/26.
  */
object Test {

  def main(args: Array[String]): Unit = {
    //    val  byteBuffer = ByteBuffer.allocate(256)  //容量为256字节
    val total = Array[Byte](97, 98, 99, 100, 65, 65, 65, 65, 65, 65, 65, 65, 65, 65, 65, 65, 49, 89, 68)
    val byteBuffer = ByteBuffer.wrap(total)
    //    val descs: List[CharSequence] = List("apples", "oranges", "pears")
    val descs = new util.ArrayList[CharSequence]()
    descs.add("cola")
    descs.add("apples")
    descs.add("oranges")
    descs.add("pears")

    val timestamp = (new Date()).getTime

    //    var colors:Map[CharSequence,CharSequence] = Map()
    //    colors = Map("red" -> "#FF0000", "azure" -> "#F0FFFF")

    val extras = new util.HashMap[CharSequence, CharSequence]
    extras.put("red", "#FF0000")
    extras.put("azure", "#F0FFFF")


    val mediaData = new MediaData
    mediaData.setData(byteBuffer)
    mediaData.setType(1)

    val rawLog = new RawLog
    rawLog.setDescs(descs)
    rawLog.setTimestamp(timestamp)
    rawLog.setLevel("001")
    rawLog.setExtras(extras)

    val logs = new util.ArrayList[RawLog]
    logs.add(rawLog)


    val svc = new SvcLog
    svc.setCallName("qjjia")
    svc.setIp(1921681120)
    svc.setLogs(logs)
    svc.setMediaData(mediaData)
    svc.setSid("atl1921681120")
    svc.setTimestamp(timestamp)
    svc.setType(2)
    svc.setUid("370272561")

    val baos = SvcLogAvroUtil.serialize(svc)

    /**
      * 测试  Demo
      */
    //    val demo = new Demo
    //    demo.setLeft("left")
    //    demo.setRight("right")
    //
    //    val baos = DemoAvroUtil.serialize(demo)
    println(baos)

    /**
      * 读avro文件
      */
    val parser: Schema.Parser = new Schema.Parser()
    val schema: Schema = parser.parse(new File("svc.avsc"))
    val record = ReadAvro.deserializeRecord(schema, baos)
    println(record)

    val bc:BizConstants = new BizConstants;
    test(bc,record);


    // 写avro文件
    //    val outName = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date())
    //    val outfile = new File("output/demo" + outName + ".avro")
    //    val fos = new FileOutputStream(outfile)
    //    fos write baos.toByteArray
    //    baos.close()
    //    fos.close()
    //
    //    val parser: Schema.Parser = new Schema.Parser()
    //    val schema: Schema = parser.parse(new File("svc.avsc"))
    //
    //    val record = ReadAvro.deserialize(schema, baos)
    //
    //    println(record)

    // 使用JsonUtils工具
    //    val recordMap = JsonUtils.json2Map(record)
    //
    //    println(recordMap.get("type"))
    //    println(recordMap.get("uid"))
    //    val llogs = recordMap.get("mediaData")
    //    println(llogs)


  }

  def test(bc: BizConstants,record: GenericRecord): Unit = {
    var result: String = null;
    result = GroovyCommonUtil.invokeMethod("aloha.groovy", "parse", bc,record).asInstanceOf[String];
    println(result)
  }
}
