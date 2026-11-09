package com.example.brokerfi.swap;

import com.example.brokerfi.token.TokenTxRecord;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import org.junit.Test;
import static org.junit.Assert.*;

/** Loopback fake gateway only: no keys, live gateway or financial transactions. */
public class SwapSendDiagnosticsTest {
    private static final String HASH="0x"+String.format("%064d",1);
    private TokenTxRecord record() {
        TokenTxRecord r=new TokenTxRecord();
        r.sendRequestId=UUID.randomUUID().toString();
        return r;
    }
    private RequestBody body() { return RequestBody.create(MediaType.parse("application/json"),"{}"); }
    private OkHttpClient client() { return new OkHttpClient.Builder().callTimeout(2,TimeUnit.SECONDS).build(); }
    private static class Gateway implements AutoCloseable {
        final ServerSocket server=new ServerSocket(0,1,java.net.InetAddress.getByName("127.0.0.1"));
        final AtomicInteger requests=new AtomicInteger();
        volatile Exception failure;
        final Thread worker;
        Gateway(int status,String response,boolean disconnect) throws IOException {
            this(status,response,disconnect,0);
        }
        Gateway(int status,String response,boolean disconnect,long responseDelayMs) throws IOException {
            server.setSoTimeout(4000);
            worker=new Thread(() -> {
                try(Socket socket=server.accept()) {
                    socket.setSoTimeout(3000);
                    BufferedReader reader=new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.UTF_8));
                    String line; int length=0;
                    while((line=reader.readLine())!=null && !line.isEmpty())
                        if(line.toLowerCase(java.util.Locale.ROOT).startsWith("content-length:"))length=Integer.parseInt(line.substring(15).trim());
                    for(int i=0;i<length;i++)if(reader.read()<0)throw new IOException("Incomplete fake request");
                    requests.incrementAndGet();
                    if(disconnect)return;
                    if(responseDelayMs>0)Thread.sleep(responseDelayMs);
                    byte[] bytes=response.getBytes(StandardCharsets.UTF_8);
                    String headers="HTTP/1.1 "+status+" Test\r\nContent-Length: "+bytes.length
                            +"\r\nConnection: close\r\nLocation: "+url()+"\r\n\r\n";
                    socket.getOutputStream().write(headers.getBytes(StandardCharsets.US_ASCII));
                    socket.getOutputStream().write(bytes);
                    socket.getOutputStream().flush();
                } catch(Exception e) { failure=e; }
            });
            worker.setDaemon(true); worker.start();
        }
        String url() { return "http://127.0.0.1:"+server.getLocalPort()+"/send"; }
        public void close() throws Exception {
            worker.join(4500); server.close();
            assertFalse("Fake gateway must terminate",worker.isAlive());
            if(failure!=null)throw failure;
        }
    }
    @Test public void successfulResponsePersistsBoundaryAndHttp() throws Exception {
        TokenTxRecord r=record(); List<String> stages=new ArrayList<>();
        SwapSendDiagnostics d=new SwapSendDiagnostics(r,() -> {stages.add(r.sendStage);return true;});
        try(Gateway gateway=new Gateway(200,"{\"result\":\""+HASH+"\"}",false)) {
            assertEquals(HASH,d.post(client(),gateway.url(),body()));
            assertEquals(1,gateway.requests.get());
        }
        assertTrue(r.sendMayHaveReachedGateway);
        assertEquals(Integer.valueOf(200),r.sendHttpStatus);
        assertEquals(java.util.Arrays.asList("NETWORK_START","REQUEST_BODY_START","REQUEST_BODY_WRITTEN","RESPONSE_HEADERS","RESPONSE_BODY"),stages);
    }
    @Test public void httpErrorAfterBodyIsUnknownNotNotSent() throws Exception {
        TokenTxRecord r=record(); SwapSendDiagnostics d=new SwapSendDiagnostics(r,() -> true);
        try(Gateway gateway=new Gateway(503,"secret server response",false)) {
            try { d.post(client(),gateway.url(),body());fail(); } catch(IOException e) {d.failure(e);}
            assertEquals(1,gateway.requests.get());
        }
        assertEquals("HTTP",r.sendFailure);
        assertEquals(Integer.valueOf(503),r.sendHttpStatus);
        assertEquals("UNKNOWN",SwapSendDiagnostics.failureStatus(r));
        assertFalse(SwapSendDiagnostics.details(r).contains("secret"));
    }
    @Test public void lostResponseIsUnknownAndNeverRetried() throws Exception {
        TokenTxRecord r=record(); SwapSendDiagnostics d=new SwapSendDiagnostics(r,() -> true);
        try(Gateway gateway=new Gateway(200,"",true)) {
            try {d.post(client(),gateway.url(),body());fail();}catch(IOException e){d.failure(e);}
            assertEquals(1,gateway.requests.get());
        }
        assertEquals("UNKNOWN",SwapSendDiagnostics.failureStatus(r));
        assertNull(r.txHash);
    }
    @Test public void redirectDoesNotReplay() throws Exception {
        TokenTxRecord r=record(); SwapSendDiagnostics d=new SwapSendDiagnostics(r,() -> true);
        try(Gateway gateway=new Gateway(307,"",false)) {
            try {d.post(client(),gateway.url(),body());fail();}catch(IOException e){d.failure(e);}
            assertEquals(1,gateway.requests.get());
        }
        assertEquals(Integer.valueOf(307),r.sendHttpStatus);
        assertEquals("UNKNOWN",SwapSendDiagnostics.failureStatus(r));
    }
    @Test public void failedInitialJournalNeverStartsNetwork() throws Exception {
        TokenTxRecord r=record(); SwapSendDiagnostics d=new SwapSendDiagnostics(r,() -> false);
        try {d.post(client(),"http://127.0.0.1:1/send",body());fail();}catch(IOException e){d.failure(e);}
        assertEquals("NETWORK_START",r.sendStage);
        assertEquals("JOURNAL",r.sendFailure);
        assertFalse(r.sendMayHaveReachedGateway);
        assertEquals("NOT_SENT",SwapSendDiagnostics.failureStatus(r));
    }
    @Test public void dnsAndLocalFailureAreNotSent() {
        TokenTxRecord r=record(); SwapSendDiagnostics d=new SwapSendDiagnostics(r,() -> true);
        d.failure(new UnknownHostException("sensitive"));
        assertEquals("DNS",r.sendFailure);
        assertEquals("NOT_SENT",SwapSendDiagnostics.failureStatus(r));
        assertEquals("LOCAL",SwapSendDiagnostics.category(new IllegalArgumentException("private")));
        assertEquals("TIMEOUT",SwapSendDiagnostics.category(new SocketTimeoutException("private")));
    }
    @Test public void connectionRefusedBeforeBodyIsNotSent() throws Exception {
        int port;
        try(ServerSocket unused=new ServerSocket(0,1,java.net.InetAddress.getByName("127.0.0.1"))) {port=unused.getLocalPort();}
        TokenTxRecord r=record();SwapSendDiagnostics d=new SwapSendDiagnostics(r,() -> true);
        try {d.post(client(),"http://127.0.0.1:"+port+"/send",body());fail();}catch(IOException e){d.failure(e);}
        assertFalse(r.sendMayHaveReachedGateway);
        assertEquals("CONNECT",r.sendFailure);
        assertEquals("NOT_SENT",SwapSendDiagnostics.failureStatus(r));
    }
    @Test public void bodyBoundaryMustBeDurableBeforePayloadAndFailureStaysConservative() throws Exception {
        TokenTxRecord r=record();AtomicInteger payloadWrites=new AtomicInteger();
        SwapSendDiagnostics d=new SwapSendDiagnostics(r,() -> !"REQUEST_BODY_START".equals(r.sendStage));
        RequestBody payload=new RequestBody() {
            public MediaType contentType(){return MediaType.parse("application/json");}
            public void writeTo(okio.BufferedSink sink)throws IOException {payloadWrites.incrementAndGet();sink.writeUtf8("{}");}
        };
        OkHttpClient intercepted=client().newBuilder().addInterceptor(chain -> {
            chain.request().body().writeTo(new okio.Buffer());
            throw new IOException("Should not reach response");
        }).build();
        try {d.post(intercepted,"http://127.0.0.1:1/send",payload);fail();}catch(IOException e){d.failure(e);}
        assertEquals(0,payloadWrites.get());
        assertEquals("JOURNAL",r.sendFailure);
        assertEquals("UNKNOWN",SwapSendDiagnostics.failureStatus(r));
    }
    @Test public void persistedDiagnosticsContainNoResponseOrExceptionText() throws Exception {
        TokenTxRecord r=record();SwapSendDiagnostics d=new SwapSendDiagnostics(r,() -> true);
        try {d.parseHash("{\"error\":{\"message\":\"signature-secret\",\"code\":\"not-a-number\"}}");fail();}
        catch(IOException e){d.failure(e);}
        d.failure(new IOException("private-key-secret"));
        String json=new com.google.gson.Gson().toJson(r);
        assertNull(r.sendRpcCode);
        assertFalse(json.contains("signature-secret"));assertFalse(json.contains("private-key-secret"));
    }
    @Test public void gatewayErrorKeepsOnlyNumericCode() throws Exception {
        TokenTxRecord r=record(); SwapSendDiagnostics d=new SwapSendDiagnostics(r,() -> true);
        try {d.parseHash("{\"error\":{\"code\":-32603,\"message\":\"private payload\"},\"result\":\""+HASH+"\"}");fail();}
        catch(IOException e){d.failure(e);}
        assertEquals("GATEWAY_ERROR",r.sendFailure);
        assertEquals(Integer.valueOf(-32603),r.sendRpcCode);
        assertNull(r.txHash);
        assertFalse(SwapSendDiagnostics.details(r).contains("private"));
    }
    @Test public void malformedResponseCannotBecomeHash() throws Exception {
        TokenTxRecord r=record(); SwapSendDiagnostics d=new SwapSendDiagnostics(r,() -> true);
        try {d.parseHash("opaque-id-with-secret");fail();}catch(IOException e){d.failure(e);}
        assertEquals("INVALID_RESPONSE",r.sendFailure);
        assertNull(r.txHash);
    }
    @Test public void validHashNeverBecomesNotSent() {
        TokenTxRecord r=record();r.txHash=HASH;
        assertEquals("PENDING",SwapSendDiagnostics.failureStatus(r));
    }
    @Test public void confirmedReceiptIsNotRegressedOnJournalFailure() {
        TokenTxRecord r=record();r.txHash=HASH;r.status="SUCCESS";r.receiptFailure="JOURNAL";
        assertEquals("SUCCESS",SwapSendDiagnostics.failureStatus(r));
        r.status="FAILED";
        assertEquals("FAILED",SwapSendDiagnostics.failureStatus(r));
    }
    @Test(timeout=50000) public void slowResponseBeyondOldThirtySecondsStillReturnsHashWithoutResend() throws Exception {
        BrokerSwapClient client=new BrokerSwapClient(null);
        java.lang.reflect.Field field=BrokerSwapClient.class.getDeclaredField("transactions");field.setAccessible(true);
        OkHttpClient transport=(OkHttpClient)field.get(client);
        TokenTxRecord r=record();SwapSendDiagnostics d=new SwapSendDiagnostics(r,() -> true);
        long started=System.nanoTime();
        try(Gateway gateway=new Gateway(200,"{\"result\":\""+HASH+"\"}",false,35000)) {
            assertEquals(HASH,d.post(transport,gateway.url(),body()));
            assertEquals(1,gateway.requests.get());
        }
        assertTrue(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime()-started)>=35);
        assertNull(r.sendFailure);assertEquals(Integer.valueOf(200),r.sendHttpStatus);
    }
    @Test public void oldRecordsAreNotRetroactivelyDiagnosedAndDisplayIsAllowlisted() {
        TokenTxRecord r=new TokenTxRecord();r.status="UNKNOWN";
        assertTrue(SwapSendDiagnostics.details(r).contains("older record"));
        assertEquals("UNKNOWN",r.status);
        r.sendRequestId="secret";r.sendStage="private key";r.sendFailure="raw payload";
        String display=SwapSendDiagnostics.details(r);
        assertFalse(display.contains("secret"));assertFalse(display.contains("private"));assertFalse(display.contains("payload"));
    }
}
