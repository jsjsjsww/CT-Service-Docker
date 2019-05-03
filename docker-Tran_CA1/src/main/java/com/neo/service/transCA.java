package com.neo.service;

import org.json.JSONObject;

import java.util.ArrayList;

public class transCA {
  public static JSONObject trans(ArrayList<int[]> tests, ArrayList<String[]> values){
    ArrayList<String[]> concreteCA = new ArrayList<>();
   // ArrayList<int[]> tests = ts.getTestsuite();
    for(int i = 0; i < tests.size(); i++){
      int[] test = tests.get(i);
      if(test.length != values.size())
        return getError("the " + i + "th testcase does not match values matrix size");
      String[] concreteTest = new String[test.length];
      for(int j = 0; j < test.length; j++){
        if(test[j] >= values.get(j).length)
          return getError("values for " + j + "th parameter may be wrong");
		concreteTest[j] = values.get(j)[test[j]];
	  }
	  concreteCA.add(concreteTest);
	}
	JSONObject res = new JSONObject();
    res.put("concreteCA", concreteCA);
    return res;
  }

  private static JSONObject getError(String descriprtion){
    JSONObject res = new JSONObject();
    res.put("errorDes", descriprtion);
    return res;
  }
}
