package com.example.brokerfi.token;

import com.example.brokerfi.xc.ChainAddressUtil;
import com.example.brokerfi.token.TokenContractHelper;
import com.example.brokerfi.xc.DashEthCall;
import android.text.TextUtils;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;

import com.example.brokerfi.token.TokenConfig;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/** Reads ERC-20 {@code symbol}/{@code name} metadata with string and bytes32 fallback. */
public final class TokenMetadataHelper {

    public static final class Erc20Metadata {
        public final String symbol;
        public final String name;
        public final int decimals;

        Erc20Metadata(String symbol, String name, int decimals) {
            this.symbol = symbol;
            this.name = name;
            this.decimals = decimals;
        }
    }

    private TokenMetadataHelper() {
    }

    /**
     * Validates an ERC-20 contract by requiring readable {@code symbol} and
     * {@code decimals} values. Returns {@code null} when validation fails.
     */
    @androidx.annotation.Nullable
    public static Erc20Metadata fetchValidatedMetadata(String contractAddress, String privateKey) {
        String symbol = readSymbol(contractAddress, privateKey);
        if (TextUtils.isEmpty(symbol)) {
            return null;
        }
        Integer decimals = readDecimalsStrict(contractAddress, privateKey);
        if (decimals == null) {
            return null;
        }
        String name = readName(contractAddress, privateKey);
        if (TextUtils.isEmpty(name)) {
            name = symbol;
        }
        return new Erc20Metadata(symbol, name, decimals);
    }

    public static String readSymbol(String contractAddress, String privateKey) {
        return readTokenString(contractAddress, "symbol", privateKey);
    }

    public static String readName(String contractAddress, String privateKey) {
        return readTokenString(contractAddress, "name", privateKey);
    }

    public static int readDecimals(String contractAddress, String privateKey) {
        Integer strict = readDecimalsStrict(contractAddress, privateKey);
        return strict != null ? strict : TokenConfig.TOKEN_DECIMALS;
    }

    @androidx.annotation.Nullable
    private static Integer readDecimalsStrict(String contractAddress, String privateKey) {
        try {
            Function function = new Function(
                    "decimals",
                    Collections.emptyList(),
                    Collections.singletonList(new TypeReference<Uint256>() {}));
            String result = ethCall(contractAddress, FunctionEncoder.encode(function), privateKey);
            if (TextUtils.isEmpty(result)) {
                return null;
            }
            List<Type> decoded = FunctionReturnDecoder.decode(result, function.getOutputParameters());
            if (decoded.isEmpty() || decoded.get(0) == null) {
                return null;
            }
            Object value = decoded.get(0).getValue();
            if (value instanceof BigInteger) {
                int d = ((BigInteger) value).intValue();
                return d >= 0 && d <= 36 ? d : null;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String readTokenString(String contractAddress, String method, String privateKey) {
        String utf8 = readAsUtf8(contractAddress, method, privateKey);
        if (!TextUtils.isEmpty(utf8)) {
            return utf8;
        }
        return readAsBytes32(contractAddress, method, privateKey);
    }

    private static String readAsUtf8(String contractAddress, String method, String privateKey) {
        try {
            Function function = new Function(
                    method,
                    Collections.emptyList(),
                    Collections.singletonList(new TypeReference<Utf8String>() {}));
            return decodeStringResult(function, ethCall(contractAddress, FunctionEncoder.encode(function), privateKey));
        } catch (Exception e) {
            return "";
        }
    }

    private static String readAsBytes32(String contractAddress, String method, String privateKey) {
        try {
            Function function = new Function(
                    method,
                    Collections.emptyList(),
                    Collections.singletonList(new TypeReference<Bytes32>() {}));
            String result = ethCall(contractAddress, FunctionEncoder.encode(function), privateKey);
            if (TextUtils.isEmpty(result)) {
                return "";
            }
            List<Type> decoded = FunctionReturnDecoder.decode(result, function.getOutputParameters());
            if (decoded.isEmpty() || decoded.get(0) == null) {
                return "";
            }
            Object value = decoded.get(0).getValue();
            if (value instanceof byte[]) {
                return trimZeroBytes((byte[]) value);
            }
            return value != null ? value.toString().trim() : "";
        } catch (Exception e) {
            return "";
        }
    }

    private static String decodeStringResult(Function function, String result) {
        if (TextUtils.isEmpty(result)) {
            return "";
        }
        List<Type> decoded = FunctionReturnDecoder.decode(result, function.getOutputParameters());
        if (decoded.isEmpty() || decoded.get(0) == null) {
            return "";
        }
        Object value = decoded.get(0).getValue();
        return value != null ? value.toString().trim() : "";
    }

    private static String trimZeroBytes(byte[] bytes) {
        int end = bytes.length;
        while (end > 0 && bytes[end - 1] == 0) {
            end--;
        }
        if (end == 0) {
            return "";
        }
        return new String(bytes, 0, end, StandardCharsets.UTF_8).trim();
    }

    private static String ethCall(String contractAddress, String dataHex, String privateKey) throws Exception {
        String raw = TokenContractHelper.queryChainRead(contractAddress, dataHex, privateKey);
        return raw != null ? raw : "";
    }
}
