package com.neo.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.neo.domain.*;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;


public class CTA {
  private HashMap<TestCase, Integer> executed;
  private List<Tuple> bugs;
  private String tree;

  public String getTree() {
	return tree;
  }

  public HashMap<TestCase, Integer> getExecuted() {
	return executed;
  }

  public List<Tuple> getBugs() {
	return bugs;
  }

  public CTA() {
	executed = new HashMap<>();
  }

  public void process(int[] parameters, String[] classes, TestSuite suite,
					  String[] state) throws Exception {
	FastVector attr = this.constructAttributes(parameters, classes);
	Instances data = this.constructData(suite, attr, state);
	// System.out.println(data.numInstances());
	tree = this.constructClassifier(data);
	bugs = this.getBugs(tree, parameters.length);
  }

  private FastVector constructAttributes(int[] parameters, String[] classes) {

	FastVector attributes = new FastVector(parameters.length + 1);

	for (int i = 0; i < parameters.length; i++) {
	  FastVector labels = new FastVector(parameters[i]);
	  for (int j = 0; j < parameters[i]; j++)
		labels.addElement("" + j);
	  String c = "c_" + i;
	  Attribute nominal = new Attribute(c, labels);
	  attributes.addElement(nominal);
	}

	final FastVector classValues = new FastVector(classes.length);
	for (String str : classes)
	  classValues.addElement(str);

	attributes.addElement(new Attribute("Class", classValues));

	return attributes;
	// Instances data = new Instances();
  }

  private Instances constructData(TestSuite suite, FastVector attributes,
								  String[] state) {
	Instances data = new Instances("Data1", attributes,
			suite.getTestCaseNum());
	// set the index of the col of the class in the data
	data.setClassIndex(data.numAttributes() - 1);

	for (int i = 0; i < suite.getTestCaseNum(); i++) {
	  TestCase testCase = suite.getAt(i);
	  Instance ins = new Instance(testCase.getLength() + 1);
	  for (int j = 0; j < testCase.getLength(); j++) {
		ins.setValue((Attribute) attributes.elementAt(j),
				testCase.getAt(j));
	  }
	  ins.setValue(
			  (Attribute) attributes.elementAt(testCase.getLength()),
			  state[i]);
	  // testCase.testDescription() == TestCase.PASSED ? "pass": "fail");
	  data.add(ins);
	}
	return data;
  }

  private String constructClassifier(Instances data) throws Exception {
	J48 classifier = new J48();
	String[] options = new String[3];
	options[0] = "-U";
	options[1] = "-M";
	options[2] = "1";
	classifier.setOptions(options);
	classifier.setConfidenceFactor((float) 0.25);
	classifier.buildClassifier(data);
	// System.out.println(classifier.toString());
	return classifier.toString();
  }



  private List<Tuple> getBugs(String tree, int length) {
	List<Tuple> bugs = new ArrayList<>();
	String[] strs = tree.split("\n");
	List<int[]> part = new ArrayList<>();
	for (String str : strs) {
	  if (str.contains("="))
		if (!str.contains(":")) { // not leaf
		  int dep = depth(str);
		  String[] va = str.split(" = ");
		  int[] com = new int[2];
		  com[0] = findNum(va[0]);
		  com[1] = findNum(va[1]);
		  part = part.subList(0, dep - 1);
		  part.add(com);
		} else {// leaf
		  if (!str.contains("pass")) {// fail
			int dep = depth(str);
			List<int[]> tu = new ArrayList<>(part.subList(0, dep - 1));
			String[] va = str.split(" = ");
			int[] com = new int[2];
			com[0] = findNum(va[0]);
			String[] vap = va[1].split(":");
			com[1] = findNum(vap[0]);
			tu.add(com);
			bugs.add(this.getBug(tu, length));
		  }
		}
	}
	return bugs;
  }


  private Tuple getBug(List<int[]> part, int length) {
	TestCase testCase = new TestCaseImplement(length);
	for (int i = 0; i < testCase.getLength(); i++)
	  testCase.set(i, 0);

	int[] index = new int[part.size()];

	int k = 0;
	for (int[] com : part) {
	  testCase.set(com[0], com[1]);
	  index[k] = com[0];
	  k++;
	}

	Tuple tuple = new Tuple(part.size(), testCase);

	Arrays.sort(index);
	for (int i = 0; i < part.size(); i++)
	  tuple.set(i, index[i]);

	return tuple;
  }

  private int depth(String str) {
	int depth = 1;
	for (int i = 0; i < str.length(); i++)
	  if (str.charAt(i) == '|')
		depth++;
	return depth;
  }

  private int findNum(String str) {
	boolean find = false;
	int num = 0;
	int dig = 1;
	for (int i = str.length() - 1; i >= 0; i--) {
	  if (str.charAt(i) >= '0' && str.charAt(i) <= '9') {
		find = true;
		num += (str.charAt(i) - '0') * dig;
		dig *= 10;
	  } else {
		if (find)
		  break;
	  }
	}
	if (!find)
	  return -1;
	return num;
  }

  // <test Mutilple>


  // </test Multiple>

  private void showBugs() {

	for (Tuple tuple : this.bugs) {
	  System.out.println(tuple.toString());
	}
  }

  /**
   * for test
   * @param args
   * @throws Exception
   */
  public static void main(String[] args) throws Exception {
	CTA cta = new CTA();
	int[] param = {3, 3, 3};
	String[] classes = {"pass", "err"};

	int[][] suites = {{0, 0, 0}, {0, 0, 1}, {0, 0, 2}, {0, 1, 0},
			{0, 1, 1}, {0, 1, 2}, {0, 2, 0}, {0, 2, 1},
			{0, 2, 2}, {1, 0, 0}, {1, 0, 1}, {1, 0, 2},
			{1, 1, 0}, {1, 1, 1}, {1, 1, 2}, {1, 2, 0},
			{1, 2, 1}, {1, 2, 2}, {2, 0, 0}, {2, 0, 1},
			{2, 0, 2}, {2, 1, 0}, {2, 1, 1}, {2, 1, 2},
			{2, 2, 0}, {2, 2, 1}, {2, 2, 2}};

	TestSuite suite = new TestSuiteImplement();

	for (int[] test : suites) {
	  TestCaseImplement testCase = new TestCaseImplement(test.length);
	  testCase.setTestCase(test);
	  suite.addTest(testCase);
	}

	String[] state = {"pass", "pass", "err", "pass", "pass", "pass",
			"pass", "pass", "pass", "err", "err", "err", "err", "err",
			"err", "err", "err", "err", "err", "err", "err", "err",
			"err", "err", "err", "err", "err"};

	cta.process(param, classes, suite, state);
	cta.showBugs();
  }
}
