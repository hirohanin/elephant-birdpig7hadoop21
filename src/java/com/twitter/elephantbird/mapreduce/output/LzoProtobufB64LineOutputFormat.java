package com.twitter.elephantbird.mapreduce.output;

import java.io.DataOutputStream;
import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.Message;
import com.hadoop.compression.lzo.LzoCodec;
import com.hadoop.compression.lzo.LzopCodec;
import com.twitter.elephantbird.mapreduce.io.ProtobufWritable;
import com.twitter.elephantbird.util.TypeRef;

/**
 * This is the base class for all base64 encoded, line-oriented protocol buffer based output formats.
 * Data is written as one base64 encoded serialized protocol buffer per line. It has two template
 * parameters, the protobuf and the writable for that protobuf.  This class cannot be instantiated
 * directly as an input format because Hadoop works via reflection, and Java type erasure makes it
 * impossible to instantiate a templatized class via reflection with the correct template parameter.
 * Instead, we codegen derived output format classes for any given protobuf which instantiate the
 * template parameter directly, as well as set the typeRef argument so that the template
 * parameter can be remembered.  See com.twitter.elephantbird.proto.HadoopProtoCodeGenerator.
 */

public abstract class LzoProtobufB64LineOutputFormat<M extends Message, W extends ProtobufWritable<M>>
    extends FileOutputFormat<NullWritable, W> {
  private static final Logger LOG = LoggerFactory.getLogger(LzoProtobufB64LineOutputFormat.class);

  protected TypeRef<M> typeRef_;

  protected void setTypeRef(TypeRef<M> typeRef) {
    typeRef_ = typeRef;
  }

  public RecordWriter<NullWritable, W> getRecordWriter(TaskAttemptContext job)
      throws IOException, InterruptedException {
    Configuration conf = job.getConfiguration();
    LzoCodec codec = new LzopCodec();
		codec.setConf(conf);

    Path file = getDefaultWorkFile(job, codec.getDefaultExtension());
    FileSystem fs = file.getFileSystem(conf);
    FSDataOutputStream fileOut = fs.create(file, false);

    return new LzoProtobufB64LineRecordWriter<M, W>(typeRef_,
        new DataOutputStream(codec.createOutputStream(fileOut)));
  }
}
