//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.volmit.fuse.fabric.screen.widget;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Environment(EnvType.CLIENT)
public class FuseToast implements Toast {
    private static final int MIN_WIDTH = 200;
    private static final int LINE_HEIGHT = 12;
    private static final int PADDING_Y = 10;
    private final Type type;
    private final int width;
    private Text title;
    private List<OrderedText> lines;
    private long startTime;
    private boolean justUpdated;

    public FuseToast(Type type, Text title, @Nullable Text description) {
        this(type, title, getTextAsList(description), Math.max(160, 30 + Math.max(MinecraftClient.getInstance().textRenderer.getWidth(title), description == null ? 0 : MinecraftClient.getInstance().textRenderer.getWidth(description))));
    }

    private FuseToast(Type type, Text title, List<OrderedText> lines, int width) {
        this.type = type;
        this.title = title;
        this.lines = lines;
        this.width = width;
    }

    public static FuseToast create(MinecraftClient client, Type type, Text title, Text description) {
        TextRenderer textRenderer = client.textRenderer;
        List<OrderedText> list = textRenderer.wrapLines(description, 200);
        Stream<OrderedText> var10001 = list.stream();
        Objects.requireNonNull(textRenderer);
        int i = Math.max(200, var10001.mapToInt(textRenderer::getWidth).max().orElse(200));
        return new FuseToast(type, title, list, i + 30);
    }

    private static ImmutableList<OrderedText> getTextAsList(@Nullable Text text) {
        return text == null ? ImmutableList.of() : ImmutableList.of(text.asOrderedText());
    }

    public static void add(ToastManager manager, Type type, Text title, @Nullable Text description) {
        manager.add(new FuseToast(type, title, description));
    }

    public static void show(ToastManager manager, Type type, Text title, @Nullable Text description) {
        FuseToast fuseToast = (FuseToast) manager.getToast(FuseToast.class, type);
        if (fuseToast == null) {
            add(manager, type, title, description);
        } else {
            fuseToast.setContent(title, description);
        }

    }

    public static void addWorldAccessFailureToast(MinecraftClient client, String worldName) {
        add(client.getToastManager(), FuseToast.Type.WORLD_ACCESS_FAILURE, Text.translatable("selectWorld.access_failure"), Text.literal(worldName));
    }

    public static void addWorldDeleteFailureToast(MinecraftClient client, String worldName) {
        add(client.getToastManager(), FuseToast.Type.WORLD_ACCESS_FAILURE, Text.translatable("selectWorld.delete_failure"), Text.literal(worldName));
    }

    public static void addPackCopyFailure(MinecraftClient client, String directory) {
        add(client.getToastManager(), FuseToast.Type.PACK_COPY_FAILURE, Text.translatable("pack.copyFailure"), Text.literal(directory));
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return 20 + Math.max(this.lines.size(), 1) * 12;
    }

    public Toast.Visibility draw(MatrixStack matrices, ToastManager manager, long startTime) {
        if (this.justUpdated) {
            this.startTime = startTime;
            this.justUpdated = false;
        }

        RenderSystem.setShaderTexture(0, TEXTURE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int i = this.getWidth();
        int j;
        if (i == 160 && this.lines.size() <= 1) {
            manager.drawTexture(matrices, 0, 0, 0, 64, i, this.getHeight());
        } else {
            j = this.getHeight();
            int l = Math.min(4, j - 28);
            this.drawPart(matrices, manager, i, 0, 0, 28);

            for (int m = 28; m < j - l; m += 10) {
                this.drawPart(matrices, manager, i, 16, m, Math.min(16, j - m - l));
            }

            this.drawPart(matrices, manager, i, 32 - l, j - l, l);
        }

        if (this.lines == null) {
            manager.getClient().textRenderer.draw(matrices, this.title, 18.0F, 12.0F, -256);
        } else {
            manager.getClient().textRenderer.draw(matrices, this.title, 18.0F, 7.0F, -256);

            for (j = 0; j < this.lines.size(); ++j) {
                manager.getClient().textRenderer.draw(matrices, (OrderedText) this.lines.get(j), 18.0F, (float) (18 + j * 12), -1);
            }
        }

        return startTime - this.startTime < this.type.displayDuration ? Visibility.SHOW : Visibility.HIDE;
    }

    private void drawPart(MatrixStack matrices, ToastManager manager, int width, int textureV, int y, int height) {
        int i = textureV == 0 ? 20 : 5;
        int j = Math.min(60, width - i);
        manager.drawTexture(matrices, 0, y, 0, 64 + textureV, i, height);

        for (int k = i; k < width - j; k += 64) {
            manager.drawTexture(matrices, k, y, 32, 64 + textureV, Math.min(64, width - k - j), height);
        }

        manager.drawTexture(matrices, width - j, y, 160 - j, 64 + textureV, j, height);
    }

    public void setContent(Text title, @Nullable Text description) {
        this.title = title;
        this.lines = getTextAsList(description);
        this.justUpdated = true;
    }

    public Type getType() {
        return this.type;
    }

    @Environment(EnvType.CLIENT)
    public static enum Type {
        TUTORIAL_HINT,
        NARRATOR_TOGGLE,
        WORLD_BACKUP,
        WORLD_GEN_SETTINGS_TRANSFER,
        PACK_LOAD_FAILURE,
        WORLD_ACCESS_FAILURE,
        PACK_COPY_FAILURE,
        PERIODIC_NOTIFICATION,
        UNSECURE_SERVER_WARNING(10000L);

        final long displayDuration;

        private Type(long displayDuration) {
            this.displayDuration = displayDuration;
        }

        private Type() {
            this(3000L);
        }
    }
}
