package com.volmit.fuse.fabric.util;

import net.minecraft.client.MinecraftClient;

public class GlfwUtils {
    public static boolean isWindowFocused() {
        try {
            return MinecraftClient.getInstance().isWindowFocused();
        } catch (Throwable e) {
            return true;
        }
    }
}
