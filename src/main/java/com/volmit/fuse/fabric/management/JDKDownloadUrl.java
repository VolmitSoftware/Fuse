package com.volmit.fuse.fabric.management;

import org.apache.commons.lang3.SystemUtils;

public enum JDKDownloadUrl {
    WINDOWS("https://download.java.net/java/GA/jdk$version/$hash/$code/GPL/openjdk-$version_windows-x64_bin.zip"),
    MAC_ARM("https://download.java.net/java/GA/jdk$version/$hash/$code/GPL/openjdk-$version_macos-aarch64_bin.tar.gz"),
    MAC("https://download.java.net/java/GA/jdk$version/$hash/$code/GPL/openjdk-$version_macos-x64_bin.tar.gz"),
    LINUX_ARM("https://download.java.net/java/GA/jdk$version/$hash/$code/GPL/openjdk-$version_macos-aarch64_bin.tar.gz"),
    LINUX("https://download.java.net/java/GA/jdk$version/$hash/$code/GPL/openjdk-$version_linux-x64_bin.tar.gz");

    public static final String VERSION = "17.0.2";
    public static final String HASH = "dfd4a8d0985749f896bed50d7138ee7f";
    public static final String CODE = "8";
    private final String url;

    JDKDownloadUrl(String url) {
        this.url = url;
    }

    public static String getAutoUrl() {
        if (SystemUtils.IS_OS_WINDOWS) {
            return WINDOWS.getUrl();
        } else if (SystemUtils.IS_OS_MAC) {
            if (System.getProperty("os.arch").equals("amd64")) {
                return MAC.getUrl();
            }
            return MAC_ARM.getUrl();
        } else if (SystemUtils.IS_OS_LINUX) {
            if (System.getProperty("os.arch").equals("amd64")) {
                return LINUX.getUrl();
            }
            return LINUX_ARM.getUrl();
        }

        return null;
    }

    public String getUrl() {
        return url.replaceAll("\\Q$version\\E", VERSION)
                .replaceAll("\\Q$hash\\E", HASH)
                .replaceAll("\\Q$code\\E", CODE);
    }
}
