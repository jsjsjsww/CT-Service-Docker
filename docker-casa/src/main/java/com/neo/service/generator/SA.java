package com.neo.service.generator;

import com.neo.service.combinatorial.*;
import com.neo.service.handler.MFTVerifier;
import com.neo.service.handler.ConflictCounter;

import java.util.ArrayList;
import java.util.Random;

public class SA implements CAGenerator {

  private boolean isTolerate;

  // calculate how many constraints a test case or a test suite violates
  private ConflictCounter conflictCounter;

  private Random random;
  private double initialTemperature;
  private int maxRound;

  // coverage matrix, indicating how many times a combination is covered
  private int[][] cover;
  private long uncoverNum;

  public SA(boolean isTolerate) {
	this(0.2, 200000, isTolerate);
  }

  public SA(double initialTemperature, int maxRound, boolean isTolerate) {
	random = new Random();
	this.initialTemperature = initialTemperature;
	this.maxRound = maxRound;
	this.isTolerate = isTolerate;
  }

  public void generation(CTModel model, TestSuite ts) {
	// if use the Tolerate technique
	if (isTolerate)
	  conflictCounter = new ConflictCounter(model);

	model.initialization();
	cover = new int[model.getCombination().getMatrix().length][];

	// calculate lower and upper bound
	int[] upperAndLower = new int[2];
	if (model.t_way == 3)
	  getUpperAndLower3Way(model, upperAndLower);
	else if (model.t_way == 2)
	  getUpperAndLower2Way(model, upperAndLower);
	int upper = upperAndLower[1];
	int lower = upperAndLower[0];
	//System.out.println("lower bound = " + lower + ", upper bound = " + upper);

	// the outer search
	ArrayList<int[]> A1 = outerSearch(model, lower, upper);

	// if no solution is found, double upper bound and try again
	if (A1.size() == 0) {
	  //while (A1.size() == 0) {
	  System.out.println("increase upper bound, new lower = " + lower + " new upper = " + upper);
	  upper *= 2;
	  A1 = outerSearch(model, lower, upper);
	}
	// while lower bound is met
	while (A1.size() == lower) {
	  upper = lower - 1;
	  lower = lower > 5 ? lower - 5 : lower / 2;
	  System.out.println("achieve lower bound, new lower = " + lower + " new upper = " + upper);
	  ArrayList<int[]> A2 = outerSearch(model, lower, upper);
	  if (A2.size() > 0)
		A1 = A2;
	}

	// update test suite
	for (int[] tc : A1) {
	  ts.suite.add(new TestCase(tc));
	}
  }

  /**
   * Calculate lower and upper bounds for covering strength 2.
   */
  private void getUpperAndLower2Way(CTModel model, int[] res) {
	int max = Integer.MIN_VALUE, second = Integer.MIN_VALUE;
	int[] values = model.value;
	if (values[0] > values[1]) {
	  max = values[0];
	  second = values[1];
	} else {
	  max = values[1];
	  second = values[0];
	}
	// find the biggest and second biggest numbers
	for (int i = 2; i < values.length; i++) {
	  if (values[i] > max) {
		second = max;
		max = values[i];
	  } else if (values[i] > second)
		second = values[i];
	}
	res[0] = max * second;
	res[1] = 5 * max * max;
  }

  /**
   * Calculate lower and upper bounds for covering strength 3.
   */
  private void getUpperAndLower3Way(CTModel model, int[] res) {
	int max = Integer.MIN_VALUE, second = Integer.MIN_VALUE, third = Integer.MIN_VALUE;
	int[] values = model.value;

	// find the biggest and second biggest and third biggest numbers
	for (int i = 0; i < values.length; i++) {
	  if (values[i] > max) {
		third = second;
		second = max;
		max = values[i];

	  } else if (values[i] > second) {
		third = second;
		second = values[i];
	  } else if (values[i] > third)
		third = values[i];
	}
	res[0] = max * second * third;
	res[1] = 5 * max * max * max;
  }

  /**
   * The outer search of SA.
   */
  private ArrayList<int[]> outerSearch(CTModel model, int lower, int upper) {
	ArrayList<int[]> A = new ArrayList<>();
	int N = (lower + 2 * upper) / 3;        // the initial test suite size

	while (upper >= lower) {
	  // System.out.println("N = " + N);
	  ArrayList<int[]> A1 = innerSearch(model, N);
	  //if (model.fitnessValue(A1) == 0) {
	  if (A1.size() != 0) {
		// if a solution is found, update upper to N - 1
		A = A1;
		upper = N - 1;
	  } else {
		// else update lower to N + 1
		lower = N + 1;
	  }
	  N = (lower + 2 * upper) / 3;
	}
	return A;
  }

  /**
   * The inner search of SA.
   */
  private ArrayList<int[]> innerSearch(CTModel model, int N) {
	initCover(model);

	// get a random initial test suite
	ArrayList<int[]> A = new ArrayList<>();
	for (int i = 0; i < N; i++) {
	  int[] test = sample(model);
	  while (!isTolerate && !model.isValid(test))
		test = sample(model);
	  A.add(test);
	}

	// initialize coverage matrix
	updateTestSuiteCoverage(model, A);
	int violateCons = isTolerate ? conflictCounter.violateConstraintNum(A) : 0;

	double temperature = initialTemperature;
	int round = 0;
	while (round < maxRound) {
	  // get a random position and a random value in testsuite
	  int row = random.nextInt(N);
	  int column = random.nextInt(model.parameter);
	  int symbol = random.nextInt(model.value[column]);
	  // the modified row
	  int[] tmpTC = A.get(row).clone();
	  tmpTC[column] = symbol;

	  // if there is no change, or the change violates constraint and the constraint handling
	  // method is not Tolerate, discard this move
	  if (symbol == A.get(row)[column] || (!isTolerate && !model.isValid(tmpTC)))
		continue;

	  // A1 is the new test suite, which changes a value in test suite A
	  ArrayList<int[]> A1 = new ArrayList<>();
	  A.forEach(x -> A1.add(x.clone()));
	  A1.get(row)[column] = symbol;

	  // clone the coverage matrix
	  // in case A1 is worse then A, roll back coverage matrix
	  int[][] coverClone = new int[cover.length][];
	  for (int i = 0; i < cover.length; i++) {
		coverClone[i] = cover[i].clone();
	  }

	  // number of coverage loss if we drop the row-th test case of A
	  long coverNumA = updateTestCaseCoverage(model, A.get(row), 0);
	  // number of coverage gain if we add the row-th test case of A1 (after dropping the row-th test case of A)
	  long coverNumA1 = updateTestCaseCoverage(model, A1.get(row), 1);
	  int violateConsNumA1 = 0, violateConsNumA = 0;
	  double delta;
	  if (!isTolerate)
		delta = coverNumA1 - coverNumA;
	  else {
		// how many constraints does the row-th test case of A violates?
		violateConsNumA = conflictCounter.violateConstraintNum(A.get(row));
		// how many constraints does the row-th test case of A1 violates?
		violateConsNumA1 = conflictCounter.violateConstraintNum(A1.get(row));
		delta = coverNumA1 - coverNumA - 4.0 * (violateConsNumA1 - violateConsNumA);
	  }
	  // if A1 is better then A
	  if (delta >= 0) {
		A = A1;
		uncoverNum = uncoverNum - (coverNumA1 - coverNumA);
		violateCons = violateCons + (violateConsNumA1 - violateConsNumA);
	  } else {
		// with a possibility pro, accept solution A1
		double pro = Math.pow(Math.E, delta / temperature);
		if (random.nextDouble() < pro) {
		  A = A1;
		  uncoverNum = uncoverNum - (coverNumA1 - coverNumA);
		  violateCons = violateCons + (violateConsNumA1 - violateConsNumA);
		} else {
		  // if we drop this move, roll back coverage matrix
		  cover = coverClone;
		}
	  }

	  // if get a CA
	  if (uncoverNum == 0 && violateCons == 0) {
		//System.out.println("Get a CA at round " + round);
		return A;
	  }

	  round++;
	  temperature = cool(temperature, round);
	}

	//System.out.println("Inner end. Size = " + A.size() + " Uncover = " + uncoverNum + " VoilateCon = " + violateCons);
	return new ArrayList<>();
  }

  /**
   * update coverage for a test suite
   */
  private void updateTestSuiteCoverage(CTModel model, ArrayList<int[]> TS) {
	long covered = 0;
	for (int[] T : TS)
	  covered += updateTestCaseCoverage(model, T, 1);
	uncoverNum = model.getCombAll() - covered;
  }

  /**
   * @param model
   * @param test
   * @param flag flag = 0 indicates the removal of a test case (as if it is removed)
   *             flag = 1 indicates the addition of a test case (as if it is added)
   * @return the number of changes on the combination coverage
   */
  private int updateTestCaseCoverage(CTModel model, int[] test, int flag) {
	int change = 0;
	int i = 0;
	for (int[] tmp : model.allPc) {
	  int col = 0;
	  int sd = 1;
	  for (int j = model.t_way - 1; j >= 0; j--) {
		if (test[tmp[j]] != -1)
		  col += sd * test[tmp[j]];
		sd *= model.value[tmp[j]];
	  }
	  if (cover[i][col] != -1) {
		if (flag == 0) {
		  cover[i][col]--;
		  if (cover[i][col] == 0)   // the combination is now uncovered in the test suite
			change++;
		} else if (flag == 1) {
		  if (cover[i][col] == 0)  // the combination is now covered in the test suite
			change++;
		  cover[i][col]++;
		} else {
		  System.err.println("Flag error in SA.java!");
		}
	  }
	  i += 1 ;
	}
	return change;
  }

  /**
   * initialise cover[][]: -1 for invalid combinations, 0 for valid combinations
   */
  private void initCover(CTModel model) {
	for (int i = 0; i < model.getCombination().getMatrix().length; i++) {
	  cover[i] = new int[model.getCombination().getMatrix()[i].length];
	  for (int j = 0; j < model.getCombination().getMatrix()[i].length; j++)
		if (model.getCombination().getMatrix()[i][j])
		  cover[i][j] = -1;
		else
		  cover[i][j] = 0;
	}
  }

  /**
   * get a random test case
   */
  private int[] sample(CTModel model) {
	int[] tc = new int[model.parameter];
	for (int i = 0; i < model.parameter; i++)
	  tc[i] = random.nextInt(model.value[i]);
	return tc;
  }

  /**
   * cool function: * 0.999999 every 10 iteration
   */
  private double cool(double temperature, int round) {
	if (round % 10 != 0)
	  return temperature;
	return temperature * 0.999999;
  }

  public static void main(String[] args) {
	int parameter = 4;
	int[] value = {2, 3, 4, 2};
	ArrayList<int[]> constraint = new ArrayList<>();
	constraint.add(new int[]{-2, -5});
	constraint.add((new int[]{-1, -6}));
	CTModel model = new CTModel(parameter, value, 2, constraint, new MFTVerifier());
	SA gen = new SA(0.2, 100000, true);
	TestSuite ts = new TestSuite();
	gen.generation(model, ts);
	for (int i = 0; i < ts.suite.size(); i++) {
	  for (int j = 0; j < ts.suite.get(i).test.length; j++)
		System.out.print(ts.suite.get(i).test[j] + " ");
	  System.out.println();
	}
  }

}