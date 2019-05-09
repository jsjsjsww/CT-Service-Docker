package com.neo.service.generator;



import com.neo.service.combinatorial.*;
import com.neo.service.common.*;
import com.neo.service.handler.ValidityChecker;

import java.util.ArrayList;

public class IPO implements CAGenerator {

  private CTModel IPOModel;
  private int[] orderedParameters;  // the non-increasing order of parameters

  private class PV {
	int val;
	int par;
  }

  /**
   * Calculate numbers of value combinations of every t parameters, store in array combinationNum
   *
   * @param combinationNum is initialized as [1,1,...,1]
   * @param value          parameter value
   * @param n              number of parameter
   * @param t              strength
   */
  private void calculateCombinationNumber(int[] combinationNum, int[] value, int n, int t) {
	for (int i = 0; i < combinationNum.length; i++)
	  combinationNum[i] = 1;
	// array p indicates which t parameters
	int[] p = new int[t];
	for (int i = 0; i < t; i++) {
	  p[i] = i;
	}
	int row = 0;
	while (p[0] != n - t + 1) {
	  for (int i = 0; i < t; i++)
		combinationNum[row] *= value[p[i]];
	  row++;
	  p[t - 1]++;
	  for (int i = t - 1; i > 0; i--) {
		if (p[i] == n - t + i + 1) {
		  p[i - 1]++;
		  for (int j = 1; j < t - i + 1; j++)
			p[i - 1 + j] = p[i - 1] + j;
		}
	  }
	}
  }

  /**
   * Calculate the column number of a value combination in coverage matrix.
   * For example, for two parameters with two values, the order is as:
   * [0, 0], [1, 0], [0, 1], [1, 1]
   *
   */
  private int value2ColumnNumber(int[] parVal, int[] parPos, int[] value, int t) {
	int res = 0;
	int base = 1;
	for (int i = 0; i < t; i++) {
	  res += parVal[i] * base;
	  base *= value[parPos[i]];
	}
	return res;
  }

  /**
   * Calculate the value combination (store in array a) of a given column.
   */
  private void columnNumber2value(int column, int[] a, int[] pos, int[] value, int t) {
	for (int i = 0; i < t; i++) {
	  a[i] = column % value[pos[i]];
	  column = (column - column % value[pos[i]]) / value[pos[i]];
	}
  }

  /**
   * Calculate the value for par, which appears the smallest times in previous test cases,
   * where row indicates parameter par's values in previous test cases
   */
  private int findMinimumOccurrenceValue(int[] tc, int[] row, int par, int value) {
	int[] count = new int[value];
	for (int i = 0; i < value; i++)
	  count[i] = 0;

	// calculate how many times each value of par appears in previous test case
	for (int i = 0; i < row.length; i++)
	  count[row[i]]++;

	int min = Integer.MAX_VALUE;
	int res = -1;
	int[] tmpTC = new int[tc.length];
	System.arraycopy(tc, 0, tmpTC, 0, tmpTC.length);

	// find the minimal occurrence number and valid value
	for (int i = 0; i < value; i++) {
	  tmpTC[par] = i;
	  if (count[i] < min && IPOModel.isValid(tmpTC)) {
		min = count[i];
		res = i;
	  }
	}
	return res;
  }

  /**
   * Check whether the first K parameters of two test cases can be merged.
   */
  private boolean isMatch(int[] ts1, int[] ts2, int k, int n) {
	for (int i = 0; i < k; i++) {
	  // if 2 test cases get different values in same column
	  if (ts1[i] != ts2[i] && ts1[i] > -1 && ts2[i] > -1)
		return false;
	}
	int[] tmpTC = new int[n];
	for (int i = 0; i < k; i++) {
	  if (ts1[i] != -1)
		tmpTC[i] = ts1[i];
	  else
		tmpTC[i] = ts2[i];
	}
	for (int i = k; i < n; i++)
	  tmpTC[i] = -1;
	// check validity
	return IPOModel.isValid(tmpTC);
  }

  /**
   * Merge the first K parameters of two test case (merge ts2 into ts1).
   */
  private void mergeTestCase(int[] ts1, int[] ts2, int k) {
	for (int i = 0; i < k; i++) {
	  if (ts1[i] == -1 && ts2[i] != -1)
		ts1[i] = ts2[i];
	}
  }

  /**
   * Sort parameters by non-increasing order, and return a new CTModel object.
   */
  private CTModel sortParameter(int par, final int[] val, int t_way, ArrayList<int[]> cons, ValidityChecker checker) {
	int[] tmpValue = new int[par];
	System.arraycopy(val, 0, tmpValue, 0, tmpValue.length);

	orderedParameters = new int[par];   // the index of sorted parameters
	int[] reversePar = new int[par];    // the old index of each parameter in orderedParameters

	for (int i = 0; i < par; i++) {
	  int max = tmpValue[0];
	  int maxI = 0;
	  // find the biggest value
	  for (int j = 1; j < par; j++)
		if (tmpValue[j] > max) {
		  max = tmpValue[j];
		  maxI = j;
		}
	  // the i-th biggest parameter's origin position is maxI
	  orderedParameters[i] = maxI;
	  reversePar[maxI] = i;
	  tmpValue[maxI] = -1;
	}

	// the new value array
	int[] sortValue = new int[par];
	for (int i = 0; i < par; i++)
	  sortValue[i] = val[orderedParameters[i]];

	ArrayList<int[]> sortConstraint = null;
	// transfer constraints
	if (cons != null) {
	  sortConstraint = new ArrayList<>();
	  for (int i = 0; i < cons.size(); i++) {
		int[] transferConstraint = new int[cons.get(i).length];
		// for every parameter
		for (int j = 0; j < cons.get(i).length; j++) {
		  int temp = Math.abs(cons.get(i)[j]);
		  // originPar means the origin position of this parameter
		  int originalPar = -1;
		  int sumPar = 0;
		  while (temp > sumPar) {
			originalPar++;
			sumPar += val[originalPar];
		  }
		  int baseNum = 0;
		  for (int k = 0; k < reversePar[originalPar]; k++)
			baseNum += sortValue[k];
		  // the new value of index j
		  transferConstraint[j] = -(baseNum + temp - sumPar + val[originalPar]);
		}
		sortConstraint.add(transferConstraint);
	  }
	}

	return new CTModel(par, sortValue, t_way, sortConstraint, checker);
  }


  public void generation(CTModel model, TestSuite ts) {
	// the CTModel with sorted parameters
	IPOModel = sortParameter(model.parameter, model.value, model.t_way, model.constraint, model.checker);

	int parameter = IPOModel.parameter;
	int t = IPOModel.t_way;
	int[] value = new int[parameter];
	System.arraycopy(this.IPOModel.value, 0, value, 0, parameter);
	int allCombinations = ALG.combine(parameter, t);

	// calculate the number of t-way value combinations of every t parameter
	int[] valueCombination = new int[allCombinations];
	calculateCombinationNumber(valueCombination, value, parameter, t);

	// initialize coverage matrix
	boolean[][] cover = new boolean[allCombinations][];
	for (int i = 0; i < allCombinations; i++) {
	  cover[i] = new boolean[valueCombination[i]];
	  for (int j = 0; j < cover[i].length; j++)
		cover[i][j] = false;
	}

	// the generated test suite
	ArrayList<int[]> testsuite = new ArrayList<>();

	//
	// cover the first t parameters
	//
	int[] tmpTestCase = new int[parameter];
	int testcaseNum = 0;
	for (int i = 0; i < parameter; i++) {
	  tmpTestCase[i] = -1;
	  if (i < t)
		tmpTestCase[i] = 0;
	}
	while (tmpTestCase[0] != value[0]) {
	  // if it is a valid combination
	  if (IPOModel.isValid(tmpTestCase)) {
		testsuite.add(tmpTestCase.clone());
		testcaseNum++;
	  }
	  tmpTestCase[t - 1]++;
	  for (int i = t - 1; i > 0; i--) {
		if (tmpTestCase[i] == value[i]) {
		  tmpTestCase[i - 1]++;
		  for (int j = 0; j < t - i; j++)
			tmpTestCase[i + j] = 0;
		}
	  }
	}

	// lineNumber[i] indicates the coverage matrix's row numbers of t-way combinations,
	// which (t+i)-th parameter combines with previous parameters
	int[][] lineNumber = new int[parameter - t][];
	for (int i = 0; i < parameter - t; i++)
	  lineNumber[i] = new int[ALG.combine(t + i, t - 1)];
	int[] tCombination = new int[t];
	int[] combination = new int[t - 1];

	// initialize lineNumber
	for (int i = 0; i < parameter - t; i++) {
	  for (int j = 0; j < t - 1; j++)
		combination[j] = j;
	  int count = 0;
	  tCombination[t - 1] = t + i;
	  //go through every combination, which contains the (i+t)-th parameter and parameters ahead parameter(i+t)
	  while (combination[0] != i + 2) {
		for (int j = 0; j < t - 1; j++)
		  tCombination[j] = combination[j];
		combination[t - 2]++;
		for (int j = t - 2; j > 0; j--) {
		  if (combination[j] == i + j + 2) {
			combination[j - 1]++;
			for (int k = 1; k < t - j; k++)
			  combination[j - 1 + k] = combination[j - 1] + k;
		  }
		}
		// calculate row number of these t parameters
		lineNumber[i][count] = ALG.combine2num(tCombination, parameter, t);
		count++;
	  }
	}

	// start IPO
	// currentPar indicates the next parameter for IPO horizontal extension, start from t
	int currentPar = t;
	int[] ParCombination = new int[t - 1];
	int[] parVal = new int[t];
	int[] parPos = new int[t];

	while (currentPar < parameter) {
	  int n = ALG.combine(currentPar, t - 1);
	  // PV[][] indicates the (t-1)-way combinations of the first currentPar parameters
	  // appends the (currentPar+1)-th parameter
	  PV[][] parameters = new PV[n][];
	  for (int i = 0; i < n; i++) {
		parameters[i] = new PV[t];
		for (int j = 0; j < t; j++)
		  parameters[i][j] = new PV();
	  }

	  // horizontal extension
	  for (int tcNumber = 0; tcNumber < testcaseNum; tcNumber++) {
		// initialize PV[][]
		// store (t-1)-way combinations of first currentPar parameters
		for (int i = 0; i < t - 1; i++)
		  ParCombination[i] = i;

		int line = 0;
		while (ParCombination[0] != currentPar + 2 - t) {
		  for (int i = 0; i < t - 1; i++) {
			parameters[line][i].val = testsuite.get(tcNumber)[ParCombination[i]];
			parameters[line][i].par = ParCombination[i];
		  }
		  ParCombination[t - 2]++;
		  for (int i = t - 2; i > 0; i--) {
			if (ParCombination[i] == currentPar + 2 - t + i) {
			  ParCombination[i - 1]++;
			  for (int j = 0; j < t - i - 1; j++)
				ParCombination[i + j] = ParCombination[i - 1] + j + 1;
			}
		  }
		  line++;
		}
		int maxCover = -1;
		for (int i = 0; i < n; i++)
		  parameters[i][t - 1].par = currentPar;

		// choose a value for currentPar, which covers most uncovered combinations
		for (int val = 0; val < value[currentPar]; val++) {
		  int[] temTC = new int[parameter];
		  for (int i = 0; i < parameter; i++)
			temTC[i] = testsuite.get(tcNumber)[i];
		  temTC[currentPar] = val;

		  // if current test case is valid
		  if (IPOModel.isValid(temTC)) {
			int coverNumber = 0;
			for (int i = 0; i < n; i++)
			  parameters[i][t - 1].val = val;
			// go through every combination
			for (int i = 0; i < n; i++) {
			  for (int j = 0; j < t; j++) {
				parVal[j] = parameters[i][j].val;
				parPos[j] = parameters[i][j].par;
			  }
			  int column = value2ColumnNumber(parVal, parPos, value, t);
			  int row = lineNumber[currentPar - t][i];
			  // if this combination is uncovered, coverNum + 1
			  if (!cover[row][column])
				coverNumber++;
			}
			// if current value covers more uncovered combinations
			if (coverNumber > maxCover) {
			  maxCover = coverNumber;
			  testsuite.get(tcNumber)[currentPar] = val;
			}
		  }
		}

		// update coverage matrix
		// cover all combinations in new test case
		for (int i = 0; i < n; i++) {
		  parameters[i][t - 1].val = testsuite.get(tcNumber)[currentPar];
		  for (int j = 0; j < t; j++) {
			parVal[j] = parameters[i][j].val;
			parPos[j] = parameters[i][j].par;
		  }
		  int column = value2ColumnNumber(parVal, parPos, value, t);
		  int row = lineNumber[currentPar - t][i];
		  cover[row][column] = true;
		}
	  }

	  // start vertical extension
	  // store all uncovered combinations in uncoveredTuples
	  ArrayList<int[]> uncoveredTuples = new ArrayList<>();
	  ArrayList<int[]> testcaseNew = new ArrayList<>();
	  int uncoveredNum = 0;

	  // for every combination concerns parameter currentPar
	  for (int i = 0; i < n; i++) {
		int row = lineNumber[currentPar - t][i];
		for (int j = 0; j < t; j++)
		  parPos[j] = parameters[i][j].par;
		for (int columnNum = 0; columnNum < valueCombination[row]; columnNum++) {
		  // get a uncovered combination
		  if (!cover[row][columnNum]) {
			int[] values = new int[t];
			// get parameter values by column number
			columnNumber2value(columnNum, values, parPos, value, t);
			int[] tmpTC = new int[parameter];
			for (int k = 0; k < parameter; k++)
			  tmpTC[k] = -1;
			for (int k = 0; k < t; k++)
			  tmpTC[parPos[k]] = values[k];
			// add to uncoveredTuples if it is valid
			if (IPOModel.isValid(tmpTC)) {
			  uncoveredTuples.add(tmpTC);
			  uncoveredNum++;
			}
		  }
		}
	  }

	  // try to merge tuples in uncoveredTuples
	  int newTCNum = 0;
	  if (uncoveredNum > 0) {
		// add the first tuple
		int[] tmpTC = new int[parameter];
		System.arraycopy(uncoveredTuples.get(0), 0, tmpTC, 0, parameter);
		testcaseNew.add(tmpTC);
		newTCNum = 1;
		int k = 1;
		// check whether the k-th tuple can be covered by assigning unfixed positions in uncoveredTuples
		while (k < uncoveredNum) {
		  int matchLine;
		  for (matchLine = 0; matchLine < newTCNum; matchLine++)
			if (isMatch(testcaseNew.get(matchLine), uncoveredTuples.get(k), currentPar + 1, parameter))
			  break;
		  if (matchLine < newTCNum)
			mergeTestCase(testcaseNew.get(matchLine), uncoveredTuples.get(k), currentPar + 1);
		  else {
			// add a new test case to cover this tuple
			tmpTC = new int[parameter];
			System.arraycopy(uncoveredTuples.get(k), 0, tmpTC, 0, parameter);
			testcaseNew.add(tmpTC);
			newTCNum++;
		  }
		  k++;
		}
	  }

	  // assign values for unfixed positions, and add new test cases
	  for (int i = 0; i < newTCNum; i++) {
		int[] tmpTC = new int[parameter];
		System.arraycopy(testcaseNew.get(i), 0, tmpTC, 0, parameter);
		for (int j = 0; j < currentPar + 1; j++) {
		  if (tmpTC[j] == -1) {
			// column stores values of parameter j in existing test cases
			int[] column = new int[testcaseNum + i];
			for (int k = 0; k < testcaseNum + i; k++)
			  column[k] = testsuite.get(k)[j];
			// assign value for parameter j
			tmpTC[j] = findMinimumOccurrenceValue(tmpTC, column, j, value[j]);
		  }
		}
		// add mew test case
		testsuite.add(tmpTC);
	  }
	  testcaseNum += newTCNum;
	  currentPar++;
	}

	//
	// change parameters to original order
	//
	ArrayList<int[]> originTS = new ArrayList<>();
	for (int i = 0; i < testcaseNum; i++) {
	  originTS.add(new int[parameter]);
	  for (int j = 0; j < parameter; j++)
		originTS.get(i)[orderedParameters[j]] = testsuite.get(i)[j];
	}
	for (int i = 0; i < testcaseNum; i++)
	  ts.suite.add(new TestCase(originTS.get(i)));

  }
}

