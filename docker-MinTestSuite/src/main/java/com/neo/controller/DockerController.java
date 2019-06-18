package com.neo.controller;

import com.neo.service.parser;
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
import java.util.HashMap;
import java.util.List;

@RestController
//@RequestMapping("/generation")
public class DockerController {
	
    @RequestMapping(value = "", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    public String chooseMinTS(HttpServletRequest request) {
        BufferedReader br;
        StringBuilder sb = null;
        String reqBody = null;
        try {
            br = new BufferedReader(new InputStreamReader(
                    request.getInputStream()));
            String line;
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
        System.out.println(sb);
        JSONObject jsonObject = new JSONObject(reqBody);
        JSONArray jsonArray = (JSONArray)jsonObject.get("result");
        if(jsonArray == null)
            return "parser error";
        List list = jsonArray.toList();
        int min = Integer.MAX_VALUE, target = -1;
        for(int i = 0; i < list.size(); i++){
            HashMap tmp = (HashMap) list.get(i);
            List tmpArray = (List) tmp.get("testsuite");
            if(min > tmpArray.size()){
                min = tmpArray.size();
                target = i;
            }
        }
        return jsonArray.get(target).toString();
    }

}