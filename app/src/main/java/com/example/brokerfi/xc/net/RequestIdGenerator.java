package com.example.brokerfi.xc.net;

import java.util.concurrent.atomic.AtomicInteger;

public class RequestIdGenerator {
    private static AtomicInteger counter = new AtomicInteger(1);
    
    public static int getNextId() {
        return counter.getAndIncrement();
    }
}
