package com.example.brokerfi.swap;

import android.os.SystemClock;
import com.example.brokerfi.core.blockchain.model.CallReq;
import com.example.brokerfi.core.blockchain.model.SendETHTXReq;
import com.example.brokerfi.core.config.ChainConfig;
import com.example.brokerfi.core.security.SecurityUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.web3j.abi.datatypes.Address;
import org.web3j.crypto.Hash;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** Bounded synchronous calls on a background worker; same signed Dash wire protocol as the wallet.
 * No key/request logging, automatic write retries or remote quote server. Read-only calls retry once. */
public final class BrokerSwapClient {
    public static final BigInteger GAS_LIMIT = SwapFeePolicy.GAS_LIMIT;
    private static final Gson GSON = new Gson();
    private final OkHttpClient http = new OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS).callTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false).build();
    // Signed Dash operations may take several minutes; public RPC retains its original bounds.
    static final int DASH_OPERATION_TIMEOUT_SECONDS = 330;
    static final int SIGNED_READ_TIMEOUT_SECONDS = DASH_OPERATION_TIMEOUT_SECONDS,
            SIGNED_READ_CALL_TIMEOUT_SECONDS = DASH_OPERATION_TIMEOUT_SECONDS;
    private final OkHttpClient signedReads = http.newBuilder()
            .readTimeout(SIGNED_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(SIGNED_READ_CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS).build();
    private final OkHttpClient transactions=http.newBuilder()
            .readTimeout(DASH_OPERATION_TIMEOUT_SECONDS,TimeUnit.SECONDS)
            .callTimeout(DASH_OPERATION_TIMEOUT_SECONDS,TimeUnit.SECONDS)
            .retryOnConnectionFailure(false).build();
    private final BrokerSwapConfig cfg;
    private final TradeReadSession session;
    public BrokerSwapClient(BrokerSwapConfig config) { this(config,null); }
    BrokerSwapClient(BrokerSwapConfig config, TradeReadSession session) { cfg=config; this.session=session; }
    BrokerSwapConfig deployment() { return cfg; }
    private String post(String url, Object body) throws IOException {
        return post(url,body,http,false);
    }
    private String post(String url, Object body, OkHttpClient transport, boolean readOnly) throws IOException {
        if(readOnly && session!=null)session.starting();
        Request request = new Request.Builder().url(url).post(RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"), GSON.toJson(body))).build();
        try (Response response = transport.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new TradeReadFailure(TradeReadFailure.Kind.HTTP);
            if (response.body() == null) throw new TradeReadFailure(TradeReadFailure.Kind.INVALID);
            String raw=response.body().string();
            if(readOnly && session!=null)session.check();
            return raw;
        }
    }
    private String result(String raw) throws IOException {
        try {
            JsonObject object = JsonParser.parseString(raw).getAsJsonObject();
            if (object.has("error")) {
                boolean unknownAccount=object.get("error").isJsonPrimitive()
                        && "addr not exist".equals(object.get("error").getAsString());
                throw new TradeReadFailure(unknownAccount?TradeReadFailure.Kind.ACCOUNT:TradeReadFailure.Kind.REJECTED);
            }
            if (!object.has("result") || object.get("result").isJsonNull())
                throw new TradeReadFailure(TradeReadFailure.Kind.INVALID);
            return object.get("result").getAsString();
        } catch (RuntimeException e) { throw new TradeReadFailure(TradeReadFailure.Kind.INVALID); }
    }
    public String rpc(String method, Object... params) throws IOException {
        if (!isReadOnlyRpcMethod(method)) throw new IllegalArgumentException("Unsupported read RPC");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("jsonrpc", "2.0"); body.put("id", 1); body.put("method", method); body.put("params", Arrays.asList(params));
        IOException failure = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            try { return result(post(ChainConfig.CHAIN_JSON_RPC_URL, body, http, true)); }
            catch (TradeReadSession.Cancelled e) { throw e; }
            catch (IOException e) { failure = e; }
        }
        throw failure;
    }
    public String read(String key, String contract, String data) throws Exception {
        IOException failure = null;
        for (int attempt = 0; attempt < 2; attempt++) {
        if(session!=null)session.check();
        String uuid = UUID.randomUUID().toString();
        String[] signature = SecurityUtil.signECDSA(key, contract + data + "0x0" + uuid);
        CallReq request = new CallReq();
        request.setPublicKey(SecurityUtil.getPublicKeyFromPrivateKey(key)); request.setRandomStr(uuid);
        request.setTo(contract); request.setData(data); request.setValue("0x0");
        request.setSign1(signature[0]); request.setSign2(signature[1]);
        try { return requireReadHex(result(post(ChainConfig.getDashGatewayPostUrl("eth_call"), request, signedReads, true))); }
        catch (TradeReadSession.Cancelled e) { throw e; }
        catch (IOException e) { failure = e; }
        }
        throw failure;
    }
    static boolean isReadOnlyRpcMethod(String method) {
        return "eth_chainId".equals(method) || "eth_getCode".equals(method)
                || "eth_getBalance".equals(method) || "eth_gasPrice".equals(method);
    }
    static String requireReadHex(String value) throws IOException {
        if (value == null || !value.matches("0x([0-9a-fA-F]{64})+"))
            throw new TradeReadFailure(TradeReadFailure.Kind.INVALID);
        return value;
    }
    public void validate(String key) throws Exception {
        if (ChainConfig.useLocalBrokerChainNode()) throw new IOException("Swap deployment requires Dash chain");
        if (!SwapReceipt.quantity(rpc("eth_chainId")).equals(BigInteger.valueOf(cfg.chainId())))
            throw new IOException("Swap chain ID does not match");
        String code = rpc("eth_getCode", cfg.router(), "latest");
        if (!code.matches("0x([0-9a-fA-F]{2})+") || !Hash.sha3(code).equalsIgnoreCase(cfg.routerCodeHash()))
            throw new IOException("Swap Router code does not match");
        checkAddress(key, cfg.router(), "factory", cfg.factory());
        checkAddress(key, cfg.router(), "WBKC", cfg.wbkc());
        checkAddress(key, cfg.router(), "musdt", cfg.musdt());
        checkAddress(key, cfg.router(), "pair", cfg.pair());
        boolean musdt0 = cfg.musdt().compareTo(cfg.wbkc()) < 0;
        checkAddress(key, cfg.pair(), "token0", musdt0 ? cfg.musdt() : cfg.wbkc());
        checkAddress(key, cfg.pair(), "token1", musdt0 ? cfg.wbkc() : cfg.musdt());
        if (!BrokerSwapAbi.words(metadataRead(key, cfg.musdt(), "decimals"), 1)[0].equals(BigInteger.valueOf(6))
                || !BrokerSwapAbi.words(metadataRead(key, cfg.wbkc(), "decimals"), 1)[0].equals(BigInteger.valueOf(18)))
            throw new IOException("Swap token decimals do not match");
    }
    private void checkAddress(String key, String contract, String getter, String expected) throws Exception {
        if (!expected.equalsIgnoreCase(BrokerSwapAbi.address(metadataRead(key, contract, getter))))
            throw new IOException("Swap " + getter + " does not match");
    }
    private String metadataRead(String key,String contract,String getter) throws Exception {
        try { return read(key,contract,BrokerSwapAbi.call(getter)); }
        catch(TradeReadSession.Cancelled e) { throw e; }
        catch(IOException e) { throw new TradeReadFailure.Query(getter,e); }
    }
    /** Independently validates the atomic liquidity router; never uses the old full router. */
    public void validateLiquidity(String key) throws Exception {
        if (ChainConfig.useLocalBrokerChainNode() || !cfg.hasLiquidity()
                || !SwapReceipt.quantity(rpc("eth_chainId")).equals(BigInteger.valueOf(cfg.chainId())))
            throw new IOException("Liquidity network does not match");
        String code = rpc("eth_getCode", cfg.liquidityRouter(), "latest");
        if (!code.matches("0x([0-9a-fA-F]{2})+") || !Hash.sha3(code).equalsIgnoreCase(cfg.liquidityRouterCodeHash()))
            throw new IOException("Liquidity Router code does not match");
        checkAddress(key, cfg.liquidityRouter(), "factory", cfg.factory());
        checkAddress(key, cfg.liquidityRouter(), "WBKC", cfg.wbkc());
        checkAddress(key, cfg.liquidityRouter(), "musdt", cfg.musdt());
        checkAddress(key, cfg.liquidityRouter(), "pair", cfg.pair());
        boolean musdt0 = cfg.musdt().compareTo(cfg.wbkc()) < 0;
        checkAddress(key, cfg.pair(), "token0", musdt0 ? cfg.musdt() : cfg.wbkc());
        checkAddress(key, cfg.pair(), "token1", musdt0 ? cfg.wbkc() : cfg.musdt());
        for (String token : new String[]{cfg.musdt(), cfg.wbkc(), cfg.pair()})
            if (!BrokerSwapAbi.words(metadataRead(key, token, "decimals"), 1)[0]
                    .equals(BigInteger.valueOf(token.equals(cfg.musdt()) ? 6 : 18)))
                throw new IOException("Liquidity token decimals do not match");
    }
    public BigInteger totalSupply(String key) throws Exception {
        return BrokerSwapAbi.words(read(key, cfg.pair(), BrokerSwapAbi.call("totalSupply")), 1)[0];
    }
    public SwapPoolSnapshot pool(String key) throws Exception {
        BigInteger[] r = BrokerSwapAbi.words(read(key, cfg.pair(), BrokerSwapAbi.call("getReserves")), 3);
        if (r[0].bitLength() > 112 || r[1].bitLength() > 112 || r[2].bitLength() > 32)
            throw new IOException("Invalid pool reserves");
        boolean musdt0 = cfg.musdt().compareTo(cfg.wbkc()) < 0;
        return new SwapPoolSnapshot(r[musdt0 ? 0 : 1], r[musdt0 ? 1 : 0], SystemClock.elapsedRealtime());
    }
    public BigInteger balance(String key, String wallet, String contract) throws Exception {
        wallet = BrokerSwapConfig.walletAddress(wallet);
        return contract.isEmpty() ? SwapReceipt.quantity(rpc("eth_getBalance", wallet, "latest"))
                : BrokerSwapAbi.words(read(key, contract, BrokerSwapAbi.call("balanceOf", new Address(wallet))), 1)[0];
    }
    public BigInteger allowance(String key, String wallet, String token) throws Exception {
        return allowance(key, wallet, token, cfg.router());
    }
    public BigInteger allowance(String key, String wallet, String token, String spender) throws Exception {
        if (!cfg.router().equalsIgnoreCase(spender) && !(cfg.hasLiquidity() && cfg.liquidityRouter().equalsIgnoreCase(spender)))
            throw new IllegalArgumentException("Unverified allowance spender");
        wallet = BrokerSwapConfig.walletAddress(wallet);
        return BrokerSwapAbi.words(read(key, token, BrokerSwapAbi.call("allowance", new Address(wallet), new Address(spender))), 1)[0];
    }
    /** Conservative reserve, not a fee guarantee: Dash chooses the execution gas price. */
    public BigInteger gasReserve() throws Exception {
        return SwapFeePolicy.reserve(SwapReceipt.quantity(rpc("eth_gasPrice")));
    }
    String send(String key, String target, String data, BigInteger value, SwapSendDiagnostics diagnostics) throws Exception {
        diagnostics.stage("SIGNING");
        String uuid = diagnostics.record.sendRequestId, valueHex = "0x" + value.toString(16), gasHex = "0x" + GAS_LIMIT.toString(16);
        String[] signature = SecurityUtil.signECDSA(key, target + data + valueHex + gasHex + uuid);
        SendETHTXReq request = new SendETHTXReq();
        request.setPublicKey(SecurityUtil.getPublicKeyFromPrivateKey(key)); request.setRandomStr(uuid);
        request.setTo(target); request.setData(data); request.setValue(valueHex); request.setGas(gasHex);
        request.setSign1(signature[0]); request.setSign2(signature[1]);
        RequestBody body=RequestBody.create(MediaType.parse("application/json; charset=utf-8"),GSON.toJson(request));
        diagnostics.stage("REQUEST_READY");
        return diagnostics.post(transactions,ChainConfig.getDashGatewayPostUrl("eth_sendTransaction"),body);
    }
    public SwapReceipt receipt(String key, String hash, SwapAsset from, SwapAsset to, BigInteger input, String wallet) throws Exception {
        return SwapReceipt.parse(receiptRaw(key, hash), hash, cfg, from, to, input, wallet);
    }
    public String receiptRaw(String key, String hash) throws Exception {
        if (!SwapReceipt.validHash(hash)) throw new IllegalArgumentException("Invalid receipt hash");
        String uuid = UUID.randomUUID().toString(), bodyHash = hash.substring(2);
        String[] sig = SecurityUtil.signECDSA(key, uuid + bodyHash);
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("uuid", bodyHash); request.put("PublicKey", SecurityUtil.getPublicKeyFromPrivateKey(key));
        request.put("RandomStr", uuid); request.put("Sign1", sig[0]); request.put("Sign2", sig[1]);
        return post(ChainConfig.getDashGatewayPostUrl("eth_getTransactionReceipt"), request,transactions,false);
    }
}
