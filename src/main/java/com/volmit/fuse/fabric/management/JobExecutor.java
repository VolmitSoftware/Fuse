package com.volmit.fuse.fabric.management;

import art.arcane.multiburst.MultiBurst;
import com.volmit.fuse.fabric.Fuse;
import com.volmit.fuse.fabric.util.Looper;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class JobExecutor {
    @Getter
    private final MultiBurst burst;
    private final List<Runnable> queue;
    private final List<Runnable> after;
    private Looper progressUpdater;
    private int queued;
    private int completed;
    private boolean draining;

    public JobExecutor() {
        this.queue = new CopyOnWriteArrayList<>();
        this.after = new CopyOnWriteArrayList<>();
        burst = new MultiBurst("Fuse", 1);
        queued = 0;
        completed = 0;
        draining = false;
    }

    public double getProgress() {
        if (getQueued() <= 0) {
            return 0;
        }

        return (double) ((int) ((((double) getCompleted()) / (double) getQueued()) * 1000)) / 1000.0;
    }

    public int getCompleted() {
        return completed;
    }

    public int getQueued() {
        return queued;
    }

    public JobExecutor after(Runnable r) {
        after.add(r);

        return this;
    }

    public JobExecutor queue(Runnable r) {
        queue.add(r);
        queued = queued + 1;

        if (!draining) {
            draining = true;
            burst.lazy(this::drain);
        }

        return this;
    }

    public JobExecutor queueThenWait(Runnable r) {
        AtomicBoolean b = new AtomicBoolean(false);
        queue(() -> {
            r.run();
            b.set(true);
        });

        while (!b.get()) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return this;
    }

    public void drain() {
        draining = true;
        Fuse.log("Work Started");
        tickProgress();
        progressUpdater = new Looper() {
            @Override
            protected long loop() {
                if (System.currentTimeMillis() - Fuse.ll > 10000) {
                    tickProgress();
                    return 10000;
                }

                return 1000;
            }
        };
        progressUpdater.start();

        while (!queue.isEmpty()) {
            List<Runnable> q = new ArrayList<>(queue).stream().map((i) -> (Runnable) () -> {
                        try {
                            i.run();
                        } catch (Throwable e) {
                            e.printStackTrace();
                            Fuse.err("Failed to execute job!");
                        }
                        completed = completed + 1;
                    }
            ).collect(Collectors.toList());
            queue.clear();
            burst.burst(q);
        }

        progressUpdater.interrupt();
        tickProgress();

        MultiBurst.burst.lazy(() -> {
            Fuse.log("Work Completed. " + getCompleted() + " jobs completed.");
        });

        completed = 0;
        queued = 0;
        draining = false;

        if (!after.isEmpty()) {
            List<Runnable> a = new ArrayList<>(after);
            after.clear();
            a.forEach(Runnable::run);
        }
    }

    private void tickProgress() {
        Fuse.log("Working: " + getProgress() * 100 + "% (" + getCompleted() + "/" + getQueued() + ")");
    }
}
