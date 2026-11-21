package mb.texturemenu.mixin;

import mb.texturemenu.GlowHelper;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Makes worn armor show the enchantment glint (only while the armor layer is drawing it). */
@Mixin(ItemStack.class)
public class ItemStackFoilMixin {
    @Inject(method = "hasFoil", at = @At("HEAD"), cancellable = true, require = 0)
    private void mb$foil(CallbackInfoReturnable<Boolean> cir) {
        if (GlowHelper.shouldFoil((ItemStack) (Object) this)) cir.setReturnValue(true);
    }
}
