package com.neo.service;

import java.util.ArrayList;
import java.util.Random;


public class Reduce {
  private int parameters;                 // 参数个数;
  private int t;                 // 维度;
  private int[] values;               // 参数取值;
  private ArrayList<int[]> testsuite;             // 覆盖表;
  private int size;             // 覆盖表长度;
  private int maxIteration;      //最大迭代次数;
  private boolean[][] RFS;        //RFS;
  private boolean[][] FS1;        //FS1;
  private int[] EachOfRFS;   //每行的灵活位置数目;
  private Random random = new Random();

  public Reduce(int parameters, int t, int[] values, ArrayList<int[]> testsuite, int size) {
	this.parameters = parameters;
	this.t = t;
	this.values = values;
	this.size = size;
	this.testsuite = testsuite;

	EachOfRFS = new int[size];
	RFS = new boolean[size][parameters];
	FS1 = new boolean[size][parameters];

	for (int i = 0; i < size; i++) {
	  EachOfRFS[i] = 0;
	  for (int j = 0; j < parameters; j++) {
		RFS[i][j] = true;
		FS1[i][j] = true;
	  }
	}
	maxIteration = 500;
  }

  private boolean isSame(int[] a, int[] b, int k) {
	for (int i = 0; i < k; i++) {
	  if (a[i] != b[i])
		return false;
	}
	return true;
  }

  private boolean checkArray(boolean[] boolArray, int m) {
	for (int i = 0; i < m; i++)
	  if (boolArray[i]) return false;
	return true;
  }

  private int count(boolean[] boolArray, int m, boolean bool) {
	int res = 0;
	for (int i = 0; i < m; i++)
	  if (boolArray[i] == bool)
		res++;
	return res;
  }

  private int multiply(int[] values, int m) {
	int res = 1;
	for (int i = 0; i < m; i++)
	  res = res * values[i];
	return res;
  }

  private int getIndex(int[] values, int[] testcase, int m) {
	int res = 0, base = 1;
	for (int i = 0; i < m; i++) {
	  res += testcase[i] * base;
	  base *= values[i];
	  // res += testcase[i] * multiply(values, i);
	}
	return res;
  }

  // get all don't care position, assign -1
  private int[][] constructFS(int[][] testsuite, int len) {
	int[][] res = new int[len][parameters];
	boolean[][] FS = new boolean[len][parameters];
	for (int i = 0; i < len; i++) {
	  for (int j = 0; j < parameters; j++) {
		FS[i][j] = true;
		res[i][j] = testsuite[i][j];
	  }
	}
	int[] tuplePos = new int[t];
	for (int i = 0; i < t; i++)
	  tuplePos[i] = i;
	boolean[] combinations;
	while (tuplePos[0] != parameters - t + 1) {             //n列全部遍历;
	  int[] tupleVal = new int[t];
	  for (int i = 0; i < t; i++)
		tupleVal[i] = values[tuplePos[i]];
	  int combinationNum = multiply(tupleVal, t);
	  combinations = new boolean[combinationNum];
	  for (int i = 0; i < combinationNum; i++)
		combinations[i] = false;
	  for (int j = 0; j < len; j++) {
		int[] tuple = new int[t];          //记录当前组合;
		for (int i = 0; i < t; i++) {
		  tuple[i] = testsuite[j][tuplePos[i]];
		}
		int index = getIndex(tupleVal, tuple, t);
		if (!combinations[index]) {
		  combinations[index] = true;
		  for (int i = 0; i < t; i++) {
			FS[j][tuplePos[i]] = false;
		  }
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
	for (int i = 0; i < len; i++) {
	  for (int j = 0; j < parameters; j++) {
		if (FS[i][j])
		  res[i][j] = -1;
	  }
	}
	return res;
  }

  private int matchCount(int[] array, int k, int target) {
	int res = 0;
	for (int i = 0; i < k; i++) {
	  if (array[i] == target)
		res++;
	}
	return res;
  }

  // try to reduce a testsuite
  private int[][] replaceFS(int[][] testsuite, int len) {
	int[][] res = new int[len][parameters];
	int maxRow = 0, max = 0;
	int row = 0;
	for (int i = 0; i < len; i++) {
	  int count = matchCount(testsuite[i], parameters, -1);
	  // if all positions are -1, drop this test case
	  if (count != parameters) {
		for (int j = 0; j < parameters; j++)
		  res[row][j] = testsuite[i][j];
		if (count > max) {
		  max = count;
		  maxRow = row;
		}
		row++;
	  }
	}
	for (int i = 0; i < row; i++) {
	  for (int j = 0; j < parameters; j++) {
		if (res[i][j] == -1 && i != maxRow) {
		  if (res[maxRow][j] == -1) {

			//res[i][j] = random.nextInt(values[j] - 1);
			res[i][j] = random.nextInt(values[j]);
			//   res[i][j] = 0;
		  } else
			res[i][j] = res[maxRow][j];
		}
	  }
	}
	// move maxRow to last row
	for (int i = 0; i < parameters; i++) {
	  int tmp = res[maxRow][i];
	  res[maxRow][i] = res[row - 1][i];
	  res[row - 1][i] = tmp;
	}

	// 将ts[row - 1]中所有灵活位置找出;
	int[] tuplePos = new int[t];
	for (int i = 0; i < t; i++)
	  tuplePos[i] = i;
	int[] tuple = new int[t];
	boolean[] mark = new boolean[parameters];
	for (int i = 0; i < parameters; i++)
	  mark[i] = true;
	while (tuplePos[0] != parameters - t + 1) {            //n列全部遍历;
	  for (int i = 0; i < t; i++)
		tuple[i] = res[row - 1][tuplePos[i]];
	  if (contains(tuple, t, -1)) {                        //不含-1;
		boolean isFlexible = false;
		int[] tmpTuple = new int[t];
		for (int k = 0; k < row - 1; k++) {
		  for (int i = 0; i < t; i++)
			tmpTuple[i] = res[k][tuplePos[i]];
		  if (isSame(tuple, tmpTuple, t)) {
			isFlexible = true;
			break;
		  }
		}
		if (!isFlexible) {                      //非绝对灵活位置
		  for (int i = 0; i < t; i++)
			mark[tuplePos[i]] = false;
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
	int flexibleNum = count(mark, t, false);
	if (flexibleNum == 0) {
	  row--;
	} else {
	  for (int i = 0; i < parameters; i++)
		if (res[row - 1][i] == -1)
		  res[row - 1][i] = random.nextInt(values[i]);
	}
	size = row;
	return res;
  }

  private boolean contains(int[] array, int k, int target) {
	for (int i = 0; i < k; i++)
	  if (array[i] == target)
		return false;
	return true;
  }

  public void run() {
	int[][] CA = new int[size][parameters];
	int size;
	int iteration = 0;
	while (iteration < maxIteration) {
	  for (int i = 0; i < this.size; i++) {
		for (int j = 0; j < parameters; j++) {
		  RFS[i][j] = true;
		  FS1[i][j] = true;
		}
	  }
	  for (int i = 0; i < this.size; i++) {
		for (int j = 0; j < parameters; j++)
		  CA[i][j] = testsuite.get(i)[j];
	  }
	  size = this.size;
	  /////////////////////////
	  //获取RFS:
	  int[] tuplePos = new int[t];
	  for (int i = 0; i < t; i++)
		tuplePos[i] = i;
	  while (tuplePos[0] != parameters - t + 1){             //n列全部遍历;
		int[] tuple1 = new int[t];          //记录当前组合;
		int[] tuple2 = new int[t];          //记录当前组合;
		boolean[] flexible = new boolean[t];  //判断当前组合对是否已确认非灵活;
		for (int j = 0; j < size; j++) {
		  for (int i = 0; i < t; i++) {
			tuple1[i] = CA[j][tuplePos[i]];
			flexible[i] = RFS[j][tuplePos[i]];
		  }
		  if (!checkArray(flexible, t)) {        //这些位置未被确定是否属于RFS;
			boolean isFlexible = false;
			for (int k = 0; k < size; k++) {
			  if (k != j) {
				for (int i = 0; i < t; i++)
				  tuple2[i] = CA[k][tuplePos[i]];
				if (isSame(tuple1, tuple2, t)) {
				  isFlexible = true;
				  break;
				}
			  }
			}
			if (!isFlexible) {                     //非绝对灵活位置
			  for (int i = 0; i < t; i++)
				RFS[j][tuplePos[i]] = false;
			}
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
	  ///对CA进行灵活度从高到低排序;
	  for (int i = 0; i < size; i++)
		EachOfRFS[i] = count(RFS[i], parameters, true);
	  for (int i = 1; i < size; i++) {
		int Con = EachOfRFS[i];                //记录该用例行号;
		int record = i;                        //标记该用例初始行号;
		for (int j = i - 1; j >= 0; j--) {
		  if (EachOfRFS[j] <= Con) {
			break;
		  } else {                                          //调整交换;
			for (int s = 0; s < parameters; s++) {
			  int tmp = CA[j][s];
			  CA[j][s] = CA[record][s];
			  CA[record][s] = tmp;
			}

			int tmp = EachOfRFS[j];
			EachOfRFS[j] = EachOfRFS[record];
			EachOfRFS[record] = tmp;
			record = j;
		  }
		}
	  }


	  CA = constructFS(CA, size);                               //生成FS1;

	  CA = replaceFS(CA, size);                                 //ReplaceFS1;

	  CA = constructFS(CA, this.size);                               //生成FS2;

	  CA = replaceFS(CA, this.size);                                 //ReplaceFS2;

	  testsuite = new ArrayList<>();
	  for (int i = 0; i < this.size; i++) {
	    testsuite.add(CA[i]);
	  }
	  iteration++;
	}

  }

  public ArrayList<int[]> getTestsuite() {
	return testsuite;
  }


}