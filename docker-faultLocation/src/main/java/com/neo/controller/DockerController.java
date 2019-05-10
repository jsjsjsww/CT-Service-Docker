package com.neo.controller;

import com.neo.domain.TestCaseImplement;
import com.neo.domain.TestSuite;
import com.neo.domain.TestSuiteImplement;
import com.neo.domain.Tuple;
import com.neo.service.CTA;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@RestController
//@RequestMapping("/generation")
public class DockerController {

  @RequestMapping(value = "", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
  public String FaultLocation(HttpServletRequest request) throws Exception {
	BufferedReader br;
	StringBuilder sb;
	String reqBody = null;
	try {
	  br = new BufferedReader(new InputStreamReader(
			  request.getInputStream()));
	  String line;
	  sb = new StringBuilder();
	  while ((line = br.readLine()) != null) {
		sb.append(line);
	  }
	  reqBody = URLDecoder.decode(sb.toString(), "UTF-8");
	} catch (IOException e) {
	  // TODO Auto-generated catch block
	  e.printStackTrace();
	}
	JSONObject jsonObject = new JSONObject(reqBody);
	int parameters = (Integer) jsonObject.get("parameters");
	JSONArray jsonArray = (JSONArray) jsonObject.get("values");
	List valueList = jsonArray.toList();
	int[] values = new int[valueList.size()];
	for (int i = 0; i < values.length; i++)
	  values[i] = (Integer) valueList.get(i);
	if(parameters != values.length)
	  return getError("value array length does not equal to parameters number");
	jsonArray = (JSONArray) jsonObject.get("state");
	List stateList = jsonArray.toList();
	int[] state = new int[stateList.size()];
	for (int i = 0; i < values.length; i++)
	  state[i] = (Integer) stateList.get(i);
	JSONArray testsuiteJSONArray = (JSONArray) jsonObject.get("testsuite");
	List testsuiteList = testsuiteJSONArray.toList();
	int num = testsuiteList.size();
	if(num != state.length)
	  return getError("result array does not match testsuite");
	ArrayList<int[]> testsuite = new ArrayList<>();
	for (int i = 0; i < num; i++) {
	  JSONArray tmp = (JSONArray) (testsuiteJSONArray.get(i));
	  List tmpList = tmp.toList();
	  if(parameters != tmpList.size())
	    return getError("the " + (i + 1 ) + "th testcase has some thing wrong");
	  int[] testcase = new int[tmpList.size()];
	  for (int j = 0; j < testcase.length; j++) {
		testcase[j] = (Integer) tmpList.get(j);
		if(testcase[j] >= values[j])
		  return getError("the " + (i + 1) + "th testcase " + (j + 1) + "th parameter get wrong value");
	  }
	  testsuite.add(testcase);
	}
	Instant start = Instant.now();

	CTA cta = new CTA();
	String[] classes = {"pass", "err"};
	TestSuite suite = new TestSuiteImplement();

	for (int[] test : testsuite) {
	  TestCaseImplement testCase = new TestCaseImplement(test.length);
	  testCase.setTestCase(test);
	  suite.addTest(testCase);
	}
	String[] states = new String[testsuite.size()];
	for(int i = 0; i < states.length; i++){
	  if(state[i] == 0)
	    states[i] = "pass";
	  else
	    states[i] = "err";
	}
	cta.process(values, classes, suite, states);
	List<String> list = new ArrayList<>();
	List<Tuple> tuples = cta.getBugs();
	//System.out.println(tuples.size());
	for(Tuple tuple: tuples){
	  //System.out.println(tuple.toString());
	  list.add(tuple.toString());
	}
	Instant end = Instant.now();
	JSONObject res = new JSONObject();
	res.put("tuples", list);
	res.put("time", Duration.between(start, end).toMillis());
	return res.toString();
  }

  private String getError(String msg){
    JSONObject res = new JSONObject();
    res.put("description", msg);
    return res.toString();
  }
}