package mb.texturemenu.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import mb.texturemenu.GlowHelper;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Wraps the "draw this model" call inside renderLayers. Only the armor-TRIM call has a sprite, so only the trim lines
 * get the glow (full-bright light + white pulse wave). 1.21.11 signature of the call:
 * submitModel(Model, S, PoseStack, RenderType, int light, int overlay, int color, TextureAtlasSprite, int outline, CrumblingOverlay)
 */
@Mixin(EquipmentLayerRenderer.class)
public class TrimEmissiveMixin {
    @WrapOperation(method = "renderLayers", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/OrderedSubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"),
            require = 0)
    private void mb$trimOrdered(OrderedSubmitNodeCollector self, Model<?> model, Object state, PoseStack poseStack,
                                RenderType renderType, int light, int overlay, int color, TextureAtlasSprite sprite,
                                int outline, ModelFeatureRenderer.CrumblingOverlay crumbling, Operation<Void> original) {
        int[] r = GlowHelper.adjust(sprite, light, overlay);
        original.call(self, model, state, poseStack, renderType, r[0], r[1], color, sprite, outline, crumbling);
    }

    @WrapOperation(method = "renderLayers", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"),
            require = 0)
    private void mb$trimDirect(SubmitNodeCollector self, Model<?> model, Object state, PoseStack poseStack,
                               RenderType renderType, int light, int overlay, int color, TextureAtlasSprite sprite,
                               int outline, ModelFeatureRenderer.CrumblingOverlay crumbling, Operation<Void> original) {
        int[] r = GlowHelper.adjust(sprite, light, overlay);
        original.call(self, model, state, poseStack, renderType, r[0], r[1], color, sprite, outline, crumbling);
    }
}
