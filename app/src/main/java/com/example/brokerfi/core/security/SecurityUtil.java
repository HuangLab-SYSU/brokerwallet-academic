package com.example.brokerfi.core.security;

import java.math.BigInteger;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.Security;
import java.util.HashMap;
import java.util.Map;

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
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

public class SecurityUtil {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    /* Old address generation method: take the first 20 bytes of the SHA-256 public-key hash. / ?????????SHA-256 ??????? 20 ???
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

    // New address generation method, consistent with the Ethereum standard and server side: the last 20 bytes of Keccak-256 (public key)
    public static String GetAddress(String privateKey) {
        try {
            String publicKey = getPublicKeyFromPrivateKey(privateKey);
            byte[] decode = Hex.decode(publicKey);

            // Hash the public key with Keccak-256, pay attention! ! Ignore prefix byte (0x04)
            KeccakDigest keccakDigest = new KeccakDigest(256);

            // According to the standard of Ethereum, that is, the crypto method used on the go supervisor side, the first byte (prefix) is skipped and only the X and Y coordinate parts are used.
            keccakDigest.update(decode, 1, decode.length - 1);
            byte[] keccakHash = new byte[keccakDigest.getDigestSize()];
            keccakDigest.doFinal(keccakHash, 0);

            // Take the last 20 bytes of the Keccak-256 hash result as the address.
            byte[] addressBytes = new byte[20];
            System.arraycopy(keccakHash, keccakHash.length - 20, addressBytes, 0, 20);

            // Convert address bytes to hex string
            return Hex.toHexString(addressBytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "NONSTANDARD_PRIVATE_KEY";
    }


    // 256r1 related code
//    public static String[] signECDSA(String privateKey1, String data)  {
//        BigInteger privateKey = new BigInteger(privateKey1,10);
//        // Bouncy Castle ECNamedCurveTable#getParameterSpec.
//        // Upgrade to secp256k1.
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

    // 256r1 related code
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
        // Change public key encoding from compressed to uncompressed format to match Ethereum and supervisor implementations.
        byte[] encoded = publicPoint.getEncoded(false);
        return Hex.toHexString(encoded);
    }

    // 256r1 related code
//    public static String generatePrivateKey() {
//        try {
//            // Get secp256r1 curve parameters.
//            ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256r1");
//            ECDomainParameters domainParams = new ECDomainParameters(
//                    spec.getCurve(), spec.getG(), spec.getN(), spec.getH());
//
//            // Use a secure random number generator.
//            SecureRandom random = new SecureRandom();
//
//            // Create the key-pair generator.
//            ECKeyPairGenerator generator = new ECKeyPairGenerator();
//            ECKeyGenerationParameters params = new ECKeyGenerationParameters(domainParams, random);
//            generator.init(params);
//
//            // Generate the key pair.
//            AsymmetricCipherKeyPair keyPair = generator.generateKeyPair();
//            ECPrivateKeyParameters privateKeyParams = (ECPrivateKeyParameters) keyPair.getPrivate();
//
//            // Get the private key BigInteger value.
//            BigInteger privateKey = privateKeyParams.getD();
//
//            // Return the private key as a hexadecimal string.
//            return privateKey.toString(10);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            return null;
//        }
//    }

    public static String generatePrivateKey() {
        try {
            // Get secp256k1 curve parameters
            ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256k1");
            ECDomainParameters domainParams = new ECDomainParameters(
                    spec.getCurve(), spec.getG(), spec.getN(), spec.getH());

            // Secure random number generator
            SecureRandom random = new SecureRandom();

            // Create a key pair generator
            ECKeyPairGenerator generator = new ECKeyPairGenerator();
            ECKeyGenerationParameters params = new ECKeyGenerationParameters(domainParams, random);
            generator.init(params);

            // Generate key pair
            AsymmetricCipherKeyPair keyPair = generator.generateKeyPair();
            ECPrivateKeyParameters privateKeyParams = (ECPrivateKeyParameters) keyPair.getPrivate();

            // Get the BigInteger value of the private key.
            BigInteger privateKey = privateKeyParams.getD();

            // Returns the hexadecimal string representation of the private key.
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
    //Support both with and without 0x/0X prefix (like MetaMask)
    public static boolean isNewPrivateKeyFormat(String privateKey) {
        if (privateKey == null) {
            return false;
        }
        // Check if it has 0x or 0X prefix
        if (privateKey.startsWith("0x") || privateKey.startsWith("0X")) {
            // Must be exactly 66 characters long (2 for prefix + 64 for key)
            if (privateKey.length() != 66) {
                return false;
            }
            // Check if the part after prefix is hexadecimal
            return privateKey.substring(2).matches("[0-9a-fA-F]+");
        } else {
            // Standard format without prefix
            if (privateKey.length() != 64) {
                return false;
            }
            // Check if it's hexadecimal
            return privateKey.matches("[0-9a-fA-F]+");
        }
    }

    // Remove 0x or 0X prefix from private key if exists
    public static String removePrivateKeyPrefix(String privateKey) {
        if (privateKey == null) {
            return null;
        }
        if (privateKey.startsWith("0x") || privateKey.startsWith("0X")) {
            return privateKey.substring(2);
        }
        return privateKey;
    }

    // Check if the address format is valid
    // Standard address is 40 hex characters without 0x prefix
    public static boolean isAddressFormatValid(String address) {
        if (address == null) {
            return false;
        }
        // Check if it has 0x or 0X prefix
        if (address.startsWith("0x") || address.startsWith("0X")) {
            // Must be exactly 42 characters long (2 for prefix + 40 for address)
            if (address.length() != 42) {
                return false;
            }
            // Check if the part after prefix is hexadecimal
            return address.substring(2).matches("[0-9a-fA-F]+");
        } else {
            // Standard format without prefix
            if (address.length() != 40) {
                return false;
            }
            // Check if it's hexadecimal
            return address.matches("[0-9a-fA-F]+");
        }
    }

    // Remove 0x or 0X prefix from address if exists
    public static String removeAddressPrefix(String address) {
        if (address == null) {
            return null;
        }
        if (address.startsWith("0x") || address.startsWith("0X")) {
            return address.substring(2);
        }
        return address;
    }


    public static Map<String, String> signMessage(String privateKeyHex, String message) {
        Credentials credentials = Credentials.create(privateKeyHex);

        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);

        // Ethereum standard signature (automatically prefixed)
        Sign.SignatureData signature = Sign.signPrefixedMessage(
                messageBytes,
                credentials.getEcKeyPair()
        );

        String r = Numeric.toHexString(signature.getR());
        String s = Numeric.toHexString(signature.getS());
        String v = Numeric.toHexString(signature.getV()); // 27 / 28

        Map<String, String> result = new HashMap<>();
        result.put("r", r);
        result.put("s", s);
        result.put("v", v);
        result.put("message", message);

        return result;
    }

}
