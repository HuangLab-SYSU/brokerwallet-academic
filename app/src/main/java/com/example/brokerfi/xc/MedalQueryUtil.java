package com.example.brokerfi.xc;

import android.util.Log;

import com.example.brokerfi.xc.net.ABIUtils;

public class MedalQueryUtil {
    
    public interface MedalQueryCallback {
        void onSuccess(ABIUtils.MedalQueryResult result);
        void onError(String error);
    }
    
    public interface GlobalStatsCallback {
        void onSuccess(ABIUtils.GlobalStatsResult result);
        void onError(String error);
    }
    
    /**
     * 查询用户勋章数据
     */
    public static void queryUserMedals(String address, String privateKey, MedalQueryCallback callback) {
        try {
            Log.d("MedalQuery", "查询用户勋章，地址: " + address);
            
            // 编码查询函数
            String data = ABIUtils.encodeGetUserMedals(address);
            
            // 使用勋章合约地址发送区块链查询
            String response = sendMedalCall(data, privateKey);
            
            if (response != null && !response.trim().isEmpty()) {
                Log.d("MedalQuery", "收到响应: " + response);
                
                // 解码响应
                ABIUtils.MedalQueryResult result = ABIUtils.decodeGetUserMedals(response);
                callback.onSuccess(result);
            } else {
                Log.w("MedalQuery", "响应为空");
                callback.onError("无响应数据");
            }
        } catch (Exception e) {
            Log.e("MedalQuery", "查询用户勋章失败", e);
            callback.onError("查询失败: " + e.getMessage());
        }
    }
    
    /**
     * 查询全局勋章统计
     */
    public static void queryGlobalStats(String privateKey, GlobalStatsCallback callback) {
        try {
            Log.d("MedalQuery", "查询全局勋章统计");
            
            // 编码查询函数
            String data = ABIUtils.encodeGetGlobalStats();
            
            // 使用勋章合约地址发送区块链查询
            String response = sendMedalCall(data, privateKey);
            
            if (response != null && !response.trim().isEmpty()) {
                Log.d("MedalQuery", "收到全局统计响应: " + response);
                
                // 解码响应
                ABIUtils.GlobalStatsResult result = ABIUtils.decodeGetGlobalStats(response);
                callback.onSuccess(result);
            } else {
                Log.w("MedalQuery", "全局统计响应为空");
                callback.onError("无响应数据");
            }
        } catch (Exception e) {
            Log.e("MedalQuery", "查询全局统计失败", e);
            callback.onError("查询失败: " + e.getMessage());
        }
    }
    
    /**
     * 发送勋章合约查询请求
     * 使用正确的勋章合约地址：0x1a202bfa10ea97a742ad22fcb1a7913821bf1b18
     */
    private static String sendMedalCall(String data, String privateKey) {
        try {
            java.util.concurrent.atomic.AtomicReference<String> reference = new java.util.concurrent.atomic.AtomicReference<>();
            java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
            
            // 使用线程池执行
            java.util.concurrent.ExecutorService service = java.util.concurrent.Executors.newCachedThreadPool();
            service.execute(() -> {
                try {
                    String uuid = java.util.UUID.randomUUID().toString();
                    com.example.brokerfi.xc.CallReq req = new com.example.brokerfi.xc.CallReq();
                    
                    // 使用勋章合约地址
                    String medalContractAddr = "0x1a202bfa10ea97a742ad22fcb1a7913821bf1b18";
                    String thedata = medalContractAddr + data + "0x0" + uuid;
                    String[] sign = com.example.brokerfi.xc.SecurityUtil.signECDSA(privateKey, thedata);

                    req.setPublicKey(com.example.brokerfi.xc.SecurityUtil.getPublicKeyFromPrivateKey(privateKey));
                    req.setData(data);
                    req.setRandomStr(uuid);
                    req.setTo(medalContractAddr);  // 使用勋章合约地址
                    req.setValue("0x0");
                    req.setSign1(sign[0]);
                    req.setSign2(sign[1]);
                    
                    byte[] bytes = com.example.brokerfi.xc.HTTPUtil.doPost("eth_call", req);
                    reference.set(new String(bytes));
                    latch.countDown();
                    return;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                reference.set(null);
                latch.countDown();
            });
            
            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return reference.get();
        } catch (Exception e) {
            Log.e("MedalQuery", "发送勋章合约查询失败", e);
            return null;
        }
    }
}