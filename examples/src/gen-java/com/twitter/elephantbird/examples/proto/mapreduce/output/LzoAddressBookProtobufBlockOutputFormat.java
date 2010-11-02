package com.twitter.elephantbird.examples.proto.mapreduce.output;

import com.twitter.elephantbird.examples.proto.AddressBookProtos.AddressBook;
import com.twitter.elephantbird.examples.proto.mapreduce.io.ProtobufAddressBookWritable;
import com.twitter.elephantbird.mapreduce.output.LzoProtobufBlockOutputFormat;
import com.twitter.elephantbird.util.TypeRef;

public class LzoAddressBookProtobufBlockOutputFormat extends LzoProtobufBlockOutputFormat<AddressBook, ProtobufAddressBookWritable> {
  public LzoAddressBookProtobufBlockOutputFormat() {
    setTypeRef(new TypeRef<AddressBook>(){});
  }
}

