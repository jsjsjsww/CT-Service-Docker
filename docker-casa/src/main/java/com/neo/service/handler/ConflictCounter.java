package com.neo.service.handler;


import com.neo.service.combinatorial.CTModel;

import java.util.ArrayList;
import java.util.Vector;

/**
 * ConflictCounter is used to calculate the number of constraint violations
 * of a given test case. Note that the test case will be checked against the
 * list of minimal forbidden tuple (MFT).
 */
public class ConflictCounter {

  private int[][] relation;
  private Vector<Constraint> hardConstraint;

  public ConflictCounter(CTModel model) {
	relation = model.relation;
	hardConstraint = new Vector<>();
	MFTVerifier tmp = (MFTVerifier) model.checker;
	for (Constraint x : tmp.MFT) {
	  hardConstraint.add(new Constraint(x.disjunction));
	}
	//System.out.println("conflict counter size = " + hardConstraint.size());
  }

  public int getConflicitSize() {
	return hardConstraint.size();
  }

  public int violateConstraintNum(final int[] test) {
	if (hardConstraint.size() == 0)
	  return 0;

	// convert the test case to constraint's form
	// for example: value = [2, 2, 2]
	// test case = [0, 0, -1] will be converted to [1, 3, -1]
	int[] TC = new int[test.length];
	int baseNum = 0;
	for (int i = 0; i < test.length; i++) {
	  if (test[i] != -1)
		TC[i] = test[i] + baseNum + 1;
	  else
		TC[i] = -1;
	  baseNum += relation[i].length;
	}

	// calculate the number of constraints that TC contains
	int violateCount = 0;
	for (Constraint cons : hardConstraint) {
	  boolean matched = true;
	  int k = 0;
	  int j = 0;
	  while (j < cons.disjunction.length) {
		while (k < TC.length && (TC[k] == -1 || Math.abs(cons.disjunction[j]) > TC[k])) {
		  k++;
		  if (k == TC.length)
			matched = false;
		}
		if (!matched)
		  break;
		if (Math.abs(cons.disjunction[j]) == TC[k]) {
		  k++;
		  j++;
		} else {
		  matched = false;
		  break;
		}
	  }
	  if (matched)
		violateCount++;
	}
	return violateCount;
  }

  public int violateConstraintNum(ArrayList<int[]> A) {
	int violate = 0;
	for (int[] aA : A)
	  violate += violateConstraintNum(aA);
	return violate;
  }

}
