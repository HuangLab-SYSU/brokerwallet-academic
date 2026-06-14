package com.example.brokerfi.token.wrappedbkc;

import android.content.Context;
import android.text.TextUtils;

import com.example.brokerfi.core.blockchain.ChainAddressUtil;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Uint256;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/** wrapped BKC contract: address resolution and deposit/withdraw calldata. */
public final class wrappedBkcContractHelper {

    private wrappedBkcContractHelper() {
    }

    public static String resolveContractAddress(Context context) {
        if (!TextUtils.isEmpty(wrappedBkcConfig.WRAPPED_BKC_CONTRACT)) {
            return ChainAddressUtil.normalizeAddress(wrappedBkcConfig.WRAPPED_BKC_CONTRACT);
        }
        try (InputStream in = context.getAssets().open(wrappedBkcConfig.DEPLOYMENT_ASSET)) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            JsonObject json = JsonParser.parseString(sb.toString()).getAsJsonObject();
            String address = json.has("contractAddress")
                    ? json.get("contractAddress").getAsString()
                    : null;
            if (!TextUtils.isEmpty(address)) {
                return ChainAddressUtil.normalizeAddress(address);
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    public static String encodeDeposit() {
        Function function = new Function("deposit", Collections.emptyList(), Collections.emptyList());
        return FunctionEncoder.encode(function);
    }

    public static String encodeWithdraw(BigInteger amount) {
        Function function = new Function(
                "withdraw",
                Collections.singletonList(new Uint256(amount)),
                Collections.emptyList());
        return FunctionEncoder.encode(function);
    }
}
