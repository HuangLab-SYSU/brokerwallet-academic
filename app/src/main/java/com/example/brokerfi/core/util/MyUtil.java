package com.example.brokerfi.core.util;


import com.example.brokerfi.token.TokenConfig;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import com.example.brokerfi.core.blockchain.model.CallReq;
import com.example.brokerfi.core.blockchain.model.SendETHTXReq;
import com.example.brokerfi.core.config.ChainConfig;
import com.example.brokerfi.core.model.ReturnAccountState;
import com.example.brokerfi.core.network.HTTPUtil;
import com.example.brokerfi.core.security.SecurityUtil;


public class MyUtil {
    static ExecutorService service = Executors.newCachedThreadPool();
    public static String getTransactionReceipt(String hash, String privateKey) {
        AtomicReference<String> reference = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        service.execute(() -> {
            try {
                String uuid = UUID.randomUUID().toString();
                String thedata = uuid + hash;
                String[] sign = SecurityUtil.signECDSA(privateKey, thedata);
                GetTransactionReceiptReq req = new GetTransactionReceiptReq();
                req.setPublicKey(SecurityUtil.getPublicKeyFromPrivateKey(privateKey));
                req.setUUID(hash);
                req.setRandomStr(uuid);
                req.setSign1(sign[0]);
                req.setSign2(sign[1]);
                byte[] bytes = HTTPUtil.doPost("eth_getTransactionReceipt", req);
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
            throw new RuntimeException(e);
        }
        return reference.get();
    }
    public static String sendethtx2(String data, String privateKey,String gas1) {
        AtomicReference<String> reference = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        service.execute(() -> {
            try {
                int decimalNumber = Integer.parseInt(gas1);

                // Convert integer to hexadecimal string
                String hexString = Integer.toHexString(decimalNumber);
                String gas = "0x"+hexString;
                String uuid = UUID.randomUUID().toString();
                SendETHTXReq req = new SendETHTXReq();
                String thedata = ChainConfig.MAIN_CONTRACT_ADDRESS + data + "0x0" + gas + uuid;
                String[] sign = SecurityUtil.signECDSA(privateKey, thedata);

                req.setPublicKey(SecurityUtil.getPublicKeyFromPrivateKey(privateKey));
                req.setData(data);
                req.setRandomStr(uuid);
                req.setTo(ChainConfig.MAIN_CONTRACT_ADDRESS);
                req.setValue("0x0");
                req.setSign1(sign[0]);
                req.setSign2(sign[1]);
                req.setGas(gas);
                byte[] bytes = HTTPUtil.doPost("eth_sendTransaction", req);
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
            throw new RuntimeException(e);
        }
        return reference.get();
    }
    public static String sendethtx(String data, String privateKey) {
        AtomicReference<String> reference = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        service.execute(() -> {
            try {
                String gas = "0xf4240";
                String uuid = UUID.randomUUID().toString();
                SendETHTXReq req = new SendETHTXReq();
                String thedata = ChainConfig.MAIN_CONTRACT_ADDRESS + data + "0x0" + gas + uuid;
                String[] sign = SecurityUtil.signECDSA(privateKey, thedata);

                req.setPublicKey(SecurityUtil.getPublicKeyFromPrivateKey(privateKey));
                req.setData(data);
                req.setRandomStr(uuid);
                req.setTo(ChainConfig.MAIN_CONTRACT_ADDRESS);
                req.setValue("0x0");
                req.setSign1(sign[0]);
                req.setSign2(sign[1]);
                req.setGas(gas);
                byte[] bytes = HTTPUtil.doPost("eth_sendTransaction", req);
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
            throw new RuntimeException(e);
        }
        return reference.get();
    }
    public static String sendethtx(String data, String privateKey,String value) {
        AtomicReference<String> reference = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        value = "0x"+value;
        String finalValue = value;
        service.execute(() -> {
            try {
                String gas = "0xf4240";
                String uuid = UUID.randomUUID().toString();
                SendETHTXReq req = new SendETHTXReq();
                String thedata = ChainConfig.MAIN_CONTRACT_ADDRESS + data + finalValue + gas + uuid;
                String[] sign = SecurityUtil.signECDSA(privateKey, thedata);

                req.setPublicKey(SecurityUtil.getPublicKeyFromPrivateKey(privateKey));
                req.setData(data);
                req.setRandomStr(uuid);
                req.setTo(ChainConfig.MAIN_CONTRACT_ADDRESS);
                req.setValue(finalValue);
                req.setSign1(sign[0]);
                req.setSign2(sign[1]);
                req.setGas(gas);
                byte[] bytes = HTTPUtil.doPost("eth_sendTransaction", req);
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
            throw new RuntimeException(e);
        }
        return reference.get();
    }
    public static String sendethcall(String data, String privateKey) {
        AtomicReference<String> reference = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        service.execute(() -> {
        try {
            String uuid = UUID.randomUUID().toString();
            CallReq req = new CallReq();
            String thedata = ChainConfig.MAIN_CONTRACT_ADDRESS + data + "0x0" + uuid;
            String[] sign = SecurityUtil.signECDSA(privateKey, thedata);

            req.setPublicKey(SecurityUtil.getPublicKeyFromPrivateKey(privateKey));
            req.setData(data);
            req.setRandomStr(uuid);
            req.setTo(ChainConfig.MAIN_CONTRACT_ADDRESS);
            req.setValue("0x0");
            req.setSign1(sign[0]);
            req.setSign2(sign[1]);
            byte[] bytes = HTTPUtil.doPost("eth_call", req);
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
            throw new RuntimeException(e);
        }
        return reference.get();
    }

    public static String withdraw(String privateKey) {
        try {
            WithdrawBrokerReq req = new WithdrawBrokerReq();
            String uuid = UUID.randomUUID().toString();
            String data = uuid;
            String[] sign = SecurityUtil.signECDSA(privateKey, data);
            req.setPublicKey(SecurityUtil.getPublicKeyFromPrivateKey(privateKey));
            req.setRandomStr(uuid);
            req.setSign1(sign[0]);
            req.setSign2(sign[1]);
            byte[] bytes = HTTPUtil.doPost("withdrawbroker", req);
            return new String(bytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String stake(String privateKey, String value) {
        try {
            StakeReq req = new StakeReq();
            String uuid = UUID.randomUUID().toString();
            String data = uuid + value;
            String[] sign = SecurityUtil.signECDSA(privateKey, data);
            req.setPublicKey(SecurityUtil.getPublicKeyFromPrivateKey(privateKey));
            req.setRandomStr(uuid);
            req.setSign1(sign[0]);
            req.setSign2(sign[1]);
            req.setValue(value);
            byte[] bytes = HTTPUtil.doPost("stake", req);
            return new String(bytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String querybrokerprofit(String privateKey) {
        try {
            ApplyBrokerReq req = new ApplyBrokerReq();
            String uuid = UUID.randomUUID().toString();
            String data = uuid;
            String[] sign = SecurityUtil.signECDSA(privateKey, data);
            req.setPublicKey(SecurityUtil.getPublicKeyFromPrivateKey(privateKey));
            req.setRandomStr(uuid);
            req.setSign1(sign[0]);
            req.setSign2(sign[1]);
            byte[] bytes = HTTPUtil.doPost("querybrokerprofit", req);
            return new String(bytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String applybroker(String privateKey) {
        try {
            ApplyBrokerReq req = new ApplyBrokerReq();
            String uuid = UUID.randomUUID().toString();
            String data = uuid;
            String[] sign = SecurityUtil.signECDSA(privateKey, data);
            req.setPublicKey(SecurityUtil.getPublicKeyFromPrivateKey(privateKey));
            req.setRandomStr(uuid);
            req.setSign1(sign[0]);
            req.setSign2(sign[1]);
            byte[] bytes = HTTPUtil.doPost("applybroker", req);
            return new String(bytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public static String queryisbroker(String privateKey) {
        try {
            QueryIsBrokerReq req = new QueryIsBrokerReq();
            String uuid = UUID.randomUUID().toString();
            String data = uuid;
            String[] sign = SecurityUtil.signECDSA(privateKey, data);
            req.setPublicKey(SecurityUtil.getPublicKeyFromPrivateKey(privateKey)); // Note: global.PublicKey here needs to be replaced according to your actual situation.
            req.setRandomStr(uuid);
            req.setSign1(sign[0]);
            req.setSign2(sign[1]);
            byte[] bytes = HTTPUtil.doPost("queryisbroker", req);
            return new String(bytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static ReturnAccountState[] GetAddrAndBalance2(String[] addrs) {
        try {

            QueryAccReq req = new QueryAccReq();
            req.setAccounts(addrs);

            try {
                byte[] bytes = HTTPUtil.doPost("query-g10", req);
                Gson gson = new Gson();
                ReturnAccountState[] returnAccountState = gson.fromJson(new String(bytes), ReturnAccountState[].class);
                return returnAccountState;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static ReturnAccountState GetAddrAndBalance(String privateKey) {
        try {
            String uuid = UUID.randomUUID().toString();
            // UUID is a randomly generated ID, string type;
            // Data is uuid+address; this address is obtained through the private key;
            String data = uuid + SecurityUtil.GetAddress(privateKey);
            //String data = uuid;
            String[] sign = SecurityUtil.signECDSA(privateKey, data);
            // Set the properties of the req parameter of this query-g: public key+RandomStr+Sign1+Sign2+UUID.
            QueryReq queryReq = new QueryReq();
            queryReq.setPublicKey(SecurityUtil.getPublicKeyFromPrivateKey(privateKey)); // Note: global.PublicKey here needs to be replaced according to your actual situation.
            queryReq.setRandomStr(uuid);
            queryReq.setSign1(sign[0]);
            queryReq.setSign2(sign[1]);
            queryReq.setUUID(SecurityUtil.GetAddress(privateKey));
            try {
                byte[] bytes = HTTPUtil.doPost("query-g", queryReq);
                Gson gson = new Gson();
                ReturnAccountState returnAccountState = gson.fromJson(new String(bytes), ReturnAccountState.class);
                if (returnAccountState != null) {
                    BigDecimal a = new BigDecimal(returnAccountState.getBalance());
                    BigDecimal b = new BigDecimal("1000000000000000000");
                    BigDecimal divide = a.divide(b);
                    returnAccountState.setBalance(divide.toString());
                }
                return returnAccountState;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getNativeBalanceForPrivateKey(String privateKey) {
        ReturnAccountState state = GetAddrAndBalance(privateKey);
        if (state == null || state.getBalance() == null || state.getBalance().trim().isEmpty()) {
            return "0";
        }
        return state.getBalance().trim();
    }

    public static void Getreward(String privateKey) {
        try {
            String uuid = UUID.randomUUID().toString();

            String data = uuid ;
            String[] sign = SecurityUtil.signECDSA(privateKey, data);
            RewardReq rewardReq = new RewardReq();
            rewardReq.setPublicKey(SecurityUtil.getPublicKeyFromPrivateKey(privateKey)); // Note: global.PublicKey here needs to be replaced according to your actual situation.
            rewardReq.setRandomStr(uuid);
            rewardReq.setSign1(sign[0]);
            rewardReq.setSign2(sign[1]);

            try {
                //HTTPUtil.doPost("reward_wallet", rewardReq);
                return ;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return ;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ;
    }

    public static String SendTX(String privateKey, String to, String value, String fee, String gasLimitStr) {
        // Old transaction sending method
        // String uuid = UUID.randomUUID().toString();
        // String data = "";
        // if (fee != null && !fee.isEmpty()) {
        //     data = uuid + to + value + fee;
        // } else {
        //     data = uuid + to + value;
        // }
        // String[] sign = SecurityUtil.signECDSA(privateKey, data);
        // TxReq req = new TxReq();
        // req.setPublicKey(SecurityUtil.getPublicKeyFromPrivateKey(privateKey));
        // req.setRandomStr(uuid);
        // req.setTo(to);
        // req.setValue(value);
        // req.setSign1(sign[0]);
        // req.setSign2(sign[1]);
        // if (fee != null && !fee.isEmpty()) {
        //     req.setFee(fee);
        // }
        // try {
        //     byte[] bytes = HTTPUtil.doPost("sendtx", req);
        //     return new String(bytes);
        // } catch (Exception e) {
        //     e.printStackTrace();
        // }
        // return null;

        // Standard eth_sendRawTransaction: sign locally, broadcast raw tx via JSON-RPC.
        try {
            Web3j web3j = Web3j.build(new HttpService(TokenConfig.getLocalChainRpcUrl()));
            Credentials credentials = Credentials.create(privateKey);

            // Recipient address: ensure 0x prefix for web3j
            String toAddress = to;
            if (!toAddress.startsWith("0x") && !toAddress.startsWith("0X")) {
                toAddress = "0x" + toAddress;
            }

            // Fetch nonce from chain
            EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(
                    credentials.getAddress(),
                    DefaultBlockParameterName.LATEST
            ).send();
            BigInteger nonce = ethGetTransactionCount.getTransactionCount();

            // Amount: BKC (ether) -> wei
            BigInteger amountWei = Convert.toWei(value, Convert.Unit.ETHER).toBigInteger();

            // Fee: interpret as gas price in gwei; default 20 gwei when not provided
            String gweiStr = (fee == null || fee.isEmpty()) ? "20" : fee;
            BigInteger gasPrice = Convert.toWei(gweiStr, Convert.Unit.GWEI).toBigInteger();

            // Gas Limit: default 21000, use provided value if given
            long gasLimitVal = 21000;
            if (gasLimitStr != null && !gasLimitStr.isEmpty()) {
                try {
                    gasLimitVal = Long.parseLong(gasLimitStr);
                } catch (NumberFormatException ignored) {}
            }
            BigInteger gasLimit = BigInteger.valueOf(gasLimitVal);

            RawTransaction rawTransaction = RawTransaction.createEtherTransaction(
                    nonce, gasPrice, gasLimit, toAddress, amountWei);

            byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
            String hexValue = Numeric.toHexString(signedMessage);

            EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexValue).send();
            if (ethSendTransaction.hasError()) {
                return "error: " + ethSendTransaction.getError().getMessage();
            }
            return "success:" + ethSendTransaction.getTransactionHash();
        } catch (Exception e) {
            e.printStackTrace();
            return "error: " + e.getMessage();
        }
    }

    public static String claim(String privateKey) {
        AtomicReference<String> reference = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        service.execute(()->{
            try {
                ClaimReq req = new ClaimReq();
                String uuid = UUID.randomUUID().toString();
                String data = uuid;
                String[] sign = SecurityUtil.signECDSA(privateKey, data);
                req.setPublicKey(SecurityUtil.getPublicKeyFromPrivateKey(privateKey));
                req.setRandomStr(uuid);
                req.setSign1(sign[0]);
                req.setSign2(sign[1]);
                byte[] bytes = HTTPUtil.doPost("claim", req);
               reference.set( new String(bytes));
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
            throw new RuntimeException(e);
        }
        return reference.get();
    }
}

class GetTransactionReceiptReq {
    @SerializedName("uuid")
    private String UUID;
    @SerializedName("PublicKey")
    private String PublicKey;
    @SerializedName("RandomStr")
    private String RandomStr;
    @SerializedName("Sign1")
    private String Sign1;
    @SerializedName("Sign2")
    private String Sign2;

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }

    public String getPublicKey() {
        return PublicKey;
    }

    public void setPublicKey(String publicKey) {
        PublicKey = publicKey;
    }

    public String getRandomStr() {
        return RandomStr;
    }

    public void setRandomStr(String randomStr) {
        RandomStr = randomStr;
    }

    public String getSign1() {
        return Sign1;
    }

    public void setSign1(String sign1) {
        Sign1 = sign1;
    }

    public String getSign2() {
        return Sign2;
    }

    public void setSign2(String sign2) {
        Sign2 = sign2;
    }
}

class ClaimReq {
    private String PublicKey;
    private String RandomStr;
    private String Sign1;
    private String Sign2;


    // Getters and Setters
    public String getPublicKey() {
        return PublicKey;
    }

    public void setPublicKey(String publicKey) {
        PublicKey = publicKey;
    }

    public String getRandomStr() {
        return RandomStr;
    }

    public void setRandomStr(String randomStr) {
        RandomStr = randomStr;
    }

    public String getSign1() {
        return Sign1;
    }

    public void setSign1(String sign1) {
        Sign1 = sign1;
    }

    public String getSign2() {
        return Sign2;
    }

    public void setSign2(String sign2) {
        Sign2 = sign2;
    }


}
class WithdrawBrokerReq {
    private String PublicKey;
    private String RandomStr;
    private String Sign1;
    private String Sign2;


    // Getters and Setters
    public String getPublicKey() {
        return PublicKey;
    }

    public void setPublicKey(String publicKey) {
        PublicKey = publicKey;
    }

    public String getRandomStr() {
        return RandomStr;
    }

    public void setRandomStr(String randomStr) {
        RandomStr = randomStr;
    }

    public String getSign1() {
        return Sign1;
    }

    public void setSign1(String sign1) {
        Sign1 = sign1;
    }

    public String getSign2() {
        return Sign2;
    }

    public void setSign2(String sign2) {
        Sign2 = sign2;
    }


}

class StakeReq {
    private String PublicKey;
    private String RandomStr;
    private String Sign1;
    private String Sign2;
    private String Value;

    // Getters and Setters
    public String getPublicKey() {
        return PublicKey;
    }

    public void setPublicKey(String publicKey) {
        PublicKey = publicKey;
    }

    public String getRandomStr() {
        return RandomStr;
    }

    public void setRandomStr(String randomStr) {
        RandomStr = randomStr;
    }

    public String getSign1() {
        return Sign1;
    }

    public void setSign1(String sign1) {
        Sign1 = sign1;
    }

    public String getSign2() {
        return Sign2;
    }

    public void setSign2(String sign2) {
        Sign2 = sign2;
    }

    public String getValue() {
        return Value;
    }

    public void setValue(String value) {
        Value = value;
    }
}

class ApplyBrokerReq {
    private String PublicKey;
    private String RandomStr;
    private String Sign1;
    private String Sign2;


    // Getters and Setters
    public String getPublicKey() {
        return PublicKey;
    }

    public void setPublicKey(String publicKey) {
        PublicKey = publicKey;
    }

    public String getRandomStr() {
        return RandomStr;
    }

    public void setRandomStr(String randomStr) {
        RandomStr = randomStr;
    }

    public String getSign1() {
        return Sign1;
    }

    public void setSign1(String sign1) {
        Sign1 = sign1;
    }

    public String getSign2() {
        return Sign2;
    }

    public void setSign2(String sign2) {
        Sign2 = sign2;
    }


}

class QueryAccReq {
    private String[] accounts;

    public String[] getAccounts() {
        return accounts;
    }

    public void setAccounts(String[] accounts) {
        this.accounts = accounts;
    }
}

class QueryIsBrokerReq {
    private String PublicKey;
    private String RandomStr;
    private String Sign1;
    private String Sign2;


    // Getters and Setters
    public String getPublicKey() {
        return PublicKey;
    }

    public void setPublicKey(String publicKey) {
        PublicKey = publicKey;
    }

    public String getRandomStr() {
        return RandomStr;
    }

    public void setRandomStr(String randomStr) {
        RandomStr = randomStr;
    }

    public String getSign1() {
        return Sign1;
    }

    public void setSign1(String sign1) {
        Sign1 = sign1;
    }

    public String getSign2() {
        return Sign2;
    }

    public void setSign2(String sign2) {
        Sign2 = sign2;
    }


}
class RewardReq {
    private String PublicKey;
    private String RandomStr;
    private String Sign1;
    private String Sign2;


    // Getters and Setters
    public String getPublicKey() {
        return PublicKey;
    }

    public void setPublicKey(String publicKey) {
        PublicKey = publicKey;
    }

    public String getRandomStr() {
        return RandomStr;
    }

    public void setRandomStr(String randomStr) {
        RandomStr = randomStr;
    }

    public String getSign1() {
        return Sign1;
    }

    public void setSign1(String sign1) {
        Sign1 = sign1;
    }

    public String getSign2() {
        return Sign2;
    }

    public void setSign2(String sign2) {
        Sign2 = sign2;
    }

}
class QueryReq {
    private String PublicKey;
    private String RandomStr;
    private String Sign1;
    private String Sign2;
    private String UUID;

    // Getters and Setters
    public String getPublicKey() {
        return PublicKey;
    }

    public void setPublicKey(String publicKey) {
        PublicKey = publicKey;
    }

    public String getRandomStr() {
        return RandomStr;
    }

    public void setRandomStr(String randomStr) {
        RandomStr = randomStr;
    }

    public String getSign1() {
        return Sign1;
    }

    public void setSign1(String sign1) {
        Sign1 = sign1;
    }

    public String getSign2() {
        return Sign2;
    }

    public void setSign2(String sign2) {
        Sign2 = sign2;
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }
}

class TxReq {
    private String PublicKey;
    private String RandomStr;
    private String To;
    private String Value;
    private String Sign1;
    private String Sign2;
    private String Fee;

    public String getFee() {
        return Fee;
    }

    public void setFee(String fee) {
        Fee = fee;
    }

    public String getPublicKey() {
        return PublicKey;
    }

    public void setPublicKey(String publicKey) {
        PublicKey = publicKey;
    }

    public String getRandomStr() {
        return RandomStr;
    }

    public void setRandomStr(String randomStr) {
        RandomStr = randomStr;
    }

    public String getTo() {
        return To;
    }

    public void setTo(String to) {
        To = to;
    }

    public String getValue() {
        return Value;
    }

    public void setValue(String value) {
        Value = value;
    }

    public String getSign1() {
        return Sign1;
    }

    public void setSign1(String sign1) {
        Sign1 = sign1;
    }

    public String getSign2() {
        return Sign2;
    }

    public void setSign2(String sign2) {
        Sign2 = sign2;
    }
}
