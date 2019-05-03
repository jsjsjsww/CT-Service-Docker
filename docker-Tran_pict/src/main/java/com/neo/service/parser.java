package com.neo.service;

import com.neo.domain.CTModel;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class parser {

  public static JSONObject parserPICT(StringBuilder sb) {
	String[] lines = sb.toString().split("&#");
	int parameters = 0;
	HashMap<String, Integer> par = new HashMap<>();
	ArrayList<Integer> values = new ArrayList<>();
	HashMap<String, HashMap<String, Integer>> pv = new HashMap<>();
	ArrayList<int[]> relations = new ArrayList<>();
	ArrayList<String> constraints = new ArrayList<>();
	for (String line : lines) {
	  String[] split = line.split(":");
	  if (split.length == 2) {
		parameters++;
		String parName = split[0].trim();
		if (par.containsKey(parName))  //重复的参数名
		  return getError("duplicated parameter name for " + parName);
		par.put(parName, parameters);
		HashMap<String, Integer> tmp = new HashMap<>();
		String[] vals = split[1].trim().split(",");
		values.add(vals.length);
		for (int i = 0; i < vals.length; i++) {
		  String val = vals[i].trim();
		  if (tmp.containsKey(val))   //重复的参数取值
			return getError("duplicated value name " + vals[i].trim());
		  tmp.put(val, i + 1);
		}
		pv.put(parName, tmp);
	  } else if (line.contains("@")) { //处理sub model
		String[] split1 = line.split("@");
		if (split1.length != 2)
		  return getError("something wrong in line " + line);
		int t = 0;
		try {
		  t = Integer.parseInt(split1[1].trim());
		} catch (Exception e) {
		  return getError("something wrong in line " + line + " must have an Integer for strength");
		}
		String s = split1[0].trim();
		s = s.substring(1, s.length() - 1);
		String[] vs = s.split(",");
		if (vs.length != t)
		  return getError("something wrong in line " + line);
		int[] tmp = new int[t + 1];
		tmp[tmp.length - 1] = t;
		for (int i = 0; i < vs.length; i++) {
		  String p = vs[i].trim();
		  if (!par.containsKey(p))
		    return getError("unknown parameter " + p);
		  tmp[i] = par.get(p) - 1;
		}
		relations.add(tmp);

	  } else { //to do
			// currently only handle "a => b", where "a" and "b" involves only one parameter
		String line1 = line.replace("IF","");
		line1 = line1.replace("THEN", "=>");
		line1 = line1.replaceAll("\\[", "");
		line1 = line1.replaceAll("]", "");
		line1 = line1.replaceAll("\"", "");
		String[] split1 = line1.split("=>");
		String a = split1[0].trim();
		String b = split1[1].trim();
		JSONObject json1 = getTuple(a, par, pv);
		JSONObject json2 = getTuple(b, par, pv);
		if((int)json1.get("errorCode") != 0 || (int)json2.get("errorCode") != 0)
		  return getError("some thing wrong in line " + line);
		ArrayList<Integer> list1 = new ArrayList<>();
		JSONArray array1 = (JSONArray) json1.get("list");
		List list = array1.toList();
		for (Object aList : list) list1.add((Integer) aList);
		ArrayList<Integer> list2 = new ArrayList<>();
		array1 = (JSONArray) json2.get("list1");
		list = array1.toList();
		for (Object aList : list) list2.add((Integer) aList);

		for (Integer aList1 : list1)
		  for (Integer aList2 : list2) {
			if (aList1 < aList2)
			  constraints.add("- " + aList1 + " - " + aList2);
			else
			  constraints.add("- " + aList2 + " - " + aList1);
		  }
	  }
	}
	CTModel res = new CTModel();
	res.setParameter(parameters);
	int size = values.size();
	int[] valuesArr = new int[size];
	for (int i = 0; i < size; i++)
	  valuesArr[i] = values.get(i);
	res.setValues(valuesArr);
	res.setRelation(relations);
	res.setConstraint(constraints);
	return new JSONObject(res);
  }
  private static JSONObject getTuple(String s, HashMap<String, Integer> parameters, HashMap<String, HashMap<String, Integer>> pv){
	JSONObject res = new JSONObject();
	ArrayList<Integer> list = new ArrayList<>();
	ArrayList<Integer> list1 = new ArrayList<>();
	if(s.contains("!=")){ //handle !=
	  String[] split = s.split("!=");
	  String p = split[0].trim();
	  String v = split[1].trim();
	  if(parameters.containsKey(p)){
		int sum = getSum(p, parameters, pv);
		for(Object o : pv.get(p).entrySet()){
		  Map.Entry entry = (Map.Entry) o;
		  String key = (String) entry.getKey();
		  Integer val = (Integer) entry.getValue();
		  if(!key.equals(v))
			list.add(sum + val - 1);
		  else
			list1.add(sum + val - 1);
		}
	  }
	  else{
		res.put("errorCode", 1);
		res.put("errorDes", "no such parameter for " + p);
		return res;
	  }
	}
	else if(s.contains("=")){
	  String[] split = s.split("=");
	  String p = split[0].trim();
	  String v = split[1].trim();
	  if(parameters.containsKey(p)){
		int sum = getSum(p, parameters, pv);
		for(Object o : pv.get(p).entrySet()){
		  Map.Entry entry = (Map.Entry) o;
		  String key = (String) entry.getKey();
		  Integer val = (Integer) entry.getValue();
		  if(key.equals(v))
			list.add(sum + val - 1);
		  else
			list1.add(sum + val - 1);
		}
	  }
	  else{
		res.put("errorCode", 1);
		res.put("errorDes", "no such parameter for " + p);
		return res;
	  }
	}
	else{
	  boolean not = false;
	  if(s.indexOf("!") == 0){
		not = true;
		s = s.substring(1,s.length());
	  }
	  if(parameters.containsKey(s) && pv.get(s).size() == 2 && pv.get(s).containsKey("TRUE") && pv.get(s).containsKey("FALSE")){
		int sum = getSum(s, parameters, pv);
		if(not){
		  list.add(sum + 1);
		  list1.add(sum);
		}
		else {
		  list.add(sum);
		  list1.add(sum + 1);
		}
	  }
	  else{
		res.put("errorCode", 1);
		res.put("errorDes", "parameter " + s + " is not boolean");
		return res;
	  }
	}
	res.put("errorCode", 0);
	res.put("list", list);
	res.put("list1", list1);
	return res;
  }

  private static int getSum(String p, HashMap<String, Integer> parameters, HashMap<String, HashMap<String, Integer>> pv){
	int index = parameters.get(p);
	int sum = 0;
	for (Object o : parameters.entrySet()) {
	  Map.Entry entry = (Map.Entry) o;
	  String key = (String) entry.getKey();
	  Integer val = (Integer) entry.getValue();
	  if (val < index)
		sum += pv.get(key).size();
	}
	return sum;
  }
  private static  JSONObject getError(String errorDes){
	JSONObject res = new JSONObject();
	res.put("errorDes", errorDes);
	return res;
  }
}
