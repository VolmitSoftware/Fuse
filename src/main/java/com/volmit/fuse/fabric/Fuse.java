package com.volmit.fuse.fabric;

import com.volmit.fuse.fabric.management.FuseService;
import com.volmit.fuse.fabric.management.data.Project;
import com.volmit.fuse.fabric.screen.WorkspaceScreen;
import com.volmit.fuse.fabric.screen.widget.FuseToast;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.InputUtil;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class Fuse implements ModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("fuse");
    public static final Identifier BUILD_FAILED_ID = new Identifier("fuse:build_failed");
    public static final Identifier BUILD_SUCCESS_ID = new Identifier("fuse:build_success");
    public static final Identifier BUILD_STARTED_ID = new Identifier("fuse:build_started");
    public static final Identifier ONLINE_ID = new Identifier("fuse:online");
    private static final KeyBinding keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "Workspace Manager",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_SEMICOLON,
            "Fuse"
    ));
    public static long ll = -1;
    public static String lastLog = "";
    public static FuseService service;
    public static SoundEvent BUILD_FAILED_SOUND = SoundEvent.of(BUILD_FAILED_ID);
    public static SoundEvent BUILD_SUCCESS_SOUND = SoundEvent.of(BUILD_SUCCESS_ID);
    public static SoundEvent BUILD_STARTED_SOUND = SoundEvent.of(BUILD_STARTED_ID);
    public static SoundEvent ONLINE_SOUND = SoundEvent.of(ONLINE_ID);

    private static void playSound(SoundEvent event) {
        playSound(event, 1.0F);
    }

    private static void playSound(SoundEvent event, float pitch) {
        MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(event, pitch));
    }

    public static void toast(FuseToast.Type type, String title, String desc) {
        MinecraftClient.getInstance().getToastManager().add(
                new FuseToast(type, Text.of(title), Text.of(desc)));
    }

    public static void onProjectBuildStarted(Project project) {
        playSound(BUILD_STARTED_SOUND);
        toast(FuseToast.Type.TUTORIAL_HINT, "Building Project", "Building " + project.getName());
    }

    public static void onProjectBuildSuccess(Project project) {
        playSound(BUILD_SUCCESS_SOUND);
        toast(FuseToast.Type.TUTORIAL_HINT, "Build Success", "Recompiled & Hotloaded " + project.getName());
    }

    public static void onProjectBuildFailed(Project project) {
        playSound(BUILD_FAILED_SOUND);
        toast(FuseToast.Type.TUTORIAL_HINT, "Build Failure", "Failed to compile " + project.getName() + " see console for details");
    }

    public static void onServerStarted() {
        playSound(ONLINE_SOUND);
        toast(FuseToast.Type.TUTORIAL_HINT, "Fuse Ready!", "The Fuse server is ready to go! Join to start developing!");
    }

    public static void log(String msg) {
        ll = System.currentTimeMillis();
        if (msg.equals("... done")) {
            return;
        }

        lastLog = msg;
        Fuse.LOG.info(msg);
    }

    public static void err(String msg) {
        ll = System.currentTimeMillis();
        if (msg.equals("... done")) {
            return;
        }

        lastLog = msg;
        Fuse.LOG.error(msg);
    }

    @Override
    public void onInitialize() {
        service = new FuseService(new File("fuse"));
        LOG.info("Starting Fuse Backend Service");
        service.open();
        Registry.register(Registries.SOUND_EVENT, BUILD_FAILED_ID, BUILD_FAILED_SOUND);
        Registry.register(Registries.SOUND_EVENT, BUILD_SUCCESS_ID, BUILD_SUCCESS_SOUND);
        Registry.register(Registries.SOUND_EVENT, BUILD_STARTED_ID, BUILD_STARTED_SOUND);
        Registry.register(Registries.SOUND_EVENT, ONLINE_ID, ONLINE_SOUND);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (keyBinding.wasPressed()) {
                client.setScreen(new WorkspaceScreen(client.currentScreen));
            }
        });
    }
}
