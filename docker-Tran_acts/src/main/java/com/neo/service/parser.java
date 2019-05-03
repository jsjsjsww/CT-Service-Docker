package com.neo.service;

import com.neo.domain.CTModel;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

public class parser {

  public static JSONObject parserACTS(StringBuilder sb) {
	String[] lines = sb.toString().split("&#");
	int phase = 0, parameters = 0;
	HashMap<String, Integer> par = new HashMap<>();
	ArrayList<Integer> values = new ArrayList<>();
	HashMap<String, HashMap<String, Integer>> pv = new HashMap<>();
	ArrayList<int[]> relations = new ArrayList<>();
	ArrayList<String> constraints = new ArrayList<>();
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
			if(split.length != 2)
			  return getError("something wrong in line " + line);
			String parName = split[0].split(":")[0];
			parName = parName.split("\\(")[0].trim();
			if (par.containsKey(parName))
			  return getError( "duplicated parameter name for " + parName);
			par.put(parName, parameters);
			HashMap<String, Integer> tmp = new HashMap<>();
			String[] vals = split[1].trim().split(",");
			values.add(vals.length);
			for (int i = 0; i < vals.length; i++) {
			  if (tmp.containsKey(vals[i].trim()))
			    return getError("duplicated value name " + vals[i].trim());
			  tmp.put(vals[i].trim(), i + 1);
			}
			pv.put(parName, tmp);
		  } else if (phase == 2) { //处理可变力度
			String[] split = line.split("\\(");
			if(split.length != 2)
			  return getError("something wrong in line " + line);
			String r = split[1].trim();
			r = r.substring(0, r.length() - 1);
			String[] pars = r.split(",");
			if(pars.length == 1)
			  return getError("something wrong in line " + line);
			int t = 0;
			try {
			  t = Integer.parseInt(pars[pars.length - 1]);
			}
			catch (Exception e){
			  return getError("something wrong in line " + line + " must have an Integer for strength");
			}
			if(pars.length != t + 1)
			  return getError("something wrong in line " + line);
			int[] tmp = new int[pars.length];
			tmp[tmp.length - 1] = t;
			for(int i = 0; i < pars.length - 1; i++) {
			  String p = pars[i].trim();
			  if(!par.containsKey(p))
			    return getError("unknown parameter " + p);
			  tmp[i] = par.get(p) - 1;
			}
			relations.add(tmp);

		  } else if (phase == 3) { // to do
			// currently only handle "a => b", where "a" and "b" involves only one parameter
			String[] split = line.split("=>");
			String a = split[0].trim();
			String b = split[1].trim();
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

		  } else if (phase == 4) { //to do

		  }
	  }
	}
	CTModel res = new CTModel();
	res.setParameter(parameters);
	int size = values.size();
	int[] valuesArr = new int[size];
	for(int i = 0; i < size; i++)
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
