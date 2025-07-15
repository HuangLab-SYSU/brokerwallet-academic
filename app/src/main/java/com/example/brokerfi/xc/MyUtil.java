package com.example.brokerfi.xc;


import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

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

                // 将整数转换为十六进制字符串
                String hexString = Integer.toHexString(decimalNumber);
                String gas = "0x"+hexString;
                String uuid = UUID.randomUUID().toString();
                SendETHTXReq req = new SendETHTXReq();
                String thedata = Holder.contractaddr + data + "0x0" + gas + uuid;
                String[] sign = SecurityUtil.signECDSA(privateKey, thedata);

                req.setPublicKey(SecurityUtil.getPublicKeyFromPrivateKey(privateKey));
                req.setData(data);
                req.setRandomStr(uuid);
                req.setTo(Holder.contractaddr);
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
                String thedata = Holder.contractaddr + data + "0x0" + gas + uuid;
                String[] sign = SecurityUtil.signECDSA(privateKey, thedata);

                req.setPublicKey(SecurityUtil.getPublicKeyFromPrivateKey(privateKey));
                req.setData(data);
                req.setRandomStr(uuid);
                req.setTo(Holder.contractaddr);
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
                String thedata = Holder.contractaddr + data + finalValue + gas + uuid;
                String[] sign = SecurityUtil.signECDSA(privateKey, thedata);

                req.setPublicKey(SecurityUtil.getPublicKeyFromPrivateKey(privateKey));
                req.setData(data);
                req.setRandomStr(uuid);
                req.setTo(Holder.contractaddr);
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
            String thedata = Holder.contractaddr + data + "0x0" + uuid;
            String[] sign = SecurityUtil.signECDSA(privateKey, thedata);

            req.setPublicKey(SecurityUtil.getPublicKeyFromPrivateKey(privateKey));
            req.setData(data);
            req.setRandomStr(uuid);
            req.setTo(Holder.contractaddr);
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
            req.setPublicKey(SecurityUtil.getPublicKeyFromPrivateKey(privateKey)); // 注意：这里的 global.PublicKey 需要根据你的实际情况进行替换
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

            String data = uuid + SecurityUtil.GetAddress(privateKey);
            String[] sign = SecurityUtil.signECDSA(privateKey, data);
            QueryReq queryReq = new QueryReq();
            queryReq.setPublicKey(SecurityUtil.getPublicKeyFromPrivateKey(privateKey)); // 注意：这里的 global.PublicKey 需要根据你的实际情况进行替换
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

    public static void Getreward(String privateKey) {
        try {
            String uuid = UUID.randomUUID().toString();

            String data = uuid ;
            String[] sign = SecurityUtil.signECDSA(privateKey, data);
            RewardReq rewardReq = new RewardReq();
            rewardReq.setPublicKey(SecurityUtil.getPublicKeyFromPrivateKey(privateKey)); // 注意：这里的 global.PublicKey 需要根据你的实际情况进行替换
            rewardReq.setRandomStr(uuid);
            rewardReq.setSign1(sign[0]);
            rewardReq.setSign2(sign[1]);

            try {
                HTTPUtil.doPost("reward_wallet", rewardReq);
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

    public static String SendTX(String privateKey, String to, String value, String fee) {
        String uuid = UUID.randomUUID().toString();
        String data = "";
        if (fee != null && !fee.isEmpty()) {
            data = uuid + to + value + fee;
        } else {
            data = uuid + to + value;
        }

        String[] sign = SecurityUtil.signECDSA(privateKey, data);
        TxReq req = new TxReq();
        req.setPublicKey(SecurityUtil.getPublicKeyFromPrivateKey(privateKey));
        req.setRandomStr(uuid);
        req.setTo(to);
        req.setValue(value);
        req.setSign1(sign[0]);
        req.setSign2(sign[1]);
        if (fee != null && !fee.isEmpty()) {
            req.setFee(fee);
        }
        try {
            byte[] bytes = HTTPUtil.doPost("sendtx", req);
            return new String(bytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
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

class CallReq {
    @SerializedName("PublicKey")
    private String PublicKey;
    @SerializedName("RandomStr")
    private String RandomStr;
    @SerializedName("To")
    private String To;
    @SerializedName("data")
    private String Data;
    @SerializedName("value")
    private String Value;
    //    private String Gas;
    @SerializedName("Sign1")
    private String Sign1;
    @SerializedName("Sign2")
    private String Sign2;

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

    public String getData() {
        return Data;
    }

    public void setData(String data) {
        this.Data = data;
    }

    public String getValue() {
        return Value;
    }

    public void setValue(String value) {
        this.Value = value;
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

class SendETHTXReq {
    private String PublicKey;
    private String RandomStr;
    private String To;
    @SerializedName("data")
    private String Data;
    @SerializedName("value")
    private String Value;
    private String Gas;
    private String Sign1;
    private String Sign2;

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

    public String getData() {
        return Data;
    }

    public void setData(String data) {
        this.Data = data;
    }

    public String getValue() {
        return Value;
    }

    public void setValue(String value) {
        this.Value = value;
    }

    public String getGas() {
        return Gas;
    }

    public void setGas(String gas) {
        Gas = gas;
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

class ReturnAccountState {
    @SerializedName("account")
    private String AccountAddr;
    @SerializedName("balance")
    private String Balance;

    public String getAccountAddr() {
        return AccountAddr;
    }

    public void setAccountAddr(String accountAddr) {
        AccountAddr = accountAddr;
    }

    public String getBalance() {
        return Balance;
    }

    public void setBalance(String balance) {
        Balance = balance;
    }
}
