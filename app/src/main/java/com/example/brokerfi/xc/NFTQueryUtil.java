package com.example.brokerfi.xc;

import android.util.Log;

import com.example.brokerfi.xc.net.ABIUtils;

import java.math.BigInteger;

public class NFTQueryUtil {
    
    public interface NFTQueryCallback {
        void onSuccess(ABIUtils.UserNftsResult result);
        void onError(String error);
    }
    
    public interface NFTDataCallback {
        void onSuccess(ABIUtils.NftDataResult result);
        void onError(String error);
    }
    
    /**
     * 查询用户NFT列表
     */
    public static void queryUserNfts(String address, String privateKey, NFTQueryCallback callback) {
        try {
            Log.d("NFTQuery", "查询用户NFT，地址: " + address);
            
            // 编码查询函数
            String data = ABIUtils.encodeGetUserNfts(address);
            
            // 使用NFT合约地址发送区块链查询
            String response = sendNftCall(data, privateKey);
            
            if (response != null && !response.trim().isEmpty()) {
                Log.d("NFTQuery", "收到NFT响应: " + response);
                
                // 解码响应
                ABIUtils.UserNftsResult result = ABIUtils.decodeGetUserNfts(response);
                callback.onSuccess(result);
            } else {
                Log.w("NFTQuery", "NFT响应为空");
                callback.onError("无响应数据");
            }
        } catch (Exception e) {
            Log.e("NFTQuery", "查询用户NFT失败", e);
            callback.onError("查询失败: " + e.getMessage());
        }
    }
    
    /**
     * 查询NFT详细信息
     */
    public static void queryNftData(BigInteger tokenId, String privateKey, NFTDataCallback callback) {
        try {
            Log.d("NFTQuery", "查询NFT数据，Token ID: " + tokenId);
            
            // 编码查询函数
            String data = ABIUtils.encodeGetNftData(tokenId);
            
            // 使用NFT合约地址发送区块链查询
            String response = sendNftCall(data, privateKey);
            
            if (response != null && !response.trim().isEmpty()) {
                Log.d("NFTQuery", "收到NFT数据响应: " + response);
                
                // 解码响应
                ABIUtils.NftDataResult result = ABIUtils.decodeGetNftData(response);
                callback.onSuccess(result);
            } else {
                Log.w("NFTQuery", "NFT数据响应为空");
                callback.onError("无响应数据");
            }
        } catch (Exception e) {
            Log.e("NFTQuery", "查询NFT数据失败", e);
            callback.onError("查询失败: " + e.getMessage());
        }
    }
    
    /**
     * 发送NFT合约查询请求
     * 使用正确的NFT合约地址：0x382ca68b8133893fdf46170efd839c7703d9e9ae
     */
    private static String sendNftCall(String data, String privateKey) {
        try {
            java.util.concurrent.atomic.AtomicReference<String> reference = new java.util.concurrent.atomic.AtomicReference<>();
            java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
            
            // 使用线程池执行
            java.util.concurrent.ExecutorService service = java.util.concurrent.Executors.newCachedThreadPool();
            service.execute(() -> {
                try {
                    String uuid = java.util.UUID.randomUUID().toString();
                    com.example.brokerfi.xc.CallReq req = new com.example.brokerfi.xc.CallReq();
                    
                    // 使用NFT合约地址
                    String nftContractAddr = "0x382ca68b8133893fdf46170efd839c7703d9e9ae";
                    String thedata = nftContractAddr + data + "0x0" + uuid;
                    String[] sign = com.example.brokerfi.xc.SecurityUtil.signECDSA(privateKey, thedata);

                    req.setPublicKey(com.example.brokerfi.xc.SecurityUtil.getPublicKeyFromPrivateKey(privateKey));
                    req.setData(data);
                    req.setRandomStr(uuid);
                    req.setTo(nftContractAddr);  // 使用NFT合约地址
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
            Log.e("NFTQuery", "发送NFT合约查询失败", e);
            return null;
        }
    }
}