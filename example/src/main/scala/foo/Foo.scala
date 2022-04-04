package foo

import bar.Bar

class Foo(val z: Long, val bar: Bar) {
  def fullStrFoo(num: Int): Bar = {
    new Bar(z.toString + bar.h + num.toString)
  }

  def explode(): Unit = {
    throw new Exception("BOOM!")
  }
}
