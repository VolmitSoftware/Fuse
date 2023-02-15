package com.volmit.fuse.fabric.util;

public abstract class Looper extends Thread {
    public void run() {
        while (!interrupted()) {
            try {
                long m = loop();

                if (m < 0) {
                    break;
                }

                Thread.sleep(m);
            } catch (InterruptedException e) {
                break;
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }

    protected abstract long loop();
}
