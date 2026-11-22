package mb.texturemenu.mixin;

import mb.texturemenu.GlowHelper;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * Hooks the "draw this model" calls inside renderLayers. Only the armor-TRIM call has a sprite (argument 7), so only
 * the trim lines get the glow. 1.21.11 signature of the call: submitModel(Model, S, PoseStack, RenderType, int light,
 * int overlay, int color, TextureAtlasSprite, int outline, CrumblingOverlay). It is made through the ordered collector.
 */
@Mixin(EquipmentLayerRenderer.class)
public class TrimEmissiveMixin {
    @ModifyArgs(method = "renderLayers", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/OrderedSubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"),
            require = 0)
    private void mb$trimOrdered(Args args) {
        GlowHelper.modifyTrimArgs(args);
    }

    @ModifyArgs(method = "renderLayers", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"),
            require = 0)
    private void mb$trimDirect(Args args) {
        GlowHelper.modifyTrimArgs(args);
    }
}
