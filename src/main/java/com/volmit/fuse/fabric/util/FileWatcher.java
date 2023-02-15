//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.volmit.fuse.fabric.util;

import java.io.File;

public class FileWatcher {
    protected final File file;
    private long lastModified;
    private long size;

    public FileWatcher(File file) {
        this.file = file;
        this.readProperties();
    }

    protected void readProperties() {
        boolean exists = this.file.exists();
        this.lastModified = exists ? this.file.lastModified() : -1L;
        this.size = exists ? (this.file.isDirectory() ? -2L : this.file.length()) : -1L;
    }

    public boolean checkModified() {
        long m = this.lastModified;
        long g = this.size;
        boolean mod = false;
        this.readProperties();
        if (this.lastModified != m || g != this.size) {
            mod = true;
        }

        return mod;
    }
}
