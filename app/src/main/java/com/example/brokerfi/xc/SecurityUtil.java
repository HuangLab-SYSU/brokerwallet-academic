package com.example.brokerfi.xc;

import java.math.BigInteger;

import java.security.SecureRandom;
import java.security.Security;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.ECKeyPairGenerator;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECKeyGenerationParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;


import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;


import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.encoders.Hex;

public class SecurityUtil {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static String GetAddress(String privateKey) {
        try {
            String publicKey = getPublicKeyFromPrivateKey(privateKey);
            byte[] decode = Hex.decode(publicKey);
            SHA256Digest digest = new SHA256Digest();
            digest.update(decode, 0, decode.length);
            byte[] hash = new byte[digest.getDigestSize()];
            digest.doFinal(hash, 0);
            byte[] hash2 = new byte[20];
            for (int i = 0; i < 20; i++) {
                hash2[i] = hash[i];
            }
            return Hex.toHexString(hash2);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }


    public static String[] signECDSA(String privateKey1, String data)  {
        BigInteger privateKey = new BigInteger(privateKey1,10);
        //Bouncy Castle 库中的 ECNamedCurveTable 方法 getParameterSpec 方法
        //升级为 secp256k1
        ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256r1");
        ECDomainParameters domainParameters = new ECDomainParameters( spec.getCurve(),spec.getG(), spec.getN());
        ECDSASigner signer = new ECDSASigner();
        signer.init(true, new ECPrivateKeyParameters(privateKey, domainParameters));
        SHA256Digest digest = new SHA256Digest();
        byte[] dataBytes = data.getBytes();
        digest.update(dataBytes, 0, dataBytes.length);
        byte[] hash = new byte[digest.getDigestSize()];
        digest.doFinal(hash, 0);
        BigInteger[] rs = signer.generateSignature(hash);
        return new String[] {
                Hex.toHexString(rs[0].toByteArray()),
                Hex.toHexString(rs[1].toByteArray())
        };
    }

    public static String getPublicKeyFromPrivateKey(String p)  {
        BigInteger privateKey = new BigInteger(p);
        ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256r1");
        ECPoint publicPoint = spec.getG().multiply(privateKey);
        byte[] encoded = publicPoint.getEncoded(true);
        return Hex.toHexString(encoded);
    }

    public static String generatePrivateKey() {
        try {
            // 获取 secp256r1 曲线的参数
            ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256r1");
            ECDomainParameters domainParams = new ECDomainParameters(
                    spec.getCurve(), spec.getG(), spec.getN(), spec.getH());

            // 使用安全随机数生成器
            SecureRandom random = new SecureRandom();

            // 创建密钥对生成器
            ECKeyPairGenerator generator = new ECKeyPairGenerator();
            ECKeyGenerationParameters params = new ECKeyGenerationParameters(domainParams, random);
            generator.init(params);

            // 生成密钥对
            AsymmetricCipherKeyPair keyPair = generator.generateKeyPair();
            ECPrivateKeyParameters privateKeyParams = (ECPrivateKeyParameters) keyPair.getPrivate();

            // 获取私钥的BigInteger值
            BigInteger privateKey = privateKeyParams.getD();

            // 返回私钥的十六进制字符串表示
            return privateKey.toString(10);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) {
        System.out.println(generatePrivateKey());
    }
}
