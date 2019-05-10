package com.neo.domain;

/**
 * the interface identify the test case
 *
 * @author Xintao Niu
 */
public interface TestCase {
  int UNTESTED = 0;
  int PASSED = 1;
  int FAILED = -1;

  int getAt(int index);

  void set(int index, int value);

  int testDescription();

  void setTestState(int state);

  int getLength();

  String getStringOfTest();

  boolean containsOf(Tuple tuple);

  TestCase copy();

  boolean equals(Object t);
}
