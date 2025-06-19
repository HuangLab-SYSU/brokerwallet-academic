package com.example.brokerfi;

import org.junit.Test;

import static org.junit.Assert.*;

import com.example.brokerfi.xc.SecurityUtil;

import java.math.BigInteger;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void T() throws Exception {

        System.out.println("generatekey:");
        String privatekey = SecurityUtil.generatePrivateKey();
        System.out.println(privatekey);
        System.out.println("PublicKey:");
        System.out.println(SecurityUtil.getPublicKeyFromPrivateKey(privatekey));
        System.out.println("addr:");
        System.out.println(SecurityUtil.GetAddress(privatekey));
    }
}