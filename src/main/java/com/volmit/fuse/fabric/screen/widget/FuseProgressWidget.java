package com.volmit.fuse.fabric.screen.widget;

import com.volmit.fuse.fabric.Fuse;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.Element;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public class FuseProgressWidget extends DrawableHelper implements Drawable, Element {
    private final int x;
    private final int y;
    private final int width;
    private final int height;

    public FuseProgressWidget(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public double getProgress() {
        return Fuse.service.getExecutor().getProgress();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        if (getProgress() >= 1 || getProgress() <= 0) {
            return;
        }

        fill(matrices, x, y, x + width, y + height, 0xff293445);
        fill(matrices, x, y, x + ((int) (getProgress() * width)), y + height, 0xff4287f5);
        matrices.scale(0.75f, 0.75f, 0.75f);
        drawTextWithShadow(matrices, MinecraftClient.getInstance().textRenderer, Text.of(Fuse.lastLog), x + 4, y + 4, 0xffffffff);
        matrices.scale(1f / 0.75f, 1f / 0.75f, 1f / 0.75f);
    }
}
