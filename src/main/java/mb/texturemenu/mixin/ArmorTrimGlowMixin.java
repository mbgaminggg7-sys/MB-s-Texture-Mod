package mb.texturemenu.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import mb.texturemenu.GlowHelper;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Marks the time during which a worn armor piece is being drawn (matched by method NAME only), and remembers
 * which slot it is so the glow wave can climb boots -> helmet.
 */
@Mixin(HumanoidArmorLayer.class)
public class ArmorTrimGlowMixin {
    @Inject(method = "renderArmorPiece", at = @At("HEAD"), require = 0)
    private void mb$begin(CallbackInfo ci) {
        GlowHelper.begin();
    }

    @Inject(method = "renderArmorPiece", at = @At("HEAD"), require = 0)
    private void mb$slot(CallbackInfo ci, @Local(argsOnly = true) EquipmentSlot slot) {
        GlowHelper.setSlot(slot);
    }

    @Inject(method = "renderArmorPiece", at = @At("RETURN"), require = 0)
    private void mb$end(CallbackInfo ci) {
        GlowHelper.end();
    }
}
