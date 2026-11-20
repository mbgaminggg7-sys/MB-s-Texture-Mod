package mb.texturemenu.mixin;

import mb.texturemenu.gui.MBSecretScreen;
import mb.texturemenu.gui.MBTextureScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
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
                        b -> this.minecraft.setScreen(mb$altDown()
                                ? new MBSecretScreen(this)
                                : new MBTextureScreen(this)))
                .bounds(6, 6, 130, 20).build());
    }

    /** Reads the Alt key straight from GLFW (works no matter how Minecraft names its input classes). */
    private static boolean mb$altDown() {
        long window = GLFW.glfwGetCurrentContext();
        if (window == 0L) return false;
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS;
    }
}

package mb.texturemenu.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import mb.texturemenu.GlowHelper;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Brightens the light value of any worn armor piece that HAS A TRIM, with a wave that climbs
 * boots -> leggings -> chestplate -> helmet.
 *
 * This hooks only the method name and its single int (the light), so it survives the rendering
 * changes in recent versions (the old version hooked exact internal calls and silently missed).
 */
@Mixin(EquipmentLayerRenderer.class)
public class ArmorTrimGlowMixin {
    @ModifyVariable(method = "renderLayers", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private int mb$glowLight(int light, @Local(argsOnly = true) ItemStack stack) {
        return GlowHelper.hasTrim(stack) ? GlowHelper.apply(light, stack) : light;
    }
}

package mb.texturemenu.mixin;

import mb.texturemenu.MBConfig;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** On dark backgrounds the inventory title text (dark gray) would be unreadable, so make it light. */
@Mixin(AbstractContainerScreen.class)
public class LabelColorMixin {
    @ModifyConstant(method = "renderLabels", constant = @Constant(intValue = 4210752), require = 0)
    private int mb$labelColorOld(int original) {
        return MBConfig.get().lightLabels() ? 0xFFD0D0D0 : original;
    }

    @ModifyConstant(method = "renderLabels", constant = @Constant(intValue = -12566464), require = 0)
    private int mb$labelColorArgb(int original) {
        return MBConfig.get().lightLabels() ? 0xFFD0D0D0 : original;
    }
}
