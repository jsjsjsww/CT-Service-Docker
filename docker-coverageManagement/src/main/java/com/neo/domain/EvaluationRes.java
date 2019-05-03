package com.neo.domain;

public class EvaluationRes {
  private int[] array;
  private long time;


  public EvaluationRes(int[] array, long time){
    this.array = array;
    this.time = time;
  }
  public int[] getArray() {
	return array;
  }

  public long getTime() {
	return time;
  }

  public void setArray(int[] array) {
	this.array = array;
  }

  public void setTime(long time) {
	this.time = time;
  }
}
