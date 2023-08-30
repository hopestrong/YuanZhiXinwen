package com.heima;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import java.util.HashMap;
import java.util.Map;

public class HutooljasonTest {
    public static void main(String[] args) {
        String html = "{\"name\":\"Something must have been changed since you leave\"}";
        JSONObject jsonObject = JSONUtil.parseObj(html);
        jsonObject.set("age",11);
        jsonObject.set("age",20);
        Map<String, String> map = new HashMap<>();
        map.put("token","djlgjsdlgsdg");
        jsonObject.set("data",map);
        String data = jsonObject.getStr("data");
        System.out.println(data);
        String s = jsonObject.toStringPretty();
        System.out.println(s);

    }
}
