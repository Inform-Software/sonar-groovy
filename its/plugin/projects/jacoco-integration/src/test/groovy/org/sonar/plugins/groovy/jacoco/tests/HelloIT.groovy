package org.sonar.plugins.groovy.jacoco.tests;

import org.junit.Test;

class HelloIT {

  @Test
  void test_sayHelloToIntegrationTests() {
    def hello = new Hello()
    hello.sayHelloToIntegrationTests()
    assert hello != null
  }
}
