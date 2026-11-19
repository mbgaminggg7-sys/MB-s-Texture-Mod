package mb.texturemenu;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

/** Works out the animated "glow" for armor trims. */
public final class GlowHelper {
    private static final double PERIOD_MS = 3200.0;   // one full wave
    private static final double STEP = 0.9;           // delay between armor pieces (radians)

    private GlowHelper() {}

    public static int apply(int light, ItemStack stack) {
        float g = MBConfig.get().glow;
        if (g <= 0f) return light;

        int rank = rankOf(stack); // boots 0 -> helmet 3
        double t = (System.currentTimeMillis() % (long) PERIOD_MS) / PERIOD_MS * Math.PI * 2.0;
        double wave = 0.5 + 0.5 * Math.sin(t - rank * STEP); // peaks later on higher pieces = wave climbs up
        // always a little glow (0.35), wave adds up to the rest. 0.85 keeps it from getting too bright.
        float amount = (float) (g * (0.35 + 0.65 * wave) * 0.85);
        return lerpToFullBright(light, amount);
    }

    private static int rankOf(ItemStack stack) {
        var eq = stack.get(DataComponents.EQUIPPABLE);
        if (eq == null) return 0;
        return switch (eq.slot()) {
            case FEET -> 0;
            case LEGS -> 1;
            case CHEST -> 2;
            case HEAD -> 3;
            default -> 0;
        };
    }

    private static int lerpToFullBright(int light, float f) {
        int block = LightTexture.block(light);
        int sky = LightTexture.sky(light);
        block = Math.round(block + (15 - block) * f);
        sky = Math.round(sky + (15 - sky) * f);
        return LightTexture.pack(block, sky);
    }
}
