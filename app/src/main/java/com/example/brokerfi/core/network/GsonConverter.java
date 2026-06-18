package com.example.brokerfi.core.network;

import com.google.gson.Gson;

import java.lang.reflect.Type;

public class GsonConverter {

    private static final Gson gson = new Gson();

    public static <T> T fromJson(String json, Type type) {
        return gson.fromJson(json, type);
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        return gson.fromJson(json, clazz);
    }

    public static String toJson(Object body) {
        if (body == null) {
            return "{}";
        }
        return gson.toJson(body);
    }
}
