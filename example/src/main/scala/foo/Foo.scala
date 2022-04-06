package foo

import bar.Bar
import co.elastic.apm.api.{ElasticApm, Span, Transaction}
import co.elastic.apm.attach.ElasticApmAttacher
import com.typesafe.scalalogging.Logger
import foo.Foo.withSpan

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

class Foo(val z: Long, val bar: Bar) {
  def fullStrFoo(num: Int)(implicit span: Span): Bar = {
    withSpan {
      new Bar(z.toString + bar.h + num.toString)
    }
  }

  def explode()(implicit span: Span): Unit = {
    withSpan {
      throw new Exception("BOOM!")
    }
  }
}

object Foo {
  val logger = Logger[Foo]

  def withSpan[A](f: => A)(implicit parent: Span): A = {
    val span = parent.startSpan()
    new Throwable().getStackTrace.lift(1).foreach { callerStack =>
      span.setName(s"${callerStack.getClassName}.${callerStack.getMethodName}")
      span.setLabel("log.origin.function", callerStack.getMethodName)
      span.setLabel("log.origin.file.name", callerStack.getFileName)
      span.setLabel("log.origin.file.line", callerStack.getLineNumber)
      span.setLabel("log.logger", callerStack.getClassName)
    }
    val result = Try(f)
    result match {
      case Failure(exception) => span.captureException(exception)
      case _ =>
    }
    span.end()
    result.get
  }

  def main(args: Array[String]): Unit = {
    ElasticApmAttacher.attach()
    implicit val transaction = ElasticApm.startTransaction()
    transaction.setName("foo")
    withSpan {
      Try {
        val foo = new Foo(1L, new Bar("b"))
        foo.fullStrFoo(5)
        foo.explode()
      } match {
        case Failure(exception) => logger.error("Error in Foo", exception)
        case Success(value) => logger.info(s"Result = ${value}")
      }
    }
    transaction.end()
  }
}
