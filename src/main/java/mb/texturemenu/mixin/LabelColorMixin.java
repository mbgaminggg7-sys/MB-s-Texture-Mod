package mb.texturemenu.mixin;

import mb.texturemenu.MBConfig;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** In dark mode the inventory title text (dark gray) would be unreadable, so make it light. */
@Mixin(AbstractContainerScreen.class)
public class LabelColorMixin {
    @ModifyConstant(method = "renderLabels", constant = @Constant(intValue = 4210752), require = 0)
    private int mb$labelColorOld(int original) {
        return MBConfig.get().inventoryDark ? 0xFFD0D0D0 : original;
    }

    @ModifyConstant(method = "renderLabels", constant = @Constant(intValue = -12566464), require = 0)
    private int mb$labelColorArgb(int original) {
        return MBConfig.get().inventoryDark ? 0xFFD0D0D0 : original;
    }
}
