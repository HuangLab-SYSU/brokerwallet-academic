package com.example.brokerfi.swap;

import com.example.brokerfi.token.TokenTxRecord;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import javax.net.ssl.SSLException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;

/** A request-body boundary, not a claim that the server accepted or executed a transaction. */
final class SwapSendDiagnostics {
    interface Save { boolean save(); }
    final TokenTxRecord record;
    private final Save save;
    SwapSendDiagnostics(TokenTxRecord record, Save save) { this.record=record; this.save=save; }
    void stage(String stage) throws IOException {
        record.sendStage=stage;
        record.sendUpdatedMs=System.currentTimeMillis();
        if (!save.save()) {
            record.sendFailure="JOURNAL";
            throw new IOException("Cannot persist send diagnostics");
        }
    }
    void failure(Exception error) {
        if (record.sendFailure==null) record.sendFailure=category(error);
    }
    static String category(Exception error) {
        if(error instanceof UnknownHostException)return "DNS";
        if(error instanceof ConnectException)return "CONNECT";
        if(error instanceof SSLException)return "TLS";
        if(error instanceof InterruptedIOException)return "TIMEOUT";
        if(error instanceof IOException)return "IO";
        return "LOCAL";
    }
    static String failureStatus(TokenTxRecord r) {
        if(SwapReceipt.validHash(r.txHash) && ("SUCCESS".equals(r.status) || "FAILED".equals(r.status)))return r.status;
        return SwapReceipt.validHash(r.txHash)?"PENDING":r.sendMayHaveReachedGateway?"UNKNOWN":"NOT_SENT";
    }
    String post(OkHttpClient base, String url, RequestBody body) throws Exception {
        // Disable redirects too: even a redirect must not silently replay a financial request.
        OkHttpClient transport=base.newBuilder().retryOnConnectionFailure(false)
                .followRedirects(false).followSslRedirects(false).build();
        RequestBody tracked=new RequestBody() {
            @Override public MediaType contentType() { return body.contentType(); }
            @Override public long contentLength() throws IOException { return body.contentLength(); }
            @Override public boolean isOneShot() { return true; }
            @Override public void writeTo(BufferedSink sink) throws IOException {
                record.sendMayHaveReachedGateway=true;
                // Commit BEFORE any body bytes: a crash after this point stays safely unresolved.
                stage("REQUEST_BODY_START");
                body.writeTo(sink);
                stage("REQUEST_BODY_WRITTEN");
            }
        };
        Request request=new Request.Builder().url(url).post(tracked).build();
        stage("NETWORK_START");
        try(Response response=transport.newCall(request).execute()) {
            record.sendHttpStatus=response.code();
            stage("RESPONSE_HEADERS");
            if(!response.isSuccessful()) {
                record.sendFailure="HTTP";
                throw new IOException("Send HTTP failure");
            }
            String raw=response.body()==null?"":response.body().string();
            stage("RESPONSE_BODY");
            return parseHash(raw);
        }
    }
    String parseHash(String raw) throws IOException {
        try {
            if(raw!=null && raw.trim().startsWith("{")) {
                JsonObject envelope=JsonParser.parseString(raw).getAsJsonObject();
                if(envelope.has("error")) {
                    record.sendFailure="GATEWAY_ERROR";
                    try {
                        String code=envelope.getAsJsonObject("error").get("code").getAsString();
                        if(code.matches("-?[0-9]{1,10}"))record.sendRpcCode=Integer.valueOf(code);
                    } catch(RuntimeException ignored) { }
                    throw new IOException("Send gateway error");
                }
            }
            return SwapReceipt.sentHash(raw);
        } catch(RuntimeException e) {
            record.sendFailure="INVALID_RESPONSE";
            throw new IOException("Send response has no valid hash");
        }
    }
    static String details(TokenTxRecord r) {
        if(r.sendRequestId==null)return "\nSend diagnostics: this older record has no send-stage data. The cause cannot be determined from it.";
        // Render fixed categories/numbers only; never show an exception or response message.
        String id=r.sendRequestId.matches("[0-9a-fA-F-]{36}")?r.sendRequestId:"Unavailable";
        return "\nRequest ID (not a transaction hash): "+id
                +"\nSend stage: "+stageLabel(r.sendStage)
                +"\nRequest may have been sent: "+(r.sendMayHaveReachedGateway?"Yes (execution is not confirmed)":"No")
                +(r.sendHttpStatus==null?"":"\nHTTP status: "+r.sendHttpStatus)
                +(r.sendRpcCode==null?"":"\nGateway error code: "+r.sendRpcCode)
                +(r.sendFailure==null?"":"\nSend error category: "+safe(r.sendFailure))
                +(r.deadlineSeconds<=0?"":"\nContract deadline (Unix seconds): "+r.deadlineSeconds)
                +(r.receiptAttempts<=0?"":"\nReceipt query attempts: "+r.receiptAttempts)
                +(r.receiptFailure==null?"":"\nReceipt query: "+safe(r.receiptFailure));
    }
    private static String stageLabel(String stage) {
        if(stage==null)return "—";
        switch(stage) {
            case "PREPARE":return "Local preparation";
            case "SIGNING":return "Local signing";
            case "REQUEST_READY":return "Request prepared";
            case "NETWORK_START":return "Connecting to gateway";
            case "REQUEST_BODY_START":return "Writing request";
            case "REQUEST_BODY_WRITTEN":return "Request written, awaiting response";
            case "RESPONSE_HEADERS":return "HTTP response received";
            case "RESPONSE_BODY":return "Response body received";
            case "HASH_RECEIVED":return "Valid transaction hash received";
            default:return "Unclassified";
        }
    }
    private static String safe(String value) {
        if(value==null)return "—";
        switch(value) {
            case "PREPARE":case "SIGNING":case "REQUEST_READY":case "NETWORK_START":
            case "REQUEST_BODY_START":case "REQUEST_BODY_WRITTEN":case "RESPONSE_HEADERS":
            case "RESPONSE_BODY":case "HASH_RECEIVED":case "JOURNAL":case "DNS":
            case "CONNECT":case "TLS":case "TIMEOUT":case "IO":case "LOCAL":case "HTTP":
            case "GATEWAY_ERROR":case "INVALID_RESPONSE":case "PENDING":return value;
            default:return "Unclassified";
        }
    }
}
