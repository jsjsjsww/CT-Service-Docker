package com.neo.service;

import com.neo.domain.CTModel;
import com.neo.domain.TestSuite;



import java.io.*;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;

public class ACTSMethod {
  public static void generateModelFile(CTModel model){
    StringBuffer res = new StringBuffer("[System]\nName:s1\n[Parameter]\n");
	int[] values = model.getValues();
    for (int i = 0; i < values.length; i++) {
      res.append("p" + (i + 1) + "(int):");
      for (int j = 0; j < values[i]; j++) {
        res.append(j);
        if (j != values[i] -1)
          res.append(",");
	  }
	  res.append("\n");
	}
	res.append("[Constraint]\n");
	ArrayList<String> constraint = model.getConstraint();
    int[] valueSum = new int[values.length];
    valueSum[0] = 0;
    for(int i = 1; i < valueSum.length; i++)
      valueSum[i] = valueSum[i - 1] + values[i - 1];
    for(int i = 0; i < constraint.size(); i++){
      res.append(transfer(constraint.get(i), valueSum));
      res.append("\n");
	}

	try {
	  File file = new File("model.txt");
	  if (!file.exists()) {
		file.getParentFile().mkdirs();
		file.createNewFile();
	  }
	  FileWriter writer = new FileWriter(file);
	  writer.write(res.toString());
	  writer.close();
	} catch (IOException e) {
	  e.printStackTrace();
	}
  }

  public static long runACTS(String modelFile, int strength){
	System.out.println(System.getProperty("user.dir"));
	String command = "java -Ddoi=" + strength + " -jar acts_3.0.jar " + modelFile + " result.txt";
	Runtime runtime = Runtime.getRuntime();
	long res = 0;
	try{
	  Instant start = Instant.now();
	  runtime.exec(command).waitFor();
	  Instant end = Instant.now();
	  res = Duration.between(start, end).toMillis();
	}catch (Exception e){
	  e.printStackTrace();
	}
	return res;
  }

  public static String transfer(String constraint, int[] valueSum){
    StringBuffer sb = new StringBuffer();
    String[] split = constraint.split(" - ");
    split[0] = split[0].substring(2, split[0].length());
    int[] tmp = new int[split.length];
    for(int i = 0; i < tmp.length; i++)
      tmp[i] = Integer.parseInt(split[i]);
    int i = 0, j = 0;
    while(i < tmp.length){
      while (j < valueSum.length && tmp[i] >= valueSum[j])
		j++;
      split[i] = "p" + j + "=" + (tmp[i] - valueSum[j - 1]);
      i++;
	}
	for(i = 0; i < split.length; i++){
      String tmpString = "";
      for(j = 0; j < split.length; j++){
        if(i != j)
          tmpString += (split[j] + "&&");
	  }
      tmpString = tmpString.substring(0, tmpString.length() - 2);
	  tmpString += "=>" + split[i].substring(0,split[i].indexOf('=')) + "!" + split[i].substring(split[i].indexOf('='), split[i].length());
	  sb.append(tmpString + "\n");
	}
    return sb.toString();
  }

  public static TestSuite transferTestsuite(String filePath){
	ArrayList<int[]> testcases = new ArrayList<>();
	try{
	  BufferedReader br = new BufferedReader(new FileReader(filePath));
	  br.readLine();
	  br.readLine();
	  br.readLine();
	  String s = br.readLine();
	  int parameter = Integer.parseInt(s.replaceAll("\n", "").split(": ")[1]);
	  System.out.println("parameters = " + parameter);
	  while ((s = br.readLine()) != null){
	    if(!s.equals("") && s.split(" ")[0].equals("Configuration")){
	      int[] ts = new int[parameter];
	      br.readLine();
	      for (int i = 0; i < parameter; i++){
	        s = br.readLine();
	        ts[i] = Integer.parseInt(s.split("=")[2]);
		  }
		  testcases.add(ts);
		}
	  }
	}catch (Exception e){
	  System.out.println(e.getMessage());
	}
	finally {
	  return new TestSuite(testcases, 1);
	}
  }


  public static void  main(String[] args){
    ArrayList<String> constraint = new ArrayList<>();
    constraint.add("- 0 - 2");
    constraint.add("- 1 - 4");
    int[] values = new int[3];
    values[0] = 2;
    values[1] = 2;
    values[2] = 2;
    ArrayList<int[]> seed = new ArrayList<>();
    ArrayList<int[]> relation = new ArrayList<>();
    generateModelFile(new CTModel(3, 2, values, constraint, seed, relation));
	runACTS("ACTS/model.txt", 2);
	//TestSuite testSuite = transferTestsuite("ACTS/result.txt");
	//System.out.println(new JSONbject(testSuite).toString());
  }
}
