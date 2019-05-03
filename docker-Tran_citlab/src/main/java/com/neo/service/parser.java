package com.neo.service;

import com.neo.domain.CTModel;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class parser {

  public static JSONObject parserCitLab(StringBuilder sb) {
	String[] lines = sb.toString().split("&#");
	int parameters = 0;
	HashMap<String, Integer> par = new HashMap<>();
	ArrayList<Integer> values = new ArrayList<>();
	HashMap<String, HashMap<String, Integer>> pv = new HashMap<>();
	ArrayList<int[]> relations = new ArrayList<>();
	ArrayList<int[]> seed = new ArrayList<>();
	ArrayList<String> constraints = new ArrayList<>();
	int phase = 0;
	for (String line : lines) {
	  line = line.trim();
	  if (line.equals("Parameters:"))
		phase = 1;
	  else if (line.equals("Constraints:"))
		phase = 2;
	  else if (line.equals("Seeds:"))
		phase = 3;
	  else if (line.equals("TestGoals:"))
		phase = 4;
	  else if (!line.equals("end")) {
		if (phase == 1) {
		  String[] split = line.split(" ");
		  if (split.length <= 1)
		    return getError("something wrong in line " + line);
		  String parName = split[1].trim();
		  if (par.containsKey(parName))  //重复的参数名
			return getError("duplicated parameter name for " + parName);
		  parameters++;
		  if(split[0].equals("Boolean"))
		    parName = parName.substring(0, parName.length() - 1);
		  par.put(parName, parameters);
		  HashMap<String, Integer> tmp = new HashMap<>();
		  switch (split[0]) {
			case "Boolean":
			  tmp.put("TRUE", 1);
			  tmp.put("FALSE", 2);
			  values.add(2);
			  break;
			case "Range": {
			  int step = 1;
			  if (line.contains("step")) {
				try {
				  int indexofStep = line.indexOf("step");

				  String stepStr = line.substring(indexofStep + 4, line.length() - 1).trim();
				  step = Integer.parseInt(stepStr);
				} catch (Exception e) {
					return getError("something wrong in line " + line);
				}
			  }
			  int start = line.indexOf("[");
			  int end = line.indexOf("]");
			  if (start == -1 || end == -1 || start >= end)
			    return getError("something wrong in line " + line);
			  String nums = line.substring(start + 1, end).trim();
			  String[] numSplit = nums.split("\\.\\.");
			  if (numSplit.length != 2)
			    return getError("something wrong in line " + line);
			  int startNum = 0, endNum = 0;
			  try {
				startNum = Integer.parseInt(numSplit[0].trim());
				endNum = Integer.parseInt(numSplit[1].trim());
			  } catch (Exception e) {
				return getError("something wrong in line " + line);
			  }
			  int index = 1;
			  for (int j = startNum; j <= endNum; j += step) {
				tmp.put(j + "", index);
				index++;
			  }
			  values.add(index - 1);
			  break;
			}
			case "Enumerative": {
			  int start = line.indexOf("{");
			  int end = line.indexOf("}");
			  if (start == -1 || end == -1 || start >= end)
			    return getError("something wrong in line " + line);
			  String valuesStr = line.substring(start + 1, end).trim();
			  String[] valStr = valuesStr.split(" ");
			  int index = 1;
			  for (int i = 0; i < valStr.length; i++) {
				if (valStr[i].length() != 0) {
				  if (tmp.containsKey(valStr[i]))  //重复的参数名
					return getError("duplicated value name " + valStr[i]);
				  tmp.put(valStr[i], index);
				  index++;
				}
			  }
			  values.add(index - 1);
			  break;
			}
		  }
		  pv.put(parName, tmp);
		} else if (phase == 2) { //处理约束
		  // currently only handle "a => b", where "a" and "b" involves only one parameter
		  line = line.substring(1, line.length() - 1).trim();
		  String[] split = line.split("=>");
		  String a = split[0].trim();
		  String b = split[1].trim();
		  JSONObject json1 = getTuple(a, par, pv);
		  JSONObject json2 = getTuple(b, par, pv);
		  if ((int) json1.get("errorCode") != 0 || (int) json2.get("errorCode") != 0)
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


		} else if (phase == 3) { //seed
		  int[] seed1 = new int[parameters];
		  line = line.substring(1, line.length() - 1).trim();
		  String[] pvStr = line.split(",");
		  if (pvStr.length != parameters)
		    return getError("something wrong in line " + line);
		  for (int j = 0; j < parameters; j++) {
			String pvs = pvStr[j].trim();
			String[] tmp = pvs.split("=");
			if (tmp.length != 2)
			  return getError("something wrong in line " + line);
			int index;
			String names = tmp[0].trim();
			String vals = tmp[1].trim();
			if ((index = vals.indexOf(".")) != -1) {
			  vals = vals.substring(index + 1, vals.length());
			}
			if (par.containsKey(names) && pv.get(names).containsKey(vals)) {
			  seed1[par.get(names) - 1] = pv.get(names).get(vals) - 1;
			} else
			  return getError("something wrong in line " + line);
		  }
		  seed.add(seed1);
		} else if (phase == 4) {

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
	res.setSeed(seed);
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
