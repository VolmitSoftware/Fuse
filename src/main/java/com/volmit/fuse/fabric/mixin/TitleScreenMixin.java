package com.volmit.fuse.fabric.mixin;

import com.volmit.fuse.fabric.Fuse;
import com.volmit.fuse.fabric.screen.widget.FuseProgressWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConnectScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class TitleScreenMixin extends Screen {
    protected TitleScreenMixin(Text title) {
        super(title);
    }

    @Inject(at = @At("RETURN"), method = "initWidgetsNormal")
    private void addCustomButton(int y, int spacingY, CallbackInfo ci) {
        this.addDrawable(new FuseProgressWidget(7, 7, width - 14, 9));
        this.addDrawableChild(ButtonWidget.builder(Text.of("✦"), (button) -> {
            if (Fuse.service.isReady()) {
                ServerAddress address = ServerAddress.parse("localhost:24627");
                ConnectScreen.connect(this, this.client, address, new ServerInfo("Fuse Server", address.toString(), false));
            } else {
                MinecraftClient.getInstance().getToastManager().add(
                        new SystemToast(SystemToast.Type.UNSECURE_SERVER_WARNING, Text.of("Fuse"), Text.of("Fuse is not ready yet!")));
            }
        }).dimensions(this.width / 2 + 104, y + spacingY, 20, 20).build());
    }
}
