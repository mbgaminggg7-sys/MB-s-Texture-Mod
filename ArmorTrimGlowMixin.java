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
