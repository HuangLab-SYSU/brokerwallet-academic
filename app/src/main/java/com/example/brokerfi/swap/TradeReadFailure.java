package com.example.brokerfi.swap;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import javax.net.ssl.SSLException;

/** Allowlisted descriptions only: never exposes server bodies, signed requests or credentials. */
final class TradeReadFailure extends IOException {
    enum Kind { HTTP, REJECTED, ACCOUNT, INVALID }
    final Kind kind;
    TradeReadFailure(Kind kind) { super("Chain read " + kind.name()); this.kind=kind; }
    static final class Query extends IOException {
        final String getter;
        Query(String getter, IOException cause) { super("Metadata read failed",cause); this.getter=getter; }
    }
    static String describe(Throwable error) {
        for(Throwable e=error;e!=null;e=e.getCause()) {
            if(e instanceof Query) return ((Query)e).getter+" query: "+describe(e.getCause());
            if(e instanceof InterruptedIOException) return "Timed out waiting for the chain response.";
            if(e instanceof UnknownHostException || e instanceof ConnectException || e instanceof SSLException)
                return "Network connection failed.";
            if(e instanceof TradeReadFailure) {
                switch(((TradeReadFailure)e).kind) {
                    case HTTP: return "Chain service temporarily unavailable.";
                    case ACCOUNT: return "The gateway did not recognize this account.";
                    case REJECTED: return "The chain service rejected the query.";
                    case INVALID: return "Incomplete data returned by the chain service.";
                }
            }
        }
        return "Validation failed or the query could not be completed.";
    }
}
