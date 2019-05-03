package com.neo.service;

import com.neo.domain.CTModel;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class parser {

  public static JSONObject parserACTS(StringBuilder sb) {
	String[] lines = sb.toString().split("&#");
	int phase = 0, parameters = 0, errorCode = 0;
	String errorDes = "";
	HashMap<String, Integer> par = new HashMap<>();
	ArrayList<Integer> values = new ArrayList<>();
	HashMap<String, HashMap<String, Integer>> pv = new HashMap<>();
	ArrayList<int[]> relations = new ArrayList<>();
	for (String line : lines) {
	  switch (line) {
		case "[System]":
		  break;
		case "[Parameter]":
		  phase = 1;
		  break;
		case "[Relation]":
		  phase = 2;
		  break;
		case "[Constraint]":
		  phase = 3;
		  break;
		case "[Test Set]":
		  phase = 4;
		  break;
		default:
		  if (phase == 1) { //处理参数列表
			parameters++;
			String[] split = line.split(":");
			if(split.length != 2) {
			  errorCode = 1;
			  errorDes = "something wrong in line " + line;
			  break;
			}
			String parName = split[0].split(":")[0];
			parName = parName.split("\\(")[0].trim();
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
			  if (tmp.containsKey(vals[i].trim())) {
				errorCode = 1;   //重复的参数名
				errorDes = "duplicated value name " + vals[i].trim();
				break;
			  }
			  tmp.put(vals[i].trim(), i + 1);
			}
			pv.put(parName, tmp);
		  } else if (phase == 2) { //处理可变力度
			String[] split = line.split("\\(");
			if(split.length != 2) {
			  errorCode = 1;
			  errorDes = "something wrong in line " + line;
			  break;
			}
			String r = split[1].trim();
			r = r.substring(0, r.length() - 1);
			String[] pars = r.split(",");
			if(pars.length == 1) {
			  errorCode = 1;
			  errorDes = "something wrong in line " + line;
			  break;
			}
			int t = 0;
			try {
			  t = Integer.parseInt(pars[pars.length - 1]);
			}
			catch (Exception e){
			  errorCode = 1;
			  errorDes = "something wrong in line " + line + " must have an Integer for strength";
			  break;
			}
			if(pars.length != t + 1) {
			  errorCode = 1;
			  errorDes = "something wrong in line " + line;
			  break;
			}
			int[] tmp = new int[pars.length];
			tmp[tmp.length - 1] = t;
			for(int i = 0; i < pars.length - 1; i++) {
			  String p = pars[i].trim();
			  if(!par.containsKey(p)) {
				errorCode = 1;
				errorDes = "unknown parameter " + p;
				break;
			  }
			  tmp[i] = par.get(p) - 1;
			}
			relations.add(tmp);

		  } else if (phase == 3) { // to do

		  } else if (phase == 4) { //to do

		  }
		if(errorCode != 0)
		  break;
	  }
	}
	if(errorCode != 0){
	  JSONObject res = new JSONObject();
	  res.put("des", errorDes);
	  return res;
	}
	CTModel res = new CTModel();
	res.setParameter(parameters);
	int size = values.size();
	int[] valuesArr = new int[size];
	for(int i = 0; i < size; i++)
	  valuesArr[i] = values.get(i);
	res.setValues(valuesArr);
	res.setRelation(relations);
	return new JSONObject(res);
  }
}
