package com.rpc.utils;

import com.alibaba.fastjson.JSONObject;

import java.io.UnsupportedEncodingException;

/**
 * @user KyZhang
 * @date
 */
public class JsonUtil{


    /**
     * convert an instance to a JSON, and then to a byte array
     *
     * @param obj
     * @return
     */
    public static byte[] Object2JsonBytes(Object obj) {
        String json = pojoToJson(obj);
        try {
            return json.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * convert a byte array to a JSON, and then to an instance
     *
     * @param bytes
     * @param tClass
     * @param <T>
     * @return
     */
    public static <T> T JsonBytes2Object(byte[] bytes, Class<T> tClass) {
        try {
            String json = new String(bytes, "UTF-8");
            T t = jsonToPojo(json, tClass);
            return t;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

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