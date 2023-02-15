package com.volmit.fuse.fabric.mixin;

import com.volmit.fuse.fabric.Fuse;
import com.volmit.fuse.fabric.management.data.Project;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.hud.SubtitlesHud;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    private final List<String> building = new ArrayList<>();
    @Shadow
    @Final
    private SubtitlesHud subtitlesHud;

    @Inject(method = "render", at = @At("RETURN"))
    public void onRender(MatrixStack matrices, float tickDelta, CallbackInfo info) {
        for (Project i : Fuse.service.getWorkspace().getProjects()) {
            if (i.isBuilding()) {
                building.add(i.getName());
            }
        }

        if (building.isEmpty()) {
            return;
        }

        MinecraftClient.getInstance().textRenderer.draw(matrices, "Building " + String.join(", ", building), 5, 5, -1);
        MinecraftClient.getInstance().textRenderer.draw(matrices, Fuse.lastLog, 5, 15, -1);
        building.clear();
    }
}
