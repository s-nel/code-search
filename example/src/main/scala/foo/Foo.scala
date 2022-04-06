package foo

import bar.Bar
import co.elastic.apm.api.{ElasticApm, Span, Transaction}
import co.elastic.apm.attach.ElasticApmAttacher
import com.typesafe.scalalogging.Logger

import scala.collection.JavaConverters._

class Foo(val z: Long, val bar: Bar, parentSpan: Span) {
  def fullStrFoo(num: Int): Bar = {
    val span = parentSpan.startSpan()
    span.setName("foo.Foo.fullStrFoo")
    val result = new Bar(z.toString + bar.h + num.toString)
    span.end()
    result
  }

  def explode(): Unit = {
    throw new Exception("BOOM!")
  }
}

object Foo {
  val logger = Logger[Foo]

  def main(args: Array[String]): Unit = {
    ElasticApmAttacher.attach()
    val transaction = ElasticApm.startTransaction()
    transaction.setName("foo.Foo$.main")
    try {
      val foo = new Foo(1L, new Bar("b"), transaction)
      foo.fullStrFoo(5)
      foo.explode()
    } catch {
      case e: Exception =>
        transaction.captureException(e)
        logger.error("Error in Foo", e)
    }
    transaction.end()
  }
}
