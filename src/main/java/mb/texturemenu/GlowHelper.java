package mb.texturemenu;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** Works out the animated "glow" for trimmed armor. */
public final class GlowHelper {
    private static final double STEP = 0.9;           // delay between armor pieces (radians)
    private static boolean logged = false;

    private GlowHelper() {}

    public static int apply(int light, EquipmentSlot slot) {
        float g = MBConfig.get().glow;
        if (g <= 0f) return light;

        int rank = rankOf(slot); // boots 0 -> helmet 3
        long period = MBConfig.get().waveSpeed.ms; // one full wave
        double t = (System.currentTimeMillis() % period) / (double) period * Math.PI * 2.0;
        double wave = 0.5 + 0.5 * Math.sin(t - rank * STEP); // higher pieces peak later = wave climbs up
        // always a little glow (40%), the wave adds the rest; slider scales the whole thing
        float amount = (float) Math.min(1.0, g * 1.4 * (0.4 + 0.6 * wave));
        int out = lerpToFullBright(light, amount);
        if (!logged) {
            logged = true;
            MBTextureMenuClient.LOG.info("Trim glow hook is ACTIVE (light {} -> {})", light, out);
        }
        return out;
    }

    public static boolean hasTrim(ItemStack stack) {
        return stack != null && stack.get(DataComponents.TRIM) != null;
    }

    private static int rankOf(EquipmentSlot slot) {
        return switch (slot) {
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
