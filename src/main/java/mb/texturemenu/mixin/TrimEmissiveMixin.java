package mb.texturemenu.mixin;

import mb.texturemenu.GlowHelper;
import mb.texturemenu.Targets;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * Hooks the "draw this model" calls inside renderLayers. Only the armor-TRIM call has a sprite, so only the
 * trim lines get the glow (full-bright light + pulse). Two handlers because the call is made either through the
 * ordered collector or the main one - only one of them exists in a given game build.
 */
@Mixin(EquipmentLayerRenderer.class)
public class TrimEmissiveMixin {
    @ModifyArgs(method = "renderLayers", at = @At(value = "INVOKE", target = Targets.ORDERED), require = 0)
    private void mb$trimOrdered(Args args) {
        GlowHelper.modifyTrimArgs(args);
    }

    @ModifyArgs(method = "renderLayers", at = @At(value = "INVOKE", target = Targets.DIRECT), require = 0)
    private void mb$trimDirect(Args args) {
        GlowHelper.modifyTrimArgs(args);
    }
}
