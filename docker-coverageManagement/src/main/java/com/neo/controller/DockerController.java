package com.neo.controller;

import com.neo.domain.EvaluationRes;
import com.neo.service.Evaluation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.json.*;

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
//@RequestMapping("/coverage")
public class DockerController {
	
    @RequestMapping(value = "", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    public EvaluationRes reduce(HttpServletRequest request) {
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
            //reqBody = reqBody.substring(reqBody.indexOf("{"));
            //request.setAttribute("inputParam", reqBody);
            //System.out.println("JsonReq reqBody>>>>>" + reqBody);
            //return reqBody;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        JSONObject jsonObject = new JSONObject(reqBody);
        int parameters = (Integer)jsonObject.get("parameters");
        int t = (Integer)jsonObject.get("strength");
        JSONArray valuesJSONArray = (JSONArray) jsonObject.get("values");
        List list = valuesJSONArray.toList();
        int[] values = new int[list.size()];
        for(int i = 0; i < values.length; i++)
            values[i] = (Integer)valuesJSONArray.get(i);

        JSONArray testsuiteJSONArray = (JSONArray) jsonObject.get("testsuite");
        List testsuiteList = testsuiteJSONArray.toList();
        int num = testsuiteList.size();
        ArrayList<int[]> testsuite = new ArrayList<>();
        for(int i = 0; i < num; i++){
            JSONArray tmp = (JSONArray)(testsuiteJSONArray.get(i));
            List tmpList = tmp.toList();
            int[] testcase = new int[tmpList.size()];
            for(int j = 0; j < testcase.length; j++)
                testcase[j] = (Integer)tmpList.get(j);
            testsuite.add(testcase);
        }
        Instant start = Instant.now();
        Evaluation evaluation = new Evaluation(t, testsuite, parameters, testsuite.size(), values);
        evaluation.calculateCoverage();
        Instant end = Instant.now();
        return new EvaluationRes(evaluation.getCover(), Duration.between(start, end).toMillis());
    }

}