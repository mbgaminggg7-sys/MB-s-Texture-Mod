package mb.texturemenu;

import mb.texturemenu.MBConfig.GlowMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

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
    private static int diag = 0;

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
     * Called for every "draw a model" call while armor is drawn. Only the trim call carries a texture sprite, so
     * everything else is left alone. Returns {light, overlay} to use for that call.
     */
    public static int[] adjust(Object sprite, int light, int overlay) {
        int[] out = {light, overlay};
        if (!wearing) return out;
        MBConfig c = MBConfig.get();
        if (diag < 4) {                                   // a few lines in latest.log to see what the game is doing
            diag++;
            MBTextureMenuClient.LOG.info("[diag] draw call while wearing armor: hasSprite={}, mode={}", sprite != null, c.glowMode);
        }
        if (c.glowMode != GlowMode.TRIMS || c.glowStrength <= 0f || sprite == null) return out;

        int rank = slot == null ? 0 : switch (slot) {
            case FEET -> 0;
            case LEGS -> 1;
            case CHEST -> 2;
            case HEAD -> 3;
            default -> 0;
        };
        float s = c.glowStrength;
        long period = (long) (6000 - 5600 * c.glowSpeed);                  // slow 6s ... fast 0.4s per wave
        double t = (System.currentTimeMillis() % period) / (double) period * Math.PI * 2.0;
        double wave = 0.5 + 0.5 * Math.sin(t - rank * STEP);               // higher pieces peak later = wave climbs up

        out[0] = lerpToFullBright(light, Math.min(1f, s));                 // always glowing in the dark
        if (overlay == OverlayTexture.NO_OVERLAY) {                        // leave the red hurt-flash alone
            int u = (int) Math.round(wave * Math.min(14.0, s * 4.0));      // pulse: trims wash toward white
            if (u > 0) out[1] = OverlayTexture.pack(u, 10);
        }
        if (!logged) {
            logged = true;
            MBTextureMenuClient.LOG.info("Trim glow is ACTIVE (trim draw call hooked)");
        }
        return out;
    }

    private static int lerpToFullBright(int light, float f) {
        int block = LightTexture.block(light);
        int sky = LightTexture.sky(light);
        block = Math.round(block + (15 - block) * f);
        sky = Math.round(sky + (15 - sky) * f);
        return LightTexture.pack(block, sky);
    }
}
