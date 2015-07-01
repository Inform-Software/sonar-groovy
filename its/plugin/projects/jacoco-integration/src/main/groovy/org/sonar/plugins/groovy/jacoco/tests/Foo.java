package org.sonar.plugins.groovy.jacoco.tests;

public class Foo {

  int a;

  public Foo(int a) {
    this.a = a;
  }

  boolean isGreatherThan(Foo foo) {
    return this.a > foo.a;
  }

}
