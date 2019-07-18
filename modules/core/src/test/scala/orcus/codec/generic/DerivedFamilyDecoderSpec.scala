package orcus.codec.generic

import java.{util => ju}

import orcus.codec.FamilyDecoder
import org.apache.hadoop.hbase.util.Bytes
import org.scalatest.FlatSpec

class DerivedFamilyDecoderSpec extends FlatSpec {
  import derived._

  case class Foo(a: Int)

  it should "decode a case class" in {
    val t = new ju.TreeMap[Array[Byte], Array[Byte]](Bytes.BYTES_COMPARATOR)
    t.put(Bytes.toBytes("a"), Bytes.toBytes(10))
    val Right(x) = FamilyDecoder[Foo].apply(t)

    assert(x === Foo(10))
  }

  it should "fail decode when the require property is absent" in {
    val t = new ju.TreeMap[Array[Byte], Array[Byte]](Bytes.BYTES_COMPARATOR)
    val x = FamilyDecoder[Foo].apply(t)

    assert(x.isLeft)
  }
}