package com.twitter.elephantbird.pig.piggybank;

import java.io.IOException;

import org.apache.pig.EvalFunc;
import org.apache.pig.data.DataByteArray;
import org.apache.pig.data.Tuple;
import org.apache.thrift.TBase;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TMemoryBuffer;

import com.twitter.elephantbird.util.TypeRef;

/**
 * This is an abstract UDF for converting serialized Thrift objects into Pig tuples.
 * To create a converter for your Thrift class <code>MyThriftClass</code>, you simply need to extend
 * <code>BytesToThriftTuple</code> with something like this:
 *<pre>
 * {@code
 * public class BytesToSimpleLocation extends BytesToThriftTuple<MyThriftClass> {
 *
 *   public BytesToSimpleLocation() {
 *     setTypeRef(new TypeRef<MyThriftClass>() {});
 *   }
 * }}
 *</pre>
 */
public abstract class BytesToThriftTuple<T extends TBase<?>> extends EvalFunc<Tuple> {

  private final TDeserializer deserializer_ = new TDeserializer(new TBinaryProtocol.Factory());
private final ThriftToTuple<T> thriftToTuple_ = new ThriftToTuple<T>();
  private TypeRef<T> typeRef_;
  private T thriftObj_ = null;

  /**
   * Set the type parameter so it doesn't get erased by Java.  Must be called by the constructor!
   *
   * @param typeRef
   */
  public void setTypeRef(TypeRef<T> typeRef) {
    typeRef_ = typeRef;
  }


  @Override
  public Tuple exec(org.apache.pig.data.Tuple input) throws IOException {
    if (input == null || input.size() < 1) return null;
    try {
      if (thriftObj_ == null) {
        thriftObj_ = typeRef_.safeNewInstance();
      }
      DataByteArray dbarr = (DataByteArray) input.get(0);
      deserializer_.deserialize(thriftObj_, dbarr.get());
      return thriftToTuple_.convert(thriftObj_);
    } catch (IOException e) {
      log.warn("Caught exception "+e.getMessage());
      return null;
    } catch (TException e) {
      log.warn("Unable to deserialize Thrift object: "+e);
      return null;
    }
  }
}
