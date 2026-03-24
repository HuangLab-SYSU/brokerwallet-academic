package com.example.brokerfi.xc;

import java.math.BigInteger;

import java.security.SecureRandom;
import java.security.Security;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.digests.KeccakDigest;
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

    /* 旧的地址生成方式（SHA-256哈希公钥取前20字节）
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
    */

    // 新的地址生成方式，与以太坊标准和服务器端一致：Keccak-256(公钥)的后20字节
    public static String GetAddress(String privateKey) {
        try {
            String publicKey = getPublicKeyFromPrivateKey(privateKey);
            byte[] decode = Hex.decode(publicKey);
            
            // 用Keccak-256对公钥进行哈希，注意！！忽略前缀字节(0x04)
            KeccakDigest keccakDigest = new KeccakDigest(256);

            // 这里按照以太坊的标准，也就是 go supervisor 端使用的 crypto 方法，跳过第一个字节（前缀），只使用X和Y坐标部分
            keccakDigest.update(decode, 1, decode.length - 1);
            byte[] keccakHash = new byte[keccakDigest.getDigestSize()];
            keccakDigest.doFinal(keccakHash, 0);
            
            // 取Keccak-256哈希结果的后20字节作为地址
            byte[] addressBytes = new byte[20];
            System.arraycopy(keccakHash, keccakHash.length - 20, addressBytes, 0, 20);
            
            // 将地址字节转换为十六进制字符串
            return Hex.toHexString(addressBytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "NONSTANDARD_PRIVATE_KEY";
    }


    // 256r1相关代码
//    public static String[] signECDSA(String privateKey1, String data)  {
//        BigInteger privateKey = new BigInteger(privateKey1,10);
//        //Bouncy Castle 库中的 ECNamedCurveTable 方法 getParameterSpec 方法
//        //升级为 secp256k1
//        ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256r1");
//        ECDomainParameters domainParameters = new ECDomainParameters( spec.getCurve(),spec.getG(), spec.getN());
//        ECDSASigner signer = new ECDSASigner();
//        signer.init(true, new ECPrivateKeyParameters(privateKey, domainParameters));
//        SHA256Digest digest = new SHA256Digest();
//        byte[] dataBytes = data.getBytes();
//        digest.update(dataBytes, 0, dataBytes.length);
//        byte[] hash = new byte[digest.getDigestSize()];
//        digest.doFinal(hash, 0);
//        BigInteger[] rs = signer.generateSignature(hash);
//        return new String[] {
//                Hex.toHexString(rs[0].toByteArray()),
//                Hex.toHexString(rs[1].toByteArray())
//        };
//    }

    // secp256k1
    public static String[] signECDSA(String privateKeyHex, String data)  {
        BigInteger privateKey = new BigInteger(privateKeyHex, 16);
        ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256k1");
        ECDomainParameters domainParameters = new ECDomainParameters(spec.getCurve(), spec.getG(), spec.getN());
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

    // 256r1相关代码
//    public static String getPublicKeyFromPrivateKey(String p)  {
//        BigInteger privateKey = new BigInteger(p);
//        ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256r1");
//        ECPoint publicPoint = spec.getG().multiply(privateKey);
//        byte[] encoded = publicPoint.getEncoded(true);
//        return Hex.toHexString(encoded);
//    }

    public static String getPublicKeyFromPrivateKey(String privateKeyHex)  {
        BigInteger privateKey = new BigInteger(privateKeyHex, 16);
        ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256k1");
        ECPoint publicPoint = spec.getG().multiply(privateKey);
        // 将公钥编码从压缩格式改为非压缩格式，以匹配以太坊和supervisor的实现
        byte[] encoded = publicPoint.getEncoded(false);
        return Hex.toHexString(encoded);
    }

    // 256r1相关代码
//    public static String generatePrivateKey() {
//        try {
//            // 获取 secp256r1 曲线的参数
//            ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256r1");
//            ECDomainParameters domainParams = new ECDomainParameters(
//                    spec.getCurve(), spec.getG(), spec.getN(), spec.getH());
//
//            // 使用安全随机数生成器
//            SecureRandom random = new SecureRandom();
//
//            // 创建密钥对生成器
//            ECKeyPairGenerator generator = new ECKeyPairGenerator();
//            ECKeyGenerationParameters params = new ECKeyGenerationParameters(domainParams, random);
//            generator.init(params);
//
//            // 生成密钥对
//            AsymmetricCipherKeyPair keyPair = generator.generateKeyPair();
//            ECPrivateKeyParameters privateKeyParams = (ECPrivateKeyParameters) keyPair.getPrivate();
//
//            // 获取私钥的BigInteger值
//            BigInteger privateKey = privateKeyParams.getD();
//
//            // 返回私钥的十六进制字符串表示
//            return privateKey.toString(10);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            return null;
//        }
//    }

    public static String generatePrivateKey() {
        try {
            // 获取 secp256k1 曲线参数
            ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256k1");
            ECDomainParameters domainParams = new ECDomainParameters(
                    spec.getCurve(), spec.getG(), spec.getN(), spec.getH());

            // 安全随机数生成器
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
            return privateKey.toString(16);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) {
        System.out.println(generatePrivateKey());
    }
    
    //The standard private key format is a 32-byte 256-bit hexadecimal number.
    public static boolean isNewPrivateKeyFormat(String privateKey) {
        if (privateKey == null || privateKey.length() != 64) {
            return false;
        }
        // IS hexadecimal
        return privateKey.matches("[0-9a-fA-F]+");
    }


    /**
     * 标准 secp256k1 签名（返回 r + s + v）
     * 后端必须靠 v 才能恢复公钥！
     */
    public static String[] sign(String privateKeyHex, String data) {
        try {
            BigInteger privateKey = new BigInteger(privateKeyHex, 16);
            ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256k1");
            ECDomainParameters domain = new ECDomainParameters(spec.getCurve(), spec.getG(), spec.getN());

            // 1. 数据必须用 Keccak256 哈希（和地址算法统一）
            byte[] dataBytes = data.getBytes();
            KeccakDigest keccak = new KeccakDigest(256);
            keccak.update(dataBytes, 0, dataBytes.length);
            byte[] hash = new byte[32];
            keccak.doFinal(hash, 0);

            // 2. 签名
            ECDSASigner signer = new ECDSASigner();
            signer.init(true, new ECPrivateKeyParameters(privateKey, domain));
            BigInteger[] rs = signer.generateSignature(hash);
            BigInteger r = rs[0];
            BigInteger s = rs[1];

            // 3. 计算 v (recoveryId) → 后端恢复公钥必备
            ECPoint pubPoint = spec.getG().multiply(privateKey);
            int v = calculateV(r, s, hash, pubPoint, domain);

            // 4. 返回：r, s, v（三个都传给后端）
            return new String[]{
                    Hex.toHexString(r.toByteArray()),
                    Hex.toHexString(s.toByteArray()),
                    String.valueOf(v)
            };
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ==============================================
    // 工具方法：计算 v 值（恢复公钥必须）
    // ==============================================
    private static int calculateV(BigInteger r, BigInteger s, byte[] hash, ECPoint pubPoint, ECDomainParameters domain) {
        int v = 0;
        while (v < 4) {
            try {
                ECPoint recover = recoverPubKey(r, s, hash, v, domain);
                if (recover != null && recover.equals(pubPoint)) {
                    break;
                }
            } catch (Exception ignored) {}
            v++;
        }
        return v;
    }

    private static ECPoint recoverPubKey(BigInteger r, BigInteger s, byte[] hash, int v, ECDomainParameters domain) {
        BigInteger n = domain.getN();
        BigInteger i = BigInteger.valueOf(v / 2);
        BigInteger x = r.add(i.multiply(n));
        ECPoint R = decompressKey(x, (v & 1) == 1, domain);
        if (R == null) return null;
        if (!R.multiply(n).isInfinity()) return null;

        BigInteger e = new BigInteger(1, hash);
        BigInteger eInv = BigInteger.ZERO.subtract(e).mod(n);
        BigInteger rInv = r.modInverse(n);
        BigInteger srInv = rInv.multiply(s).mod(n);
        BigInteger eInvrInv = rInv.multiply(eInv).mod(n);

        ECPoint q = domain.getG().multiply(eInvrInv).add(R.multiply(srInv));
        return q.normalize();
    }

    private static ECPoint decompressKey(BigInteger xBN, boolean yBit, ECDomainParameters domain) {
        try {
            org.bouncycastle.math.ec.ECCurve curve = domain.getCurve();
            BigInteger x = xBN.mod(domain.getN());
            byte[] xEnc = bigIntegerToBytes(x, 32);
            return curve.decodePoint(concatenate(yBit ? (byte)0x03 : 0x02, xEnc));
        } catch (Exception e) {
            return null;
        }
    }

    private static byte[] bigIntegerToBytes(BigInteger bi, int length) {
        byte[] bytes = bi.toByteArray();
        byte[] result = new byte[length];
        if (bytes.length > length + 1) throw new RuntimeException();
        int srcPos = (bytes[0] == 0) ? 1 : 0;
        int destPos = length - Math.min(bytes.length - srcPos, length);
        System.arraycopy(bytes, srcPos, result, destPos, bytes.length - srcPos);
        return result;
    }

    private static byte[] concatenate(byte b, byte[] array) {
        byte[] res = new byte[array.length + 1];
        res[0] = b;
        System.arraycopy(array, 0, res, 1, array.length);
        return res;
    }

}