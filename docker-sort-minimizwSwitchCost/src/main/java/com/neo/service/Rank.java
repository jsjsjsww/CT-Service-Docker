package com.neo.service;

import java.util.Random;
import java.util.ArrayList;

public class Rank {
  private int parameter;
  private int[] weight;// 权重
  private int num;// 覆盖表条数
  private ArrayList<int[]> testsuite;// 原始覆盖表
  private ArrayList<int[]> sortedTestsuite;// 排序后覆盖表
  private Random random;

  public Rank(int parameter, int num, int[] weight, ArrayList<int[]> testsuite) {
	this.parameter = parameter;
	this.num = num;
	this.weight = weight;
	this.testsuite = testsuite;
	sortedTestsuite = new ArrayList<>();
	random = new Random();
  }

  public void sort() {
	int first = random.nextInt(num);
	//System.out.println("first = " + first);
	boolean[] mark = new boolean[num];
	for (int i = 0; i < num; i++)
	  mark[i] = false;
	sortedTestsuite.add(testsuite.get(first));
	mark[first] = true;

	int size = 0;
	int next;
	while (size < num - 1) {
	  int bestWeight = Integer.MAX_VALUE;
	  next = -1;
	  for (int i = 0; i < num; i++)
		if (!mark[i]) {
		  int weight = 0;
		  for (int j = 0; j < parameter; j++)
			if (sortedTestsuite.get(size)[j] != this.testsuite.get(i)[j])
			  weight += this.weight[j];
		  if (weight < bestWeight) {
			bestWeight = weight;
			next = i;
		  }
		}
	  //System.out.println("next = " + next);
	  size++;
	  sortedTestsuite.add(testsuite.get(next));
	  mark[next] = true;
	}
  }

  public ArrayList<int[]> getSortedTestsuite() {
	return sortedTestsuite;
  }

  //测试代码
  public static void main(String[] args) {
	ArrayList<int[]> ts = new ArrayList<>();
	ts.add(new int[]{0, 1, 2, 0});
	ts.add(new int[]{1, 2, 2, 2});
	ts.add(new int[]{2, 1, 1, 1});
	ts.add(new int[]{0, 0, 0, 0});
	ts.add(new int[]{2, 2, 2, 0});
	ts.add(new int[]{1, 1, 1, 1});
	int[] weight = new int[]{1, 1, 1, 1};
	Rank rank = new Rank(4, 6, weight, ts);
	rank.sort();
	ArrayList<int[]> result = rank.getSortedTestsuite();
	for (int i = 0; i < 6; i++) {
	  for (int j = 0; j < 4; j++)
		System.out.print(result.get(i)[j] + " ");
	  System.out.println();
	}

  }
}

