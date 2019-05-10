package com.neo.domain;


public interface TestSuite {
  void addTest(TestCase test);

  void deleteTest(int index);

  TestCase getAt(int index);

  void setOneTestCaseState(int index, int value);

  int getTestCaseNum();

  TestSuite getInfoTestCases(int state);

  void clear();
}
