package com.example.brokerfi.xc;

import android.util.Log;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;

// 导入配置包中的ServerConfig
import com.example.brokerfi.config.ServerConfig;

public class MedalApiUtil {
    private static final String TAG = "MedalApiUtil";
    private static final OkHttpClient client = new OkHttpClient();
    
    /**
     * 获取勋章排行榜数据
     * @return JSON字符串，失败返回null
     */
    public static String getMedalRanking() {
        Request request = new Request.Builder()
                .url(ServerConfig.MEDAL_RANKING_API)
                .build();
        
        // 使用try-with-resources自动管理Response资源
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                ResponseBody body = response.body();
                if (body != null) {
                    return body.string();
                } else {
                    Log.w(TAG, "Response body is null for medal ranking");
                }
            } else {
                Log.w(TAG, "Failed to get medal ranking, response code: " + response.code());
            }
        } catch (IOException e) {
            Log.e(TAG, "Network error while fetching medal ranking", e);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while fetching medal ranking", e);
        }
        return null;
    }
    
    /**
     * 根据地址获取勋章信息
     * @param address 用户地址
     * @return JSON字符串，失败返回null
     */
    public static String getMedalByAddress(String address) {
        if (address == null || address.trim().isEmpty()) {
            Log.w(TAG, "Address is null or empty");
            return null;
        }
        
        Request request = new Request.Builder()
                .url(ServerConfig.getApiUrl("/api/medal/query?address=" + address))
                .build();
        
        // 使用try-with-resources自动管理Response资源
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                ResponseBody body = response.body();
                if (body != null) {
                    return body.string();
                } else {
                    Log.w(TAG, "Response body is null for address: " + address);
                }
            } else {
                Log.w(TAG, "Failed to get medal for address: " + address + 
                        ", response code: " + response.code());
            }
        } catch (IOException e) {
            Log.e(TAG, "Network error while fetching medal for address: " + address, e);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while fetching medal for address: " + address, e);
        }
        return null;
    }
    
    /**
     * 获取全局勋章统计信息
     * @return JSON字符串，失败返回null
     */
    public static String getGlobalStats() {
        Request request = new Request.Builder()
                .url(ServerConfig.getApiUrl("/api/medal/stats"))
                .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                ResponseBody body = response.body();
                if (body != null) {
                    String result = body.string();
                    Log.d(TAG, "Global stats response: " + result);
                    return result;
                } else {
                    Log.w(TAG, "Response body is null for global stats");
                }
            } else {
                Log.w(TAG, "Failed to get global stats, response code: " + response.code());
            }
        } catch (IOException e) {
            Log.e(TAG, "Network error while fetching global stats", e);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while fetching global stats", e);
        }
        return null;
    }
    
    /**
     * 获取服务器信息
     * @return JSON字符串，失败返回null
     */
    public static String getServerInfo() {
        Request request = new Request.Builder()
                .url(ServerConfig.getApiUrl("/api/server/info"))
                .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                ResponseBody body = response.body();
                if (body != null) {
                    String result = body.string();
                    Log.d(TAG, "Server info response: " + result);
                    return result;
                } else {
                    Log.w(TAG, "Response body is null for server info");
                }
            } else {
                Log.w(TAG, "Failed to get server info, response code: " + response.code());
            }
        } catch (IOException e) {
            Log.e(TAG, "Network error while fetching server info", e);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while fetching server info", e);
        }
        return null;
    }
    
    /**
     * 获取系统健康状态
     * @return JSON字符串，失败返回null
     */
    public static String getHealthStatus() {
        Request request = new Request.Builder()
                .url(ServerConfig.getApiUrl("/api/health"))
                .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                ResponseBody body = response.body();
                if (body != null) {
                    String result = body.string();
                    Log.d(TAG, "Health status response: " + result);
                    return result;
                } else {
                    Log.w(TAG, "Response body is null for health status");
                }
            } else {
                Log.w(TAG, "Failed to get health status, response code: " + response.code());
            }
        } catch (IOException e) {
            Log.e(TAG, "Network error while fetching health status", e);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while fetching health status", e);
        }
        return null;
    }
    
    /**
     * 获取所有NFT
     * @param page 页码
     * @param size 每页大小
     * @return JSON字符串，失败返回null
     */
    public static String getAllNfts(int page, int size) {
        String url = ServerConfig.getApiUrl("/api/blockchain/nft/all") + "?page=" + page + "&size=" + size;
        Log.d(TAG, "Calling getAllNfts API: " + url);
        Request request = new Request.Builder()
                .url(url)
                .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                ResponseBody body = response.body();
                if (body != null) {
                    String result = body.string();
                    Log.d(TAG, "All NFTs response: " + result);
                    return result;
                } else {
                    Log.w(TAG, "Response body is null for all NFTs");
                }
            } else {
                Log.w(TAG, "Failed to get all NFTs, response code: " + response.code());
            }
        } catch (IOException e) {
            Log.e(TAG, "Network error while fetching all NFTs", e);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while fetching all NFTs", e);
        }
        return null;
    }
}