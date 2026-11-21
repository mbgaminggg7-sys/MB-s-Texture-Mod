package mb.texturemenu;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

/**
 * Armor never glows in vanilla, so we borrow the ENCHANTMENT GLINT (the shimmering animated overlay).
 * While a worn armor piece is being drawn, ItemStack.hasFoil() is told to answer "yes".
 * Speed and strength are the game's own Glint Speed / Glint Strength settings (controlled from our menu).
 */
public final class GlowHelper {
    private static boolean wearing = false;
    private static boolean logged = false;

    private GlowHelper() {}

    public static void begin() { wearing = true; }

    public static void end() { wearing = false; }

    /** Should this stack show the glint right now? Only true while worn armor is being rendered. */
    public static boolean shouldFoil(ItemStack stack) {
        if (!wearing || stack == null) return false;
        boolean yes = switch (MBConfig.get().glowMode) {
            case OFF -> false;
            case TRIMS -> stack.get(DataComponents.TRIM) != null;
            case ALL -> true;
        };
        if (yes && !logged) {
            logged = true;
            MBTextureMenuClient.LOG.info("Armor glow is ACTIVE (glint forced on worn armor)");
        }
        return yes;
    }
}
