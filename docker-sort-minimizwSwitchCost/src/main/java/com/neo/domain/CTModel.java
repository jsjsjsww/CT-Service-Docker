package com.neo.domain;

import java.util.ArrayList;

public class CTModel {
  private int parameter;
  private int[] weight;
  private ArrayList<int[]> testsuite;

  public CTModel(int parameter, int[] weight, ArrayList<int[]> testsuite){
    this.parameter = parameter;
    this.weight = weight;
    this.testsuite = testsuite;
  }

  public int getParameter() {
	return parameter;
  }

  public int[] getWeight() {
	return weight;
  }

  public ArrayList<int[]> getTestsuite() {
    return testsuite;
  }
}
