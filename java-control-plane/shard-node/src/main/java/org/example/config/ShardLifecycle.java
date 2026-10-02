package org.example.config;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class ShardLifecycle {

    private final AtomicBoolean initialized = new AtomicBoolean(false);

    private final AtomicBoolean running = new AtomicBoolean(false);

    public void markInitialized() {
        initialized.set(true);
    }

    public boolean isInitialized() {
        return initialized.get();
    }

    public void markStarted() {
        running.set(true);
    }

    public void markStopped() {
        running.set(false);
    }

    public boolean isRunning() {
        return running.get();
    }

    public boolean isReady() {
        return running.get();
    }
}