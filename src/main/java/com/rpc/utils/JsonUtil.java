package com.rpc.utils;

import com.alibaba.fastjson.JSONObject;


/**
 * @user KyZhang
 * @date
 */
public class JsonUtil{


    /**
     * convert an instance to a JSON
     *
     * @param obj
     * @return
     */
    public static String pojoToJson(Object obj) {
        String json = JSONObject.toJSONString(obj);
        return json;
    }


    /**
     * convert a JSON to and instance
     *
     * @param json
     * @param tClass
     * @param <T>
     * @return
     */
    public static <T> T jsonToPojo(String json, Class<T> tClass) {
        T t = JSONObject.parseObject(json, tClass);
        return t;
    }

}