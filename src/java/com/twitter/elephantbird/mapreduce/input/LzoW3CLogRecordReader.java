package com.twitter.elephantbird.mapreduce.input;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;

import com.twitter.elephantbird.util.W3CLogParser;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.MapWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.util.LineReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A reader for LZO-compressed W3C-style log formatted files.  See discussion in the
 * LzoW3CLogInputFormat for more on the format.  To use, derive from this class and implement
 * the getFieldDefinitionFile() method.
 */
public abstract class LzoW3CLogRecordReader extends LzoRecordReader<LongWritable, MapWritable> {
  private static final Logger LOG = LoggerFactory.getLogger(LzoW3CLogRecordReader.class);

  private LineReader in_;

  private final LongWritable key_ = new LongWritable();
  private final Text currentLine_ = new Text();
  private final MapWritable value_ = new MapWritable();
  protected W3CLogParser w3cLogParser_ = null;

  @Override
  public synchronized void close() throws IOException {
    if (in_ != null) {
      in_.close();
    }
  }

  @Override
  public LongWritable getCurrentKey() throws IOException, InterruptedException {
    return key_;
  }

  @Override
  public MapWritable getCurrentValue() throws IOException, InterruptedException {
    return value_;
  }

  @Override
  protected void createInputReader(InputStream input, Configuration conf) throws IOException {
    in_ = new LineReader(input, conf);

    String fileURI = getFieldDefinitionFile();
    FileSystem fs = FileSystem.get(URI.create(fileURI), conf);
    InputStream is = fs.open(new Path(fileURI));
    w3cLogParser_ = new W3CLogParser(is);
    is.close();
  }

  @Override
  protected void skipToNextSyncPoint(boolean atFirstRecord) throws IOException {
    if (!atFirstRecord) {
      in_.readLine(new Text());
    }
  }

  @Override
  public boolean nextKeyValue() throws IOException, InterruptedException {
    // Since the lzop codec reads everything in lzo blocks, we can't stop if pos == end.
    // Instead we wait for the next block to be read in, when pos will be > end.
    value_.clear();

    while (pos_ <= end_) {
      key_.set(pos_);

      int newSize = in_.readLine(currentLine_);
      if (newSize == 0) {
        return false;
      }

      pos_ = getLzoFilePos();

      if (!decodeLine()) {
        continue;
      }

      return true;
    }

    return false;
  }

  protected boolean decodeLine() {
    try {
      Map<String, String> w3cLogFields = w3cLogParser_.parse(currentLine_.toString());
      for(Map.Entry<String, String> entrySet : w3cLogFields.entrySet()) {
        String value = entrySet.getValue();
        value_.put(new Text(entrySet.getKey()), new Text(value));
      }
      return true;
    } catch (IOException e) {
      LOG.warn("Could not w3c-decode string: " + currentLine_, e);
      return false;
    }
  }

  protected abstract String getFieldDefinitionFile();
}

