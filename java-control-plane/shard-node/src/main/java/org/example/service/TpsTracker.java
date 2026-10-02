package org.example.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class TpsTracker {

    private final AtomicLong processed = new AtomicLong(0);

    public void increment() {
        processed.incrementAndGet();
    }

    public long resetAndGet() {
        return processed.getAndSet(0);
    }
}