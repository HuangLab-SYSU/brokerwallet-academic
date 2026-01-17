package com.example.brokerfi.xc;

import com.google.gson.Gson;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class HTTPUtil {
    public static byte[] doPost(String url, Object requestBody) throws Exception {
        Gson gson = new Gson();
        String jsonInputString = gson.toJson(requestBody);
//        String urlString = "https://" + Holder.serverHost + ":" + Holder.serverPort+"/";
        String urlString = "https://" + Holder.serverHost + ":" + "443"+"/";
        urlString += url;

        URL requestUrl = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) requestUrl.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; utf-8");
        connection.setRequestProperty("Accept", "application/json");
        connection.setDoOutput(true);

        try(OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonInputString.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        int responseCode = connection.getResponseCode();
        if(responseCode != HttpURLConnection.HTTP_OK) {
            throw new RuntimeException("HttpResponseCode: " + responseCode);
        } else {
            java.util.Scanner scanner = new java.util.Scanner(connection.getInputStream(), "UTF-8").useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next().getBytes() : new byte[0];
        }
    }

    public static byte[] doPost2(String url, Object requestBody) throws Exception {
        Gson gson = new Gson();
        String jsonInputString = gson.toJson(requestBody);
//        String urlString = "http://" + Holder.serverHost + ":" + Holder.serverPort+"/";
//        urlString += url;

        URL requestUrl = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) requestUrl.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; utf-8");
        connection.setRequestProperty("Accept", "application/json");
        connection.setDoOutput(true);

        try(OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonInputString.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        int responseCode = connection.getResponseCode();
        if(responseCode != HttpURLConnection.HTTP_OK) {
            throw new RuntimeException("HttpResponseCode: " + responseCode);
        } else {
            java.util.Scanner scanner = new java.util.Scanner(connection.getInputStream(), "UTF-8").useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next().getBytes() : new byte[0];
        }
    }

    public static byte[] doGet(String url, Object requestBody) throws Exception {
        Gson gson = new Gson();
        String jsonInputString = gson.toJson(requestBody);
        //String urlString = "https://" + Holder.serverHost + ":" + Holder.serverPort+"/";
        String urlString = "https://" + Holder.serverHost + ":" +"443" +"/";
        urlString += url;

        URL requestUrl = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) requestUrl.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json; utf-8");
        connection.setRequestProperty("Accept", "application/json");
        connection.setDoOutput(true);

        try(OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonInputString.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        int responseCode = connection.getResponseCode();
        if(responseCode != HttpURLConnection.HTTP_OK) {
            throw new RuntimeException("HttpResponseCode: " + responseCode);
        } else {
            java.util.Scanner scanner = new java.util.Scanner(connection.getInputStream(), "UTF-8").useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next().getBytes() : new byte[0];
        }
    }

    public static byte[] doGet2(String url, Object requestBody) throws Exception {
        Gson gson = new Gson();
        String jsonInputString = gson.toJson(requestBody);

        URL requestUrl = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) requestUrl.openConnection();
        connection.setRequestMethod("GET");
//        connection.setRequestProperty("Content-Type", "application/json; utf-8");
//        connection.setRequestProperty("Accept", "application/json");
        connection.setDoOutput(true);

        try(OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonInputString.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        int responseCode = connection.getResponseCode();
        if(responseCode != HttpURLConnection.HTTP_OK) {
            throw new RuntimeException("HttpResponseCode: " + responseCode);
        } else {
            java.util.Scanner scanner = new java.util.Scanner(connection.getInputStream(), "UTF-8").useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next().getBytes() : new byte[0];
        }
    }
}