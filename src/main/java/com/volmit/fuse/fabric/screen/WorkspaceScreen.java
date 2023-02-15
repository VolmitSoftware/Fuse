//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.volmit.fuse.fabric.screen;

import com.mojang.logging.LogUtils;
import com.volmit.fuse.fabric.Fuse;
import com.volmit.fuse.fabric.management.data.Project;
import com.volmit.fuse.fabric.screen.widget.WorkspaceListWidget;
import javafx.application.Platform;
import javafx.stage.DirectoryChooser;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.world.gen.GeneratorOptions;
import org.slf4j.Logger;

import java.io.File;
import java.util.Optional;

@Environment(EnvType.CLIENT)
public class WorkspaceScreen extends Screen {
    public static final GeneratorOptions DEBUG_GENERATOR_OPTIONS = new GeneratorOptions("test1".hashCode(), true, false);
    private static final Logger LOGGER = LogUtils.getLogger();
    protected final Screen parent;
    protected TextFieldWidget searchBox;
    private ButtonWidget unlinkButton;
    private ButtonWidget launchButton;
    private ButtonWidget editButton;
    private ButtonWidget settingsButton;
    private WorkspaceListWidget levelList;

    public WorkspaceScreen(Screen parent) {
        super(Text.translatable("Workspace"));
        this.parent = parent;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    public void tick() {
        this.searchBox.tick();
    }

    protected void init() {
        Fuse.log("WIT?");
        this.searchBox = new TextFieldWidget(this.textRenderer, this.width / 2 - 100, 22, 200, 20, this.searchBox, Text.translatable("selectWorld.search"));
        this.searchBox.setChangedListener((search) -> {
            this.levelList.setSearch(search);
        });
        this.levelList = new WorkspaceListWidget(this, this.client, this.width, this.height, 48, this.height - 64, 36, this.searchBox.getText(), this.levelList);
        this.addSelectableChild(this.searchBox);
        this.addSelectableChild(this.levelList);
        this.launchButton = this.addDrawableChild(ButtonWidget.builder(Text.translatable("Launch"), (button) -> {
            // TODO: LAUNCH
        }).dimensions(this.width / 2 - 154, this.height - 52, 150, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("Add Project"), (button) -> {
            Platform.runLater(() -> {
                File directory = chooseDirectory();
                Project project = new Project(directory.getAbsolutePath());
                Fuse.service.getWorkspace().getProjects().add(project);
                Fuse.log("Added project " + project.getName());
            });
        }).dimensions(this.width / 2 + 4, this.height - 52, 150, 20).build());
        this.editButton = this.addDrawableChild(ButtonWidget.builder(Text.of("Edit"), (button) -> {
            // TODO EDIT
        }).dimensions(this.width / 2 - 154, this.height - 28, 72, 20).build());
        this.unlinkButton = this.addDrawableChild(ButtonWidget.builder(Text.of("Unlink"), (button) -> {
            Fuse.log("Unlinking project");
            getSelectedAsOptional().ifPresent((entry) -> {
                Fuse.log("Unlinked project " + entry.getProject().getName());
                Fuse.service.getWorkspace().getProjects().remove(entry.getProject());
                close();
                client.setScreen(new WorkspaceScreen(parent));
            });
        }).dimensions(this.width / 2 - 76, this.height - 28, 72, 20).build());
        this.settingsButton = this.addDrawableChild(ButtonWidget.builder(Text.of("Settings"), (button) -> {
            // TODO: SETTINGS
        }).dimensions(this.width / 2 + 4, this.height - 28, 72, 20).build());
        this.addDrawableChild(ButtonWidget.builder(ScreenTexts.CANCEL, (button) -> {
            this.client.setScreen(this.parent);
        }).dimensions(this.width / 2 + 82, this.height - 28, 72, 20).build());
        this.worldSelected(false);
        this.setInitialFocus(this.searchBox);
    }

    private File chooseDirectory() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Project Directory");
        return chooser.showDialog(null);
    }

    private Optional<WorkspaceListWidget.WorkspaceEntry> getSelectedAsOptional() {
        return levelList.getSelectedAsOptional();
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return super.keyPressed(keyCode, scanCode, modifiers) || this.searchBox.keyPressed(keyCode, scanCode, modifiers);
    }

    public void close() {
        this.client.setScreen(this.parent);
    }

    public boolean charTyped(char chr, int modifiers) {
        return this.searchBox.charTyped(chr, modifiers);
    }

    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.levelList.render(matrices, mouseX, mouseY, delta);
        this.searchBox.render(matrices, mouseX, mouseY, delta);
        drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, 8, 16777215);
        super.render(matrices, mouseX, mouseY, delta);
    }

    public void worldSelected(boolean active) {
        this.launchButton.active = active;
        this.unlinkButton.active = active;
        this.editButton.active = active;
    }

    public void removed() {

    }

    public void workspaceSelected(boolean b) {
        this.launchButton.active = b;
        this.unlinkButton.active = b;
        this.editButton.active = b;
    }
}
