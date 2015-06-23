package org.sonar.plugins.groovy.jacoco.tests;

import org.junit.Assert;
import org.junit.Test;

public class FooIT {

  @Test
  public void test() {
    Foo f = new Foo(0);
    Assert.assertTrue(f.isGreatherThan(new Foo(-1)));
  }
}
