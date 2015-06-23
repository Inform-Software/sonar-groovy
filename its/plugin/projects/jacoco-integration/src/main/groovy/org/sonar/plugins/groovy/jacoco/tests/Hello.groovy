package org.sonar.plugins.groovy.jacoco.tests;

class Hello {

  def name
  def innerClass

  Hello(name) {
    this.name = name
    this.innerClass = new InnerClass()
  }

  String salute() {
    if (name != null) {
      return "Hello $name!"
    }
    return "Hello!"
  }

  def sayHelloToUnitTests() {
    println "Hello Unit Tests"
  }

  def sayHelloToIntegrationTests() {
    println "Hello Integration Tests"
  }

  def branches(boolean test) {
    if (test) {
      println "branch 1"
    } else {
      println "branch 2"
      println "branch 2 bis"
    }
  }

  def notcovered() {
    println "Not covered"
  }

  def bar() {
    innerClass.foo()
  }

  class InnerClass {
    void foo() {
      println "foo"
    }
  }
}
