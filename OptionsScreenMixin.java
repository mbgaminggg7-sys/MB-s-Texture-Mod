package mb.texturemenu.mixin;

import mb.texturemenu.gui.MBSecretScreen;
import mb.texturemenu.gui.MBTextureScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds the "MB's Texture Menu" button. Normal click = menu, Alt + click = secret menu. */
@Mixin(OptionsScreen.class)
public abstract class OptionsScreenMixin extends Screen {
    protected OptionsScreenMixin(Component title) { super(title); }

    @Inject(method = "init", at = @At("TAIL"), require = 0)
    private void mb$addTextureButton(CallbackInfo ci) {
        this.addRenderableWidget(Button.builder(Component.literal("MB's Texture Menu"),
                        b -> this.minecraft.setScreen(Screen.hasAltDown()
                                ? new MBSecretScreen(this)
                                : new MBTextureScreen(this)))
                .bounds(6, 6, 130, 20).build());
    }
}
