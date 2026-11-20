package mb.texturemenu.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import mb.texturemenu.GlowHelper;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Worn armor goes through HumanoidArmorLayer.renderArmorPiece(poseStack, collector, stack, slot, light, state).
 * We wrap it and raise the light value of pieces that HAVE A TRIM, in a wave that climbs
 * boots -> leggings -> chestplate -> helmet.
 */
@Mixin(HumanoidArmorLayer.class)
public class ArmorTrimGlowMixin {
    @WrapMethod(method = "renderArmorPiece")
    private void mb$glow(PoseStack poseStack, SubmitNodeCollector collector, ItemStack stack,
                         EquipmentSlot slot, int light, HumanoidRenderState state, Operation<Void> original) {
        int newLight = GlowHelper.hasTrim(stack) ? GlowHelper.apply(light, slot) : light;
        original.call(poseStack, collector, stack, slot, newLight, state);
    }
}
