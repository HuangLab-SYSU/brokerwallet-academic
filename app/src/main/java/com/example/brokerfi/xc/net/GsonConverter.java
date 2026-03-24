package com.example.brokerfi.xc.net;

import com.google.gson.Gson;

import java.lang.reflect.Type;

public class GsonConverter {

    private static final Gson gson = new Gson();

    public static <T> T fromJson(String json, Type type) {
        return gson.fromJson(json, type);
    }
}