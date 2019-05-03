package com.neo.service;

import org.json.JSONObject;

import java.util.ArrayList;

public class transCA {
  public static JSONObject trans(ArrayList<String[]> tests, String s){
    ArrayList<String> testStep = new ArrayList<>();
    for(int i = 0; i < tests.size(); i++){
      String[] test = tests.get(i);
	  String tmp = s;
      for(int j = 0; j < test.length; j++){

		tmp = tmp.replaceAll("#p" + (j + 1), test[j]);
	  }
	  testStep.add(tmp);
	}
	JSONObject res = new JSONObject();
    res.put("concreteCA", testStep);
    return res;
  }

  public static JSONObject getError(String descriprtion){
    JSONObject res = new JSONObject();
    res.put("errorDes", descriprtion);
    return res;
  }
}
