package com.example.brokerfi.xc;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ProofApiUtil {
    private static final String BASE_URL = "http://localhost:3001/api"; // contract项目的API地址
    
    public static String submitProof(String authorInfo, String eventType, String eventDescription, 
                                   String contributionLevel, String filePath) {
        try {
            // 构建请求数据
            JsonObject requestData = new JsonObject();
            requestData.addProperty("authorInfo", authorInfo);
            requestData.addProperty("eventType", eventType);
            requestData.addProperty("eventDescription", eventDescription);
            requestData.addProperty("contributionLevel", contributionLevel);
            
            // 读取文件并转换为Base64
            if (filePath != null) {
                File file = new File(filePath);
                if (file.exists()) {
                    FileInputStream fis = new FileInputStream(file);
                    byte[] fileBytes = new byte[(int) file.length()];
                    fis.read(fileBytes);
                    fis.close();
                    
                    String base64File = android.util.Base64.encodeToString(fileBytes, android.util.Base64.DEFAULT);
                    requestData.addProperty("fileData", base64File);
                    requestData.addProperty("fileName", file.getName());
                }
            }
            
            // 发送请求
            URL url = new URL(BASE_URL + "/submit-proof");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            
            String jsonInputString = new Gson().toJson(requestData);
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }
            
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                java.util.Scanner scanner = new java.util.Scanner(connection.getInputStream(), "UTF-8").useDelimiter("\\A");
                return scanner.hasNext() ? scanner.next() : "";
            } else {
                return "HTTP Error: " + responseCode;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }
}




