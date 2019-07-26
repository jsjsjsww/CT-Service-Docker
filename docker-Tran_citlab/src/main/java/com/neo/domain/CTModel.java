package com.neo.domain;

import java.util.ArrayList;

public class CTModel {
  private int parameter;
  private int strength;
  private int[] values;
  private ArrayList<String> constraint;
  private ArrayList<int[]> seed;


  public CTModel(){
    strength = 2;
    constraint = new ArrayList<>();
    seed = new ArrayList<>();
  }

  public CTModel(int parameter, int strength, int[] values, ArrayList<String> constraint, ArrayList<int[]> seed){
    this.parameter = parameter;
    this.strength = strength;
    this.values = values;
    this.constraint = constraint;
    this.seed = seed;
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

  public void setParameter(int parameter) {
    this.parameter = parameter;
  }

  public void setStrength(int strength) {
    this.strength = strength;
  }

  public void setValues(int[] values) {
    this.values = values;
  }

  public void setConstraint(ArrayList<String> constraint) {
    this.constraint = constraint;
  }

  public void setSeed(ArrayList<int[]> seed) {
    this.seed = seed;
  }


  public ArrayList<String> getConstraint() {
    return constraint;
  }


  public ArrayList<int[]> getSeed() {
    return seed;
  }
}
