package com.example.brokerfi.xc;

import android.util.Log;

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

import java.math.BigInteger;

public class Web3jTransferUtil {

    private static final String RPC_URL = "http://dash.broker-chain.com:42515";

    /**
     * 基于Web3j的原生BKC转账
     * @param privateKey 转账人私钥
     * @param toAddress 接收人地址
     * @param amountEther 转账单位（BKC
     * @return 交易哈希
     */
    public static String sendTransaction(String privateKey, String toAddress, String amountEther) {
        try {
            Web3j web3j = Web3j.build(new HttpService(RPC_URL));

            Credentials credentials = Credentials.create(privateKey);

            // 获取 nonce
            EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(
                    credentials.getAddress(),
                    DefaultBlockParameterName.LATEST
            ).send();

            BigInteger nonce = ethGetTransactionCount.getTransactionCount();

            Log.d("Web3jTransferUtil", "Nonce: " + nonce);

            // 转账金额（单位：wei）
            BigInteger value = Convert.toWei(amountEther, Convert.Unit.ETHER).toBigInteger();

            // gas 参数（后续优化）
            BigInteger gasPrice = Convert.toWei("20", Convert.Unit.GWEI).toBigInteger();
            BigInteger gasLimit = BigInteger.valueOf(21000);

            // 构造交易
            RawTransaction rawTransaction = RawTransaction.createEtherTransaction(
                    nonce,
                    gasPrice,
                    gasLimit,
                    toAddress,
                    value
            );

            // 签名交易
            byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);

            String hexValue = Numeric.toHexString(signedMessage);

            // 发送交易
            EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexValue).send();

            if (ethSendTransaction.hasError()) {
                Log.d("Web3jTransferUtil", "转账失败: " + ethSendTransaction.getError().getMessage());
                return "error: " + ethSendTransaction.getError().getMessage();
            }

            // txHash
            return ethSendTransaction.getTransactionHash();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
