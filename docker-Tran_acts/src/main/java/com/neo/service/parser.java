package com.neo.service;

import com.neo.domain.CTModel;;
import org.json.JSONObject;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import java.util.*;


public class parser {

  public static JSONObject parserACTS(StringBuilder sb) {
	String[] lines = sb.toString().split("&#");
	int phase = 0, parameters = 0;
	HashMap<String, Integer> par = new HashMap<>();
	ArrayList<String> paraList = new ArrayList<>();
	ArrayList<Integer> values = new ArrayList<>();
	HashMap<String, HashMap<String, Integer>> pv = new HashMap<>();
	ArrayList<int[]> relations = new ArrayList<>();
	ArrayList<String> constraints = new ArrayList<>();
	HashSet<String> isNumeric = new HashSet<>();
	int[] base;
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
			String parName = split[0];
			parName = parName.split("\\(")[0].trim();
			if (par.containsKey(parName))
			  return getError( "duplicated parameter name for " + parName);
			par.put(parName, parameters);
			paraList.add(parName);
			HashMap<String, Integer> tmp = new HashMap<>();
			String[] vals = split[1].trim().split(",");
			values.add(vals.length);
			for (int i = 0; i < vals.length; i++) {
			  String valName = vals[i].trim();
			  if (tmp.containsKey(valName))
			    return getError("duplicated value name " + valName);
			  if(valName.equals("TRUE"))
			    valName = "true";
			  else if(valName.equals("FALSE"))
			    valName = "false";
			  tmp.put(valName, i + 1);
			}
			if(split[0].contains("(enum)"))
			  isNumeric.add(parName);
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
			int t;
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

		  } else if (phase == 3) {
		    // 使用脚本语言来判断字符串表达式的真值
			// 缺点是效率可能比较低，但是尝试使用parser库时发现已有库存在一些不足，例如不支持字符串等
			// 如果有更合适的parser库，可以替换掉脚本判断真值
			base = new int[par.size()];
			Arrays.fill(base, -1);
			HashSet<String> involvedPar = new HashSet<>();
			ArrayList<String> parList = new ArrayList<>();
			for(String key: paraList){
			  if(line.contains(key) && !involvedPar.contains(key)) {
				involvedPar.add(key);
				parList.add(key);
			  }
			}
			//ArrayList<String> parList = new ArrayList<>(involvedPar);
			ArrayList<String[]> allCom = getAllCombinations(parList, pv, isNumeric);
			String exp = transImply(line);
			System.out.println(exp);
			for (String[] anAllCom : allCom) {
			  try {
				ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
				StringBuilder con = new StringBuilder();
				for (String s : anAllCom) {
				  engine.eval(s);
				  String[] split = s.split("=");
				  int parIndex = par.get(split[0]);
				  if (base[parIndex - 1] == -1)
					base[parIndex - 1] = getSum(split[0], par, pv);
				  if(split[1].charAt(0) == '"')
				    split[1] = split[1].substring(1, split[1].length() - 1);
				  con.append(" - ").append(base[parIndex - 1] + pv.get(split[0]).get(split[1]) - 1);
				}
				if (!(Boolean) engine.eval(exp))
				  constraints.add(con.substring(1, con.length()));
			  } catch (Exception e) {
				System.out.println(e.getMessage());
				return getError("something wrong in line " + line);
			  }
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

  /**
   * count how many parameter values before parameter p
   * @param p
   * @param parameters
   * @param pv
   * @return
   */
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
		  tmp[i] = name + "=\"" + value + "\"";
		else
		  tmp[i] = name + "=" + value;
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

  public static void main(String[] args) throws Exception{
	ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
	String[] var = {"a = true", "b = \"fg\"", "c = true"};
	for(String s: var)
	  engine.eval(s);
	String expr = "(b =\"fg\") && c";
	System.out.println(engine.eval(expr));

	String exp = "a&&(c=2)=>b";
	System.out.println(transImply(exp));
	/*

    ArrayList<String> pars = new ArrayList<>();
    pars.add("a");
    pars.add("bd");
    pars.add("tes");
    HashMap<String, Integer> value1 = new HashMap<>();
    value1.put("0",1);
    value1.put("1",2);
    HashMap<String, Integer> value2 = new HashMap<>();
    value2.put("16MC", 1);
    value2.put("8MC", 2);
    value2.put("BW",3);
    HashMap<String, Integer> value3 = new HashMap<>();
    value3.put("25",1);
    value3.put("26",2);
    value3.put("27",3);
    value3.put("28",4);
    value3.put("29",5);
    HashMap<String, HashMap<String, Integer>> pv = new HashMap<>();
    pv.put("a", value1);
    pv.put("bd", value2);
    pv.put("tes", value3);
    ArrayList<String[]> list = getAllCombinations(pars, pv);
    for(int i = 0; i < list.size(); i++){
      for(int j = 0; j < list.get(i).length; j++)
        System.out.print(list.get(i)[j] + " ");
      System.out.println();
	}
	BigDecimal result = null;
	Expression expression = new Expression("1+1/3");
	result = expression.eval();
	System.out.println(result);
	expression.setPrecision(2);
	result = expression.eval();
	System.out.println(result);
	Expression e =  new Expression("a == \"2\" && b ==2").with("a", "3").with("b", "1");
	e.addOperator(new AbstractOperator("=>", 10,true) {
	  @Override
	  public BigDecimal eval(BigDecimal bigDecimal, BigDecimal bigDecimal1) {
		return null;
	  }
	});
	result =  new Expression("a == as && b ==1").with("a", "as").with("b", "1").eval();
	System.out.println(result);*/
  }
}
