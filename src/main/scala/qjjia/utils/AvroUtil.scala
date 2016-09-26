package qjjia.utils

import java.io.{ByteArrayOutputStream, File}
import java.text.SimpleDateFormat
import java.util.Date

import org.apache.avro.Schema
import org.apache.avro.file.{DataFileReader, DataFileWriter}
import org.apache.avro.generic.{GenericData, GenericDatumReader, GenericDatumWriter, GenericRecord}
import org.apache.avro.io._
import org.apache.avro.reflect.ReflectDatumWriter
import org.apache.avro.specific.SpecificDatumWriter
import qjjia.model.log.{Demo, SvcLog}

/**
  * Created by Administrator on 2016/8/26.
  */

object SvcLogAvroUtil {
  val baos: ByteArrayOutputStream = new ByteArrayOutputStream()

    val parser: Schema.Parser = new Schema.Parser()
    val schema: Schema = parser.parse(new File("svc.avsc"))
  val encoder: Encoder = EncoderFactory.get.binaryEncoder(baos, null)
  val writer: DatumWriter[SvcLog] = new ReflectDatumWriter(schema)
  //  val writer: DatumWriter[SvcLog] = new ReflectDatumWriter(new Schema.Parser().parse(this.getClass.getResourceAsStream("/svc.avsc")))

  /**
    * Do serialize.
    *
    * @param svcLog instances.
    * @return outputstream.
    */
  def serialize(svcLog: SvcLog): ByteArrayOutputStream = {
    baos.reset()
    writer.write(svcLog, encoder)
    encoder.flush()
    baos
  }

}

object DemoAvroUtil {


  val baos: ByteArrayOutputStream = new ByteArrayOutputStream()
  /**
    * generic
    */
  //  val parser: Schema.Parser = new Schema.Parser()
  //  val schema: Schema = parser.parse(new File("demo.avsc"))
  //  val datum: GenericRecord = new GenericData.Record(schema)
  //  datum.put("left", "L")
  //  datum.put("right", "R")
  //  val writer: DatumWriter[GenericRecord] = new GenericDatumWriter[GenericRecord](schema)
  //  val encoder: Encoder = EncoderFactory.get().binaryEncoder(baos, null)

  /**
    * specific
    *
    */
  val encoder: Encoder = EncoderFactory.get.binaryEncoder(baos, null)
  val writer: DatumWriter[Demo] = new SpecificDatumWriter[Demo](Demo.getClassSchema)

  /**
    * Do serialize.
    *
    * @param demo instances.
    * @return outputstream.
    */
  def serialize(demo: Demo): ByteArrayOutputStream = {
    baos.reset()
    writer.write(demo, encoder)
    encoder.flush()
    baos.close()
    baos
  }
}

object AvroNoCode {

  def main(args: Array[String]): Unit = {
    val schema = new Schema.Parser().parse(new File(
      "demo.avsc"))
    val user1 = new GenericData.Record(schema)
    println(user1)
    user1.put("left", "left")
    user1.put("right", "right")
    val user2 = new GenericData.Record(schema)
    println(user2)
    user2.put("left", "right")
    user2.put("right", "left")


    println("create user:")
    println(user1)
    println(user2)

    val iString = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date())
    val outfile = new File("output/demo" + iString + ".avro")
    val datumWriter = new GenericDatumWriter[GenericRecord](
      schema)
    val dataFileWriter = new DataFileWriter[GenericRecord](
      datumWriter)
    dataFileWriter.create(schema, outfile)
    dataFileWriter.append(user1)
    dataFileWriter.append(user2)
    dataFileWriter.close()

    val datumReader = new GenericDatumReader[GenericRecord](schema)
    val dataFileReader = new DataFileReader[GenericRecord](outfile, datumReader)

    var user = null: GenericRecord
    while (dataFileReader.hasNext()) {
      user = dataFileReader.next(user)
      println(user)
    }

    println("end")
  }

}

