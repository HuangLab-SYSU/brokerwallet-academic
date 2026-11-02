package com.example.brokerfi.swap;

import java.io.IOException;

/** Cancels obsolete previews between calls; an in-flight read may finish but is not published. */
final class TradeReadSession {
    interface Active { boolean get(); }
    interface Progress { void started(int number); }
    static final class Cancelled extends IOException {
        Cancelled() { super("Obsolete read session"); }
    }
    private final Active active;
    private final Progress progress;
    private int calls;
    TradeReadSession(Active active, Progress progress) { this.active=active; this.progress=progress; }
    void check() throws Cancelled { if (!active.get()) throw new Cancelled(); }
    void starting() throws Cancelled { check(); if(progress!=null)progress.started(++calls); }
}
