package com.example.brokerfi.swap;

import java.io.IOException;

/** Cancels obsolete preview requests and rejects results from an inactive session. */
final class TradeReadSession {
    interface Active { boolean get(); }
    interface Progress { void started(int number); }
    static final class Cancelled extends IOException {
        Cancelled() { super("Obsolete read session"); }
    }
    private final Active active;
    private final Progress progress;
    private volatile boolean cancelled;
    private Runnable cancelRequest;
    private int calls;
    TradeReadSession(Active active, Progress progress) { this.active=active; this.progress=progress; }
    void check() throws Cancelled { if (cancelled || !active.get()) throw new Cancelled(); }
    void starting() throws Cancelled { check(); if(progress!=null)progress.started(++calls); }
    synchronized void attach(Runnable cancelRequest) throws Cancelled {
        check();
        this.cancelRequest = cancelRequest;
    }
    synchronized void finished() { cancelRequest = null; }
    void cancel() {
        Runnable request;
        synchronized (this) {
            cancelled = true;
            request = cancelRequest;
        }
        if (request != null) request.run();
    }
}
