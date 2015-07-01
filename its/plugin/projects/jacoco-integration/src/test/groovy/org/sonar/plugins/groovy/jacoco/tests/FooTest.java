package org.sonar.plugins.groovy.jacoco.tests;

import org.junit.Assert;
import org.junit.Test;

public class FooTest {

  @Test
  public void test() {
    Foo f = new Foo(0);
    Assert.assertFalse(f.isGreatherThan(f));
  }
}
