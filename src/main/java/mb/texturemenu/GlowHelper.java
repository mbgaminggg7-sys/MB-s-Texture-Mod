package mb.texturemenu;

import mb.texturemenu.MBConfig.GlowMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * Armor glow.
 *  TRIMS   - only the trim lines: drawn full-bright (glows in the dark) and pulsing whiter in a wave that climbs
 *            boots -> leggings -> chestplate -> helmet. Strength 0-250%, speed = how fast the wave runs.
 *  SHIMMER - the animated enchantment glint on the whole armor (uses the game's Glint Strength / Speed).
 */
public final class GlowHelper {
    private static final double STEP = 0.9;           // delay between armor pieces (radians)
    private static boolean wearing = false;
    private static EquipmentSlot slot = null;
    private static boolean logged = false;

    private GlowHelper() {}

    public static void begin() { wearing = true; }

    public static void end() { wearing = false; slot = null; }

    public static void setSlot(EquipmentSlot s) { slot = s; }

    // ------------------------------------------------------------------ SHIMMER (whole armor)

    public static boolean shouldFoil(ItemStack stack) {
        return wearing && stack != null && MBConfig.get().glowMode == GlowMode.SHIMMER;
    }

    /** Pushes our strength/speed to the game's glint settings (only used by SHIMMER mode). */
    public static void syncGlint() {
        MBConfig c = MBConfig.get();
        if (c.glowMode != GlowMode.SHIMMER) return;
        var options = Minecraft.getInstance().options;
        options.glintStrength().set((double) Math.min(1f, c.glowStrength));
        options.glintSpeed().set((double) c.glowSpeed);
    }

    // ------------------------------------------------------------------ TRIMS (trim lines only)

    /**
     * Called for every "draw a model" call while armor is drawn. Only the trim call carries a texture sprite
     * (argument 7), so everything else is left alone. Args: 4 = light, 5 = overlay.
     */
    public static void modifyTrimArgs(Args args) {
        MBConfig c = MBConfig.get();
        if (!wearing || c.glowMode != GlowMode.TRIMS || c.glowStrength <= 0f) return;
        if (args.size() < 8) return;
        Object sprite = args.get(7);
        if (sprite == null) return;

        int rank = slot == null ? 0 : switch (slot) {
            case FEET -> 0;
            case LEGS -> 1;
            case CHEST -> 2;
            case HEAD -> 3;
            default -> 0;
        };
        float s = c.glowStrength;
        long period = (long) (7000 - 6200 * c.glowSpeed);                  // slow 7s ... fast 0.8s per wave
        double t = (System.currentTimeMillis() % period) / (double) period * Math.PI * 2.0;
        double wave = 0.5 + 0.5 * Math.sin(t - rank * STEP);               // higher pieces peak later = wave climbs up

        int light = args.get(4);
        args.set(4, lerpToFullBright(light, Math.min(1f, s)));             // always glowing in the dark

        int overlay = args.get(5);
        if (overlay == OverlayTexture.NO_OVERLAY) {                        // leave the red hurt-flash alone
            int u = (int) Math.round(wave * Math.min(10.0, s * 4.0));      // pulse: trims wash toward white
            if (u > 0) args.set(5, OverlayTexture.pack(u, 10));
        }
        if (!logged) {
            logged = true;
            MBTextureMenuClient.LOG.info("Trim glow is ACTIVE (trim submit hooked)");
        }
    }

    private static int lerpToFullBright(int light, float f) {
        int block = LightTexture.block(light);
        int sky = LightTexture.sky(light);
        block = Math.round(block + (15 - block) * f);
        sky = Math.round(sky + (15 - sky) * f);
        return LightTexture.pack(block, sky);
    }
}
