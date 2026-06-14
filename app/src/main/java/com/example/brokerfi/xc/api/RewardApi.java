package com.example.brokerfi.xc.api;

import static com.example.brokerfi.core.config.ApiConfig.BASE_URL_HTTP;

import com.example.brokerfi.core.network.ApiCallback;
import com.example.brokerfi.core.network.ApiResponse;
import com.example.brokerfi.core.network.BaseApi;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import com.example.brokerfi.core.config.ApiConfig;


public class RewardApi extends BaseApi {

    private static final String url = BASE_URL_HTTP + "/reward/verify";

    /**
     * 后端验证请求
     * 由于目前brokerchain没有收据树，无法直接通过txHash验证交易状态
     * 因此采用基于账户交易记录的弱匹配机制进行交易确认（fromAddr + toAddr +timestamp）
     * @param txHash 交易哈希
     * @param from 打赏人地址
     * @param to 受赏人地址
     * @param timestamp 交易时间戳
     * @param nonce nonce
     * @param r
     * @param s
     * @param v
     * @param amount 交易金额（用于后端入库
     * @param postId 受赏帖子ID用于后端入库
     * @param callback
     */
    public void verifyReward(
            String txHash,
            String from,
            String to,
            long timestamp,
            String nonce,
            String r,
            String s,
            String v,
            String amount,
            Long postId,
            ApiCallback<Boolean> callback
    ) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("txHash", txHash);
            body.put("from", from);
            body.put("to", to);
            body.put("timestamp", timestamp);
            body.put("nonce", nonce);
            body.put("r", r);
            body.put("s", s);
            body.put("v", v);
            body.put("amount", amount);
            body.put("postId", postId);

            Type type = new TypeToken<ApiResponse<Boolean>>() {}.getType();
            executePost(url, body, type, callback);

        } catch (Exception e) {
            callback.onFail("Failed to build request");
        }
    }
}
