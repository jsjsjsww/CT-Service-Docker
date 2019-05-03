package com.neo.service;

import com.neo.domain.CTModel;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class parser {

  public static JSONObject parserCitLab(StringBuilder sb) {
	String[] lines = sb.toString().split("&#");
	int parameters = 0, errorCode = 0;
	String errorDes = "";
	HashMap<String, Integer> par = new HashMap<>();
	ArrayList<Integer> values = new ArrayList<>();
	HashMap<String, HashMap<String, Integer>> pv = new HashMap<>();
	ArrayList<int[]> relations = new ArrayList<>();
	ArrayList<int[]> seed = new ArrayList<>();
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
	  else if(!line.equals("end")){
		if (phase == 1) {
		  String[] split = line.split(" ");
		  if(split.length <= 1){
		    errorCode = 1;
		    errorDes = "something wrong in line " + line;
		    break;
		  }
		  String parName = split[1].trim();
		  if (par.containsKey(parName)) {
			errorCode = 1;   //重复的参数名
			errorDes = "duplicated parameter name for " + parName;
			break;
		  }
		  parameters++;
		  par.put(parName, parameters);
		  HashMap<String, Integer> tmp = new HashMap<>();
		  System.out.println(split[0]);
		  if(split[0].equals("Boolean")){
		    tmp.put("true", 1);
		    tmp.put("false", 2);
		    values.add(2);
		  }
		  else if(split[0].equals("Range")){
		    int step = 1;
		    if(line.indexOf("step") != -1) {
			  try {
			    int indexofStep = line.indexOf("step");

				String stepStr = line.substring(indexofStep + 4, line.length() - 1).trim();
				step = Integer.parseInt(stepStr);
			  } catch (Exception e) {
				errorCode = 1;
				errorDes = "something wrong in line " + line;
				break;
			  }
			}
		    int start = line.indexOf("[");
		    int end = line.indexOf("]");
		    if(start == -1 || end == -1 || start >= end){
			  errorCode = 1;
			  errorDes = "something wrong in line " + line;
			  break;
			}
			//System.out.println("a");
			String nums = line.substring(start + 1, end).trim();
			//System.out.println(nums);
		    String[] numSplit = nums.split("\\.\\.");
		    if(numSplit.length != 2){
			  errorCode = 1;
			  errorDes = "something wrong in line " + line;
			  break;
			}
			int startNum = 0, endNum = 0;
		    try{
		      startNum = Integer.parseInt(numSplit[0].trim());
		      endNum = Integer.parseInt(numSplit[1].trim());
			}catch (Exception e){
			  errorCode = 1;
			  errorDes = "something wrong in line " + line;
			  break;
			}
			int index = 1;
			for(int j = startNum; j <= endNum; j += step){
		      tmp.put(j + "",index);
		      index ++;
			}
			values.add(index -  1);
		  }
		  else if (split[0].equals("Enumerative")){
			int start = line.indexOf("{");
			int end = line.indexOf("}");
			if(start == -1 || end == -1 || start >= end){
			  errorCode = 1;
			  errorDes = "something wrong in line " + line;
			  break;
			}
			String valuesStr = line.substring(start + 1, end).trim();
			String[] valStr = valuesStr.split(" ");
			int index = 1;
			for(int i = 0; i < valStr.length; i++){
			  if(valStr[i].length() != 0) {
			    if(tmp.containsKey(valStr[i])){
				  errorCode = 1;   //重复的参数名
				  errorDes = "duplicated value name " + valStr[i];
				  break;
				}
				tmp.put(valStr[i], index);
				index ++;
			  }
			}
			values.add(index - 1);
		  }
		  pv.put(parName, tmp);
		} else if (phase == 2) { //处理约束

		} else if(phase == 3){ //seed
		  int[] seed1 = new int[parameters];
		  line = line.substring(1, line.length() - 1).trim();
		  String[] pvStr = line.split(",");
		  if(pvStr.length != parameters){
			errorCode = 1;
			errorDes = "something wrong in line " + line;
			break;
		  }
		  for(int j = 0; j < parameters; j++){
		    String pvs = pvStr[j].trim();
		    String[] tmp = pvs.split("=");
		    if(tmp.length != 2){
			  errorCode = 1;
			  errorDes = "something wrong in line " + line;
			  break;
			}
			int index;
		    String names = tmp[0].trim();
		    String vals = tmp[1].trim();
			if((index = vals.indexOf(".")) !=-1){
			  vals = vals.substring(index + 1, vals.length());
			}
			if(par.containsKey(names) && pv.get(names).containsKey(vals)){
			  seed1[par.get(names) - 1] = pv.get(names).get(vals) - 1;
			}
			else {
			  errorCode = 1;
			  errorDes = "something wrong in line " + line;
			  break;
			}
		  }
		  seed.add(seed1);
		}
		else if(phase == 4){

		}
		if (errorCode != 0)
		  break;
	  }
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
	res.setSeed(seed);
	return new JSONObject(res);
  }
}
