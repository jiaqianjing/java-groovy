package qjjia.utils

import java.io.ByteArrayOutputStream

import org.apache.avro.Schema
import org.apache.avro.generic.{GenericDatumReader, GenericRecord}
import org.apache.avro.io.{DatumReader, Decoder, DecoderFactory}

/**
  * Created by Administrator on 2016/9/26.
  */
object ReadAvro {

  def deserializeString(schema: Schema, baos: ByteArrayOutputStream): String = {
    val reader: DatumReader[GenericRecord] = new GenericDatumReader[GenericRecord](schema)
    val decoder: Decoder = DecoderFactory.get().binaryDecoder(baos.toByteArray(), null)
    val result: GenericRecord = reader.read(null, decoder)
    result.toString
  }

  def deserializeRecord(schema: Schema, baos: ByteArrayOutputStream): GenericRecord = {
    val reader: DatumReader[GenericRecord] = new GenericDatumReader[GenericRecord](schema)
    val decoder: Decoder = DecoderFactory.get().binaryDecoder(baos.toByteArray(), null)
    val result: GenericRecord = reader.read(null, decoder)
    result
  }
}
