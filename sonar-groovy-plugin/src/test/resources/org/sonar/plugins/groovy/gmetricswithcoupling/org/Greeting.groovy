package org;

import org.foo.Foo;
import org.bar.Bar;

class Greeting {

  def void sayHello() {
    new Bar().sayHello()
    new Foo().sayHello()
  }

}
