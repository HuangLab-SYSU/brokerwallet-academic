package com.example.brokerfi.xc;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Security;
import java.security.Signature;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.ECGenParameterSpec;


import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.gm.GMNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.params.ECDomainParameters;
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
}
