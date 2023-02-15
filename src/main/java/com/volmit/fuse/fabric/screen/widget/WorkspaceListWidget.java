//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.volmit.fuse.fabric.screen.widget;

import com.mojang.logging.LogUtils;
import com.volmit.fuse.fabric.Fuse;
import com.volmit.fuse.fabric.management.data.Project;
import com.volmit.fuse.fabric.screen.WorkspaceScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.FatalErrorScreen;
import net.minecraft.client.gui.screen.LoadingDisplay;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Environment(EnvType.CLIENT)
public class WorkspaceListWidget extends AlwaysSelectedEntryListWidget<WorkspaceListWidget.Entry> {
    static final Logger LOGGER = LogUtils.getLogger();
    static final DateFormat DATE_FORMAT = new SimpleDateFormat();
    static final Identifier UNKNOWN_SERVER_LOCATION = new Identifier("textures/misc/unknown_server.png");
    static final Identifier WORLD_SELECTION_LOCATION = new Identifier("textures/gui/world_selection.png");
    static final Text FROM_NEWER_VERSION_FIRST_LINE;
    static final Text FROM_NEWER_VERSION_SECOND_LINE;
    static final Text SNAPSHOT_FIRST_LINE;
    static final Text SNAPSHOT_SECOND_LINE;
    static final Text LOCKED_TEXT;
    static final Text CONVERSION_TOOLTIP;

    static {
        FROM_NEWER_VERSION_FIRST_LINE = Text.translatable("selectWorld.tooltip.fromNewerVersion1").formatted(Formatting.RED);
        FROM_NEWER_VERSION_SECOND_LINE = Text.translatable("selectWorld.tooltip.fromNewerVersion2").formatted(Formatting.RED);
        SNAPSHOT_FIRST_LINE = Text.translatable("selectWorld.tooltip.snapshot1").formatted(Formatting.GOLD);
        SNAPSHOT_SECOND_LINE = Text.translatable("selectWorld.tooltip.snapshot2").formatted(Formatting.GOLD);
        LOCKED_TEXT = Text.translatable("selectWorld.locked").formatted(Formatting.RED);
        CONVERSION_TOOLTIP = Text.translatable("selectWorld.conversion.tooltip").formatted(Formatting.RED);
    }

    private final WorkspaceScreen parent;
    private final LoadingEntry loadingEntry;
    @Nullable
    private List<Project> levels;
    private String search;

    public WorkspaceListWidget(WorkspaceScreen parent, MinecraftClient client, int width, int height, int top, int bottom, int itemHeight, String search, @Nullable WorkspaceListWidget oldWidget) {
        super(client, width, height, top, bottom, itemHeight);
        this.parent = parent;
        this.loadingEntry = new LoadingEntry(client);
        this.search = search;
        this.show(this.tryGet());
    }

    @Nullable
    private List<Project> tryGet() {
        return Fuse.service.getWorkspace().getProjects();
    }

    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        List<Project> list = this.tryGet();
        if (list != this.levels) {
            this.show(list);
        }

        super.render(matrices, mouseX, mouseY, delta);
    }

    private void show(@Nullable List<Project> levels) {
        if (levels == null) {
            this.showLoadingScreen();
        } else {
            this.showSummaries(this.search, levels);
        }

        this.levels = levels;
    }

    public void setSearch(String search) {
        if (this.levels != null && !search.equals(this.search)) {
            this.showSummaries(search, this.levels);
        }

        this.search = search;
    }

    private void showSummaries(String search, List<Project> summaries) {
        this.clearEntries();
        search = search.toLowerCase(Locale.ROOT);

        for (Project levelSummary : summaries) {
            if (this.shouldShow(search, levelSummary)) {
                this.addEntry(new WorkspaceEntry(this, levelSummary));
            }
        }

        this.narrateScreenIfNarrationEnabled();
    }

    private boolean shouldShow(String search, Project summary) {
        return summary.getLocation().toLowerCase(Locale.ROOT).contains(search) || summary.getName().toLowerCase(Locale.ROOT).contains(search);
    }

    private void showLoadingScreen() {
        this.clearEntries();
        this.addEntry(this.loadingEntry);
        this.narrateScreenIfNarrationEnabled();
    }

    private void narrateScreenIfNarrationEnabled() {
        this.parent.narrateScreenIfNarrationEnabled(true);
    }

    private void showUnableToLoadScreen(Text message) {
        this.client.setScreen(new FatalErrorScreen(Text.translatable("selectWorld.unable_to_load"), message));
    }

    protected int getScrollbarPositionX() {
        return super.getScrollbarPositionX() + 20;
    }

    public int getRowWidth() {
        return super.getRowWidth() + 50;
    }

    protected boolean isFocused() {
        return this.parent.getFocused() == this;
    }

    public void setSelected(@Nullable Entry entry) {
        super.setSelected(entry);
        this.parent.workspaceSelected(entry != null && entry.isAvailable());
    }

    protected void moveSelection(EntryListWidget.MoveDirection direction) {
        this.moveSelectionIf(direction, Entry::isAvailable);
    }

    public Optional<WorkspaceEntry> getSelectedAsOptional() {
        Fuse.log("Got it!?");
        Entry entry = this.getSelectedOrNull();
        if (entry instanceof WorkspaceEntry workspaceEntry) {
            return Optional.of(workspaceEntry);
        } else {
            return Optional.empty();
        }
    }

    public WorkspaceScreen getParent() {
        return this.parent;
    }

    public void appendNarrations(NarrationMessageBuilder builder) {
        if (this.children().contains(this.loadingEntry)) {
            this.loadingEntry.appendNarrations(builder);
        } else {
            super.appendNarrations(builder);
        }
    }

    @Environment(EnvType.CLIENT)
    public static class LoadingEntry extends Entry {
        private static final Text LOADING_LIST_TEXT = Text.translatable("selectWorld.loading_list");
        private final MinecraftClient client;

        public LoadingEntry(MinecraftClient client) {
            this.client = client;
        }

        public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            int i = (this.client.currentScreen.width - this.client.textRenderer.getWidth(LOADING_LIST_TEXT)) / 2;
            Objects.requireNonNull(this.client.textRenderer);
            int j = y + (entryHeight - 9) / 2;
            this.client.textRenderer.draw(matrices, LOADING_LIST_TEXT, (float) i, (float) j, 16777215);
            String string = LoadingDisplay.get(Util.getMeasuringTimeMs());
            int k = (this.client.currentScreen.width - this.client.textRenderer.getWidth(string)) / 2;
            Objects.requireNonNull(this.client.textRenderer);
            int l = j + 9;
            this.client.textRenderer.draw(matrices, string, (float) k, (float) l, 8421504);
        }

        public Text getNarration() {
            return LOADING_LIST_TEXT;
        }

        public boolean isAvailable() {
            return false;
        }
    }

    @Environment(EnvType.CLIENT)
    public abstract static class Entry extends AlwaysSelectedEntryListWidget.Entry<Entry> implements AutoCloseable {
        public Entry() {
        }

        public abstract boolean isAvailable();

        public void close() {
        }
    }

    @Environment(EnvType.CLIENT)
    public final class WorkspaceEntry extends Entry implements AutoCloseable {
        private static final int field_32435 = 32;
        private static final int field_32436 = 32;
        private static final int field_32437 = 0;
        private static final int field_32438 = 32;
        private static final int field_32439 = 64;
        private static final int field_32440 = 96;
        private static final int field_32441 = 0;
        private static final int field_32442 = 32;
        private final MinecraftClient client;
        private final WorkspaceScreen screen;
        private final Project level;
        @Nullable
        private Path iconPath;
        private long time;


        public WorkspaceEntry(WorkspaceListWidget levelList, Project level) {
            this.client = levelList.client;
            this.screen = levelList.getParent();
            this.level = level;
            String string = level.getName();
            String var10004 = Util.replaceInvalidChars(string, Identifier::isPathCharacterValid);
        }

        public Project getProject() {
            return level;
        }

        public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            String string = this.level.getName();
            String var10000 = this.level.getName();
            if (StringUtils.isEmpty(string)) {
                var10000 = I18n.translate("selectWorld.world");
                string = var10000 + " " + (index + 1);
            }

            this.client.textRenderer.draw(matrices, string, (float) (x + 32 + 3), (float) (y + 1), 16777215);
            TextRenderer var17 = this.client.textRenderer;
            float var10003 = (float) (x + 32 + 3);
            Objects.requireNonNull(this.client.textRenderer);
            var17.draw(matrices, Text.of(level.getLocation()), var10003, (float) (y + 9 + 3), 8421504);
            var10003 = (float) (x + 32 + 3);
            Objects.requireNonNull(this.client.textRenderer);
            int var10004 = y + 9;
            Objects.requireNonNull(this.client.textRenderer);
            var17.draw(matrices, Text.of(level.getMain()), var10003, (float) (var10004 + 9 + 3), 8421504);
        }

        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            WorkspaceListWidget.this.setSelected(this);
            this.screen.worldSelected(WorkspaceListWidget.this.getSelectedAsOptional().isPresent());
            if (mouseX - (double) WorkspaceListWidget.this.getRowLeft() <= 32.0) {
                this.play();
                return true;
            } else if (Util.getMeasuringTimeMs() - this.time < 250L) {
                this.play();
                return true;
            } else {
                this.time = Util.getMeasuringTimeMs();
                return false;
            }
        }

        @Override
        public boolean isAvailable() {
            return false;
        }

        public void close() {

        }

        @Override
        public Text getNarration() {
            return Text.of("");
        }

        public void play() {
        }

        public void edit() {
        }

        public void deleteIfConfirmed() {
        }

        public void recreate() {
        }
    }
}
