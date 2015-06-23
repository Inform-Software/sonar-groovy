package org.sonar.plugins.groovy.jacoco.tests;

import org.junit.Test;

class HelloTest {

  @Test
  void test_salute_full_coverage() {
    def hello = new Hello()
    assert hello != null
    assert hello.name == null
    assert hello.salute().equals("Hello!")

    hello = new Hello("Nico")
    assert hello != null
    assert hello.name != null
    assert hello.salute() == "Hello Nico!"
  }

  @Test
  void test_sayHelloToUnitTests() {
    def hello = new Hello()
    hello.sayHelloToUnitTests()
  }

  @Test
  void test_branches_partial_coverage() {
    def hello = new Hello()
    hello.branches(false);
  }

  @Test
  void test_inner_class() {
    def hello = new Hello()
    hello.bar()
  }
}
