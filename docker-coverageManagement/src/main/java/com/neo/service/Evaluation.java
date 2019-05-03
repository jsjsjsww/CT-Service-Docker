package com.neo.service;


import java.util.ArrayList;

public class Evaluation {

  private int t;
  private int[] cover;
  private ArrayList<int[]> testsuite;
  private int parameters;
  private int n;
  private int[] values;

  public Evaluation(int t, ArrayList<int[]> testsuite, int parameters, int n, int[] values) {
	this.t = t;
	this.parameters = parameters;
	this.n = n;
	this.testsuite = testsuite;
	this.values = values;

	cover = new int[n + 1];
	for (int i = 0; i < this.n; i++)
	  cover[i] = 0;
  }

  public int c(int m, int n) {
	int n1 = 1;
	int n2 = 1;
	for (int i = 0; i < m; i++) {
	  n1 = n1 * (i + 1);
	  n2 = n2 * (n - i);
	}
	if (n2 < n1)
	  return 0;
	else
	  return n2 / n1;
  }

  public void calculateCoverage(){
	int paraCombination = c(t, parameters);
	// 标志每条测试用例中的每个组合是否第一次覆盖
	boolean[][] covered = new boolean[n][paraCombination];
	// 保存之前测试用例中出现的组合
	ArrayList<int[]> storedTuple;

	for (int i = 0; i < n; i++)
	  for (int j = 0; j < paraCombination; j++)
		covered[i][j] = false;

	int[] tuplePos = new int[t];
	for (int i = 0; i < t; i++)
	  tuplePos[i] = i;
	int column = -1;

	while (tuplePos[0] != parameters - t + 1) {
	  storedTuple = new ArrayList<>();
	  column++;

	  for (int i = 0; i < n; i++) {
		int j;
		for (j = 0; j < storedTuple.size(); j++) {
		  int k;
		  // 判断当前组合是否已经被覆盖
		  for (k = 0; k < t; k++) {
			if (storedTuple.get(j)[k] != testsuite.get(i)[tuplePos[k]])
			  break;
		  }
		  if (k == t)
			break;
		}
		// 该组合第一次覆盖
		if (j == storedTuple.size()) {
		  int[] tmp = new int[t];
		  for (int m = 0; m < t; m++)
		    tmp[m] = testsuite.get(i)[tuplePos[m]];
		  storedTuple.add(tmp);
		  covered[i][column] = true;
		}
	  }

	  tuplePos[t - 1]++;
	  for (int i = t - 1; i > 0; i--) {
		if (tuplePos[i] == parameters - t + i + 1) {
		  tuplePos[i - 1]++;
		  for (int j = 1; j < t - i + 1; j++)
			tuplePos[i - 1 + j] = tuplePos[i - 1] + j;
		}
	  }


	}

	for (int i = 0; i < n; i++)
	  for (int j = 0; j < paraCombination; j++)
	    if(covered[i][j])
			cover[i] ++;

	for(int i = 1; i < n; i++)
	  cover[i] += cover[i - 1];
	cover[n] = getCombinationNum(parameters, t, values);
  }

  public int[] getCover(){
    return cover;
  }

  public static void main(String[] args) {
	int parameters = 4;
	int n = 3;
	ArrayList<int[]> testsuite = new ArrayList<>();
	testsuite.add(new int[]{0, 0, 0, 0});
	testsuite.add(new int[]{1, 1, 1, 1});
	testsuite.add(new int[]{2, 2, 2, 2});
	int[] values = {3, 3, 3, 3};
	String[][] COVER = new String[6][n + 1];

	for (int t = 1; t <= 6; t++) {
	  if (t > parameters) {
		for (int i = 0; i < n; i++)
		  COVER[t - 1][i] = "0";
		COVER[t - 1][n] = "1";
	  } else {
	    Evaluation evaluation = new Evaluation(t, testsuite, parameters, n, values);
	    evaluation.calculateCoverage();
		int[] cover =evaluation.getCover();
		for (int i = 0; i < n + 1; i++) {
		  COVER[t - 1][i] = Integer.toString(cover[i]);
		}
		//COVER[t - 1][n] = Integer.toString(getCombinationNum(parameters, t, values));
	  }
	}
	for (int i = 0; i < 6; i++) {
	  for (int j = 0; j < COVER[0].length; j++) {
		System.out.print(COVER[i][j] + " ");
	  }
	  System.out.println();
	}
  }

  private static int getCombinationNum(int parameters, int t, int[] values) {
	int res = 0;
	int[] pos = new int[t];
	for (int i = 0; i < t; i++)
	  pos[i] = i;
	while (pos[0] != parameters - t + 1) {
	  int tmp = 1;
	  for (int i = 0; i < t; i++)
		tmp = tmp * values[pos[i]];
	  res += tmp;
	  pos[t - 1]++;
	  for (int i = t - 1; i > 0; i--) {
		if (pos[i] == parameters - t + i + 1) {
		  pos[i - 1]++;
		  for (int j = 1; j < t - i + 1; j++)
			pos[i - 1 + j] = pos[i - 1] + j;
		}
	  }
	}
	return res;
  }
}