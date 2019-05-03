package com.neo.controller;

import com.neo.domain.TestSuite;
import com.neo.service.Rank;
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
public class DockerController {
	
    @RequestMapping(value = "", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    // ACTS 3.0 version
    public TestSuite sort(HttpServletRequest request) {
        BufferedReader br;
        StringBuilder sb = null;
        String reqBody = null;
        try {
            br = new BufferedReader(new InputStreamReader(
                    request.getInputStream()));
            String line = null;
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
        JSONArray weightJSONArray = (JSONArray) jsonObject.get("weight");
        List list = weightJSONArray.toList();
        int[] weights = new int[list.size()];
        for(int i = 0; i < weights.length; i++)
            weights[i] = (Integer)weightJSONArray.get(i);

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
        Rank rank = new Rank(parameters, testsuite.size(), weights, testsuite);
        rank.sort();
        Instant end = Instant.now();
        return new TestSuite(rank.getSortedTestsuite(), Duration.between(start, end).toMillis());
    }

}