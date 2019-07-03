package com.neo.service;

import com.neo.domain.CTModel;
import org.json.JSONObject;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class parser {

  public static JSONObject parserPICT(StringBuilder sb) {
	String[] lines = sb.toString().split("&#");
	int parameters = 0;
	HashMap<String, Integer> par = new HashMap<>();
	ArrayList<Integer> values = new ArrayList<>();
	HashMap<String, HashMap<String, Integer>> pv = new HashMap<>();
	ArrayList<int[]> relations = new ArrayList<>();
	ArrayList<String> constraints = new ArrayList<>();
	int[] base;
	ArrayList<String> paraList = new ArrayList<>();
	HashSet<String> isNumeric = new HashSet<>();
	for (String line : lines) {
	  String[] split = line.split(":");
	  if (split.length == 2) {
		parameters++;
		String parName = split[0].trim();
		if (par.containsKey(parName))  //重复的参数名
		  return getError("duplicated parameter name for " + parName);
		par.put(parName, parameters);
		paraList.add(parName);
		HashMap<String, Integer> tmp = new HashMap<>();
		String[] vals = split[1].trim().split(",");
		values.add(vals.length);
		Boolean isNum = true;
		for (int i = 0; i < vals.length; i++) {
		  String val = vals[i].trim();
		  if (tmp.containsKey(val))   //重复的参数取值
			return getError("duplicated value name " + vals[i].trim());
		  isNum = isNum && isNumeric(val);
		  tmp.put(val, i + 1);
		}
		if(isNum)
			isNumeric.add(parName);
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

	  } else {
		String line1 = line.replace("IF","");
		line1 = line1.replace("THEN", "=>");
		line1 = line1.replaceAll("NOT", "!");
		line1 = line1.replaceAll("AND", "&&");
		line1 = line1.replaceAll("OR", "||");
		line1 = line1.replaceAll("<>", "!=");
		line1 = line1.replaceAll("\\[", "");
		line1 = line1.replaceAll("]", "");
		base = new int[par.size()];
		Arrays.fill(base, -1);
		HashSet<String> involvedPar = new HashSet<>();
		ArrayList<String> parList = new ArrayList<>();
		for(String key: paraList){
		  if(line1.contains(key) && !involvedPar.contains(key)) {
			involvedPar.add(key);
			parList.add(key);
		  }
		}
		//ArrayList<String> parList = new ArrayList<>(involvedPar);
		ArrayList<String[]> allCom = getAllCombinations(parList, pv, isNumeric);
		String exp = transImply(line1);
		System.out.println(exp);
		for (String[] anAllCom : allCom) {
		  try {
			ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
			StringBuilder con = new StringBuilder();
			for (String s : anAllCom) {
			  System.out.println(s);
			  engine.eval(s);
			  String[] split2 = s.split("=");
			  int parIndex = par.get(split2[0]);
			  if (base[parIndex - 1] == -1)
				base[parIndex - 1] = getSum(split2[0], par, pv);
			  if(split2[1].charAt(0) == '"')
			    split2[1] = split2[1].substring(1, split2[1].length() - 1);
			  con.append(" - ").append(base[parIndex - 1] + pv.get(split2[0]).get(split2[1]) - 1);
			}
			//System.out.println(exp);
			if (!(Boolean) engine.eval(exp))
			  constraints.add(con.substring(1, con.length()));
		  } catch (Exception e) {
			System.out.println(Arrays.toString(e.getStackTrace()));
			return getError("something wrong in line " + line);
		  }
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

  /**
   * 利用正则表达式判断字符串是否是数字
   * @param str
   * @return
   */
  private static Pattern pattern = Pattern.compile("-?[1-9]\\d*|0");
  private static Pattern pattern1 = Pattern.compile("-?([1-9]\\d*\\.\\d*|0\\.\\d*[1-9]\\d*|0?\\.0+|0)");
  private static boolean isNumeric(String str){
	Matcher isNum = pattern.matcher(str);
	if(isNum.matches())
	  return true;
	Matcher isFloat = pattern1.matcher(str);
	return isFloat.matches();
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


  /**
   * transfer "a => b" to "!a || b"
   * and double "=", since we need "==" instead of "="
   * @param s
   * @return
   */
  private static String transImply(String s){
	String res = s;
	res = res.replaceAll(" ", "");
	StringBuilder sb = new StringBuilder(res);
	int index = sb.indexOf("=>");
	while(index != -1){
	  //int last = index;
	  sb.insert(0, "!(");
	  sb.insert(index + 2, ")||(");
	  sb.insert(sb.length(), ")");
	  index += 6;
	  sb.delete(index, index + 2);
	  index = sb.indexOf("=>");
	}
	index = sb.indexOf("=");
	while(index != -1){
	  char pre = sb.charAt(index - 1);
	  if(pre != '!' && pre != '>' && pre != '<'){
		sb.insert(index, '=');
		index ++;
	  }
	  index ++;
	  index = sb.indexOf("=", index);
	}
	return sb.toString();
  }

  private static  JSONObject getError(String errorDes){
	JSONObject res = new JSONObject();
	res.put("errorDes", errorDes);
	return res;
  }
  private static boolean cons = true;

  /**
   * 笛卡尔积
   * @param pars
   * @param pv
   * @return
   */
  private static ArrayList<String[]> getAllCombinations(ArrayList<String> pars, HashMap<String, HashMap<String, Integer>> pv, HashSet<String> isNum){
	ArrayList<String[]> res = new ArrayList<>();
	int[] counter = new int[pars.size()];
	Arrays.fill(counter, 0);
	cons = true;
	while(cons){
	  String[] tmp = new String[pars.size()];
	  for(int i = 0; i < pars.size(); i++){
		String name = pars.get(i);
		String value = "";
		for (Map.Entry<String, Integer> entry : pv.get(name).entrySet()) {
		  if(entry.getValue() == counter[i] + 1){
			value = entry.getKey();
			break;
		  }
		}
		if(isNum.contains(name))
			tmp[i] = name + "=" + value;
		else
		  tmp[i] = name +"=\"" + value +"\"";
	  }
	  handle(counter, counter.length - 1, pars, pv);
	  res.add(tmp);
	}
	return res;
  }

  /**
   * 递归，用于笛卡儿积计算
   * @param counter
   * @param counterIndex
   * @param pars
   * @param pv
   */
  private static void handle(int[] counter, int counterIndex, ArrayList<String> pars, HashMap<String, HashMap<String, Integer>> pv) {
	if(counterIndex < 0)
	  return;
	counter[counterIndex]++;
	if (counter[counterIndex] >= pv.get(pars.get(counterIndex)).size()) {
	  counter[counterIndex] = 0;
	  counterIndex--;
	  if(counterIndex < 0) {
		cons = false;
	  }
	  handle(counter, counterIndex, pars, pv);
	}
  }

  public static void main(String[] args){
    String s = "-0.0454";
    System.out.println(isNumeric(s));
  }
}
