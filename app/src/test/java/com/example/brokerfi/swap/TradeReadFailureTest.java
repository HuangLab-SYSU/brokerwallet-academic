package com.example.brokerfi.swap;

import org.junit.Test;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import static org.junit.Assert.*;

public class TradeReadFailureTest {
    @Test public void timeoutIsNotPresentedAsConfigurationMismatch() {
        assertEquals("Timed out waiting for the chain response.",TradeReadFailure.describe(new SocketTimeoutException("secret request")));
        assertEquals("factory query: Timed out waiting for the chain response.",TradeReadFailure.describe(
                new TradeReadFailure.Query("factory",new SocketTimeoutException("secret"))));
    }
    @Test public void networkErrorDoesNotRevealHostnameOrBody() {
        assertEquals("Network connection failed.",TradeReadFailure.describe(new UnknownHostException("sensitive hostname")));
        assertEquals("Validation failed or the query could not be completed.",TradeReadFailure.describe(new IOException("sensitive body")));
    }
    @Test public void serviceAccountAndMalformedDataHaveDifferentDescriptions() {
        assertEquals("Chain service temporarily unavailable.",TradeReadFailure.describe(new TradeReadFailure(TradeReadFailure.Kind.HTTP)));
        assertEquals("The chain service rejected the query.",TradeReadFailure.describe(new TradeReadFailure(TradeReadFailure.Kind.REJECTED)));
        assertEquals("The gateway did not recognize this account.",TradeReadFailure.describe(new TradeReadFailure(TradeReadFailure.Kind.ACCOUNT)));
        assertEquals("Incomplete data returned by the chain service.",TradeReadFailure.describe(new TradeReadFailure(TradeReadFailure.Kind.INVALID)));
    }
    @Test public void slowDashWaitIsBounded() {
        assertEquals(330,BrokerSwapClient.SIGNED_READ_TIMEOUT_SECONDS);
        assertEquals(330,BrokerSwapClient.SIGNED_READ_CALL_TIMEOUT_SECONDS);
    }
    @Test public void publicRpcKeepsOriginalBoundsAndTransactionsWaitWithoutRetries() throws Exception {
        BrokerSwapClient client=new BrokerSwapClient(null);
        java.lang.reflect.Field writes=BrokerSwapClient.class.getDeclaredField("http");writes.setAccessible(true);
        java.lang.reflect.Field reads=BrokerSwapClient.class.getDeclaredField("signedReads");reads.setAccessible(true);
        java.lang.reflect.Field operations=BrokerSwapClient.class.getDeclaredField("transactions");operations.setAccessible(true);
        okhttp3.OkHttpClient original=(okhttp3.OkHttpClient)writes.get(client), signed=(okhttp3.OkHttpClient)reads.get(client);
        okhttp3.OkHttpClient transaction=(okhttp3.OkHttpClient)operations.get(client);
        assertEquals(25000,original.readTimeoutMillis());assertEquals(30000,original.callTimeoutMillis());
        assertFalse(original.retryOnConnectionFailure());assertFalse(signed.retryOnConnectionFailure());
        assertEquals(330000,signed.readTimeoutMillis());assertEquals(330000,signed.callTimeoutMillis());
        assertEquals(330000,transaction.readTimeoutMillis());assertEquals(330000,transaction.callTimeoutMillis());
        assertFalse(transaction.retryOnConnectionFailure());
    }
}
