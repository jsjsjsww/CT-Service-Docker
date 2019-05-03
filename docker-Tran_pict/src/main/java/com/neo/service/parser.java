package com.neo.service;

import com.neo.domain.CTModel;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class parser {

  public static JSONObject parserPICT(StringBuilder sb) {
	String[] lines = sb.toString().split("&#");
	int parameters = 0, errorCode = 0;
	String errorDes = "";
	HashMap<String, Integer> par = new HashMap<>();
	ArrayList<Integer> values = new ArrayList<>();
	HashMap<String, HashMap<String, Integer>> pv = new HashMap<>();
	ArrayList<int[]> relations = new ArrayList<>();
	for (String line : lines) {
	  String[] split = line.split(":");
	  if (split.length == 2) {
		parameters++;
		String parName = split[0].trim();
		if (par.containsKey(parName)) {
		  errorCode = 1;   //重复的参数名
		  errorDes = "duplicated parameter name for " + parName;
		  break;
		}
		par.put(parName, parameters);
		HashMap<String, Integer> tmp = new HashMap<>();
		String[] vals = split[1].trim().split(",");
		values.add(vals.length);
		for (int i = 0; i < vals.length; i++) {
		  String val = vals[i].trim();
		  if (tmp.containsKey(val)) {
			errorCode = 1;   //重复的参数名
			errorDes = "duplicated value name " + vals[i].trim();
			break;
		  }
		  tmp.put(val, i + 1);
		}
		pv.put(parName, tmp);
	  } else if (line.indexOf("@") != -1) { //处理sub model
		String[] split1 = line.split("@");
		if (split1.length != 2) {
		  errorCode = 1;
		  errorDes = "something wrong in line " + line;
		  break;
		}
		int t = 0;
		try {
		  t = Integer.parseInt(split1[1].trim());
		} catch (Exception e) {
		  errorCode = 1;
		  errorDes = "something wrong in line " + line + " must have an Integer for strength";
		  break;
		}
		String s = split1[0].trim();
		s = s.substring(1, s.length() - 1);
		String[] vs = s.split(",");
		if (vs.length != t) {
		  errorCode = 1;
		  errorDes = "something wrong in line " + line;
		  break;
		}
		int[] tmp = new int[t + 1];
		tmp[tmp.length - 1] = t;
		for (int i = 0; i < vs.length; i++) {
		  String p = vs[i].trim();
		  if (!par.containsKey(p)) {
			errorCode = 1;
			errorDes = "unknown parameter " + p;
			break;
		  }
		  tmp[i] = par.get(p) - 1;
		}
		relations.add(tmp);

	  } else { //to do
	  }
	  if (errorCode != 0)
		break;
	}
	if (errorCode != 0) {
	  JSONObject res = new JSONObject();
	  res.put("des", errorDes);
	  return res;
	}
	CTModel res = new CTModel();
	res.setParameter(parameters);
	int size = values.size();
	int[] valuesArr = new int[size];
	for (int i = 0; i < size; i++)
	  valuesArr[i] = values.get(i);
	res.setValues(valuesArr);
	res.setRelation(relations);
	return new JSONObject(res);
  }
}
