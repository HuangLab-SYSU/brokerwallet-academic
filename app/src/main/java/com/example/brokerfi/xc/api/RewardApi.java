package com.example.brokerfi.xc.api;

import com.example.brokerfi.xc.net.ApiCallback;
import com.example.brokerfi.xc.net.BaseApi;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class RewardApi extends BaseApi {

    String url = "http://10.0.2.2:5001/reward/verify";

    public void verifyReward(
            String txHash,
            String from,
            String to,
            long timestamp,
            String nonce,
            String r,
            String s,
            String v,
            ApiCallback<Boolean> callback
    ) {
        try {
            // 使用 Map 代替 JSONObject，避免 nameValuePairs 包裹
            Map<String, Object> body = new HashMap<>();
            body.put("txHash", txHash);
            body.put("from", from);
            body.put("to", to);
            body.put("timestamp", timestamp);
            body.put("nonce", nonce);
            body.put("r", r);
            body.put("s", s);
            body.put("v", v);

            // 执行 POST 请求，类型仍为 Boolean
            executePost(url, body, Boolean.class, callback);

        } catch (Exception e) {
            callback.onFail("构造请求失败");
        }
    }
}
