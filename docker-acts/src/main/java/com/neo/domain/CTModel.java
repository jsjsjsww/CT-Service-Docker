package com.neo.domain;

import java.util.ArrayList;

public class CTModel {
  private int parameter;
  private int strength;
  private int[] values;
  private ArrayList<String> constraint;
  private ArrayList<int[]> seed;
  private ArrayList<int[]> relation;

  public CTModel(int parameter, int strength, int[] values, ArrayList<String> constraint, ArrayList<int[]> seed, ArrayList<int[]> relation){
    this.parameter = parameter;
    this.strength = strength;
    this.values = values;
    this.constraint = constraint;
    this.seed = seed;
    this.relation = relation;
  }

  public int getParameter() {
	return parameter;
  }

  public int getStrength() {
    return strength;
  }

  public int[] getValues() {
	return values;
  }

  public ArrayList<String> getConstraint() {
    return constraint;
  }

  public ArrayList<int[]> getRelation() {
    return relation;
  }

  public ArrayList<int[]> getSeed() {
    return seed;
  }
}
