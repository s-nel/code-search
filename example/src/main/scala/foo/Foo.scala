package foo

import bar.Bar
import com.typesafe.scalalogging.Logger

class Foo(val z: Long, val bar: Bar) {
  def fullStrFoo(num: Int): Bar = {
    new Bar(z.toString + bar.h + num.toString)
  }

  def explode(): Unit = {
    throw new Exception("BOOM!")
  }
}

object Foo {
  val logger = Logger[Foo]

  def main(args: Array[String]): Unit = {
    try {
      new Foo(1L, new Bar("b")).explode()
    } catch {
      case e: Exception => logger.error("Error in Foo", e)
    }
  }
}
