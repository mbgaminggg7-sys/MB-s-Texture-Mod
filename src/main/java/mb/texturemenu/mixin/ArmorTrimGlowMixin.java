package mb.texturemenu.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import mb.texturemenu.GlowHelper;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Makes ONLY the armor trim layer glow (and wave). The trim is the part drawn through
 * TextureAtlasSprite.wrap(...), so we flag that moment and brighten the light value for it.
 *
 * NOTE: these targets are written for 1.21.11 and may need a tweak - see README.
 */
@Mixin(EquipmentLayerRenderer.class)
public class ArmorTrimGlowMixin {
    @Unique private static ItemStack mb$stack = ItemStack.EMPTY;
    @Unique private static boolean mb$inTrim = false;

    @Inject(method = "renderLayers", at = @At("HEAD"), require = 0)
    private void mb$begin(CallbackInfo ci, @Local(argsOnly = true) ItemStack stack) {
        mb$stack = stack;
        mb$inTrim = false;
    }

    @Inject(method = "renderLayers",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;wrap(Lcom/mojang/blaze3d/vertex/VertexConsumer;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"),
            require = 0)
    private void mb$enterTrim(CallbackInfo ci) {
        mb$inTrim = true;
    }

    @Inject(method = "renderLayers", at = @At("RETURN"), require = 0)
    private void mb$end(CallbackInfo ci) {
        mb$inTrim = false;
        mb$stack = ItemStack.EMPTY;
    }

    @ModifyArg(method = "renderLayers",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/model/Model;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V"),
            index = 2, require = 0)
    private int mb$glowLight(int light) {
        return mb$inTrim ? GlowHelper.apply(light, mb$stack) : light;
    }
}
