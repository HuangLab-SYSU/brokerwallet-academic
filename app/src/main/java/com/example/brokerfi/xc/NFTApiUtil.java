package com.example.brokerfi.xc;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class NFTApiUtil {
    private static final String BASE_URL = "http://localhost:3001/api"; // contract项目的API地址
    
    public static String mintNFT(String name, String description, String imageType, String imagePath) {
        try {
            // 构建请求数据
            JsonObject requestData = new JsonObject();
            requestData.addProperty("name", name);
            requestData.addProperty("description", description);
            requestData.addProperty("imageType", imageType);
            
            // 如果是自定义图片，读取并转换为Base64
            if ("自定义图片".equals(imageType) && imagePath != null) {
                File file = new File(imagePath);
                if (file.exists()) {
                    FileInputStream fis = new FileInputStream(file);
                    byte[] fileBytes = new byte[(int) file.length()];
                    fis.read(fileBytes);
                    fis.close();
                    
                    String base64Image = android.util.Base64.encodeToString(fileBytes, android.util.Base64.DEFAULT);
                    requestData.addProperty("imageData", base64Image);
                }
            }
            
            // 发送请求
            URL url = new URL(BASE_URL + "/mint-nft");
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
    
    public static String getUserNFTs(String address) {
        try {
            URL url = new URL(BASE_URL + "/query-nft/" + address);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "application/json");
            
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                java.util.Scanner scanner = new java.util.Scanner(connection.getInputStream(), "UTF-8").useDelimiter("\\A");
                return scanner.hasNext() ? scanner.next() : "";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return getMockNFTData();
    }
    
    public static String getAllNFTs() {
        try {
            URL url = new URL(BASE_URL + "/query-all-nfts");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "application/json");
            
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                java.util.Scanner scanner = new java.util.Scanner(connection.getInputStream(), "UTF-8").useDelimiter("\\A");
                return scanner.hasNext() ? scanner.next() : "";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return getMockNFTData();
    }
    
    private static String getMockNFTData() {
        // 模拟NFT数据
        JsonObject response = new JsonObject();
        com.google.gson.JsonArray nfts = new com.google.gson.JsonArray();
        
        // 模拟NFT项目
        String[] names = {"科研贡献奖章", "代码贡献NFT", "论文发表纪念", "会议报告证书", "项目合作徽章"};
        String[] descriptions = {
            "表彰在BlockEmulator项目中的杰出科研贡献",
            "为开源项目贡献代码的纪念NFT",
            "发表高质量学术论文的荣誉证书",
            "在重要会议上做报告的纪念品",
            "参与重要项目合作的纪念徽章"
        };
        String[] tokenIds = {"1", "2", "3", "4", "5"};
        String[] owners = {
            "0x1234567890123456789012345678901234567890",
            "0x2345678901234567890123456789012345678901",
            "0x3456789012345678901234567890123456789012",
            "0x4567890123456789012345678901234567890123",
            "0x5678901234567890123456789012345678901234"
        };
        
        for (int i = 0; i < names.length; i++) {
            JsonObject nft = new JsonObject();
            nft.addProperty("tokenId", tokenIds[i]);
            nft.addProperty("name", names[i]);
            nft.addProperty("description", descriptions[i]);
            nft.addProperty("imageUrl", "https://via.placeholder.com/300x300?text=NFT+" + (i + 1));
            nft.addProperty("ownerAddress", owners[i]);
            nfts.add(nft);
        }
        
        response.add("nfts", nfts);
        response.addProperty("success", true);
        
        return new Gson().toJson(response);
    }
}




