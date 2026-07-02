package com.example.brokerfi.core.blockchain;

import android.util.Log;

import com.example.brokerfi.core.config.ChainConfig;

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

    /**
     * Native BKC transfer based on Web3j / 基于Web3j的原生BKC转账
     * @param privateKey Transferor’s private key / 转账人私钥
     * @param toAddress Recipient address / 接收人地址
     * @param amountEther Transfer unit (BKC) / 转账单位（BKC
     * @return transaction hash / 交易哈希
     */
    public static String sendTransaction(String privateKey, String toAddress, String amountEther) {
        try {
            Web3j web3j = Web3j.build(new HttpService(ChainConfig.CHAIN_JSON_RPC_URL));

            Credentials credentials = Credentials.create(privateKey);

            // Get nonce
            EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(
                    credentials.getAddress(),
                    DefaultBlockParameterName.LATEST
            ).send();

            BigInteger nonce = ethGetTransactionCount.getTransactionCount();

            Log.d("Web3jTransferUtil", "Nonce: " + nonce);

            // Transfer amount (unit: wei)
            BigInteger value = Convert.toWei(amountEther, Convert.Unit.ETHER).toBigInteger();

            // gas parameter (subsequent optimization)
            BigInteger gasPrice = Convert.toWei("20", Convert.Unit.GWEI).toBigInteger();
            BigInteger gasLimit = BigInteger.valueOf(21000);

            // Build the transaction
            RawTransaction rawTransaction = RawTransaction.createEtherTransaction(
                    nonce,
                    gasPrice,
                    gasLimit,
                    toAddress,
                    value
            );

            // Sign the transaction
            byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);

            String hexValue = Numeric.toHexString(signedMessage);

            // Send the transaction
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
