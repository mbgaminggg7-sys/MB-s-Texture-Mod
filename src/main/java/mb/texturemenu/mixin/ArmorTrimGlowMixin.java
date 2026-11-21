package mb.texturemenu.mixin;

import mb.texturemenu.GlowHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;

/**
 * Marks the time during which a worn armor piece is being drawn. Matched by method NAME only (no signature),
 * so it keeps working even if Mojang changes the parameters.
 */
@Mixin(HumanoidArmorLayer.class)
public class ArmorTrimGlowMixin {
    @Inject(method = "renderArmorPiece", at = @At("HEAD"), require = 0)
    private void mb$begin(CallbackInfo ci) {
        GlowHelper.begin();
    }

    @Inject(method = "renderArmorPiece", at = @At("RETURN"), require = 0)
    private void mb$end(CallbackInfo ci) {
        GlowHelper.end();
    }
}
