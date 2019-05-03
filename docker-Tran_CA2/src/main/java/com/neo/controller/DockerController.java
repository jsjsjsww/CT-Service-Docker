package com.neo.controller;
import com.neo.service.transCA;
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
import java.util.ArrayList;
import java.util.List;

@RestController
//@RequestMapping("/generation")
public class DockerController {
	
    @RequestMapping(value = "", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    // ACTS 3.0 version
    public String trans_CA(HttpServletRequest request) {
        BufferedReader br;
        StringBuilder sb = null;
        String reqBody = null;
        try {
            br = new BufferedReader(new InputStreamReader(
                    request.getInputStream()));
            String line = null;
            sb = new StringBuilder();
            while ((line = br.readLine()) != null) {
                if(line.length() == 0)
                    continue;
                sb.append(line);
            }
            reqBody = URLDecoder.decode(sb.toString(), "UTF-8");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.out.println(reqBody);
        JSONObject jsonObject = new JSONObject(reqBody);

        JSONArray testsuiteJSONArray = (JSONArray) jsonObject.get("testsuite");
        List testsuiteList = testsuiteJSONArray.toList();
        int num = testsuiteList.size();
        ArrayList<String[]> testsuite = new ArrayList<>();
        for(int i = 0; i < num; i++){
            JSONArray tmp = (JSONArray)(testsuiteJSONArray.get(i));
            List tmpList = tmp.toList();
            String[] testcase = new String[tmpList.size()];
            for(int j = 0; j < testcase.length; j++)
                testcase[j] = (String)tmpList.get(j);
            testsuite.add(testcase);
        }

        String step = (String) jsonObject.get("templateTest");

        JSONObject res = transCA.trans(testsuite, step);
        return res.toString();
    }

}