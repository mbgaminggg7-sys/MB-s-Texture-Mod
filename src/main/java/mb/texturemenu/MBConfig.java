package mb.texturemenu;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public class MBConfig {
    public enum NetheriteMode { NORMAL, DARK, LIGHT }

    /** OFF, TRIMS = neon + animated + glowing trims, SHIMMER = enchantment-style shimmer on the armor, BOTH = both. */
    public enum GlowMode {
        OFF, TRIMS, SHIMMER, BOTH;
        public boolean usesTrims() { return this == TRIMS || this == BOTH; }
        public boolean usesShimmer() { return this == SHIMMER || this == BOTH; }
    }

    public enum SecretTheme { OFF, GOLD, EMERALD, RED, MB, RAINBOW, DIAMOND }

    /** Totem of Undying + shield. */
    public enum ExtraMode { NORMAL, DARK, LIGHT, DIAMOND }

    /** Dark inventory + hotbar + XP bar background. */
    public boolean inventoryDark = false;
    public NetheriteMode netherite = NetheriteMode.NORMAL;
    /** Netherite tools: sword, pickaxe, axe, shovel, hoe, spear, mace (no shears). */
    public NetheriteMode tools = NetheriteMode.NORMAL;
    public NetheriteMode diamondArmor = NetheriteMode.NORMAL;
    public NetheriteMode diamondTools = NetheriteMode.NORMAL;
    public ExtraMode totemShield = ExtraMode.NORMAL;
    public GlowMode glowMode = GlowMode.BOTH;
    /** 0.0 - 10.0  (0% - 1000%). Higher = brighter neon, stronger wave, stronger shimmer. */
    public float glowStrength = 10.0f;
    public int cfgVersion = 0;
    /** 0.0 - 1.0, how fast the wave runs. */
    public float glowSpeed = 0.7f;
    public SecretTheme secretTheme = SecretTheme.OFF;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("mb_texture_menu.json");
    private static MBConfig instance;

    public static MBConfig get() {
        if (instance == null) instance = load();
        return instance;
    }

    private static MBConfig load() {
        try {
            if (Files.exists(FILE)) {
                MBConfig c = GSON.fromJson(Files.readString(FILE), MBConfig.class);
                if (c != null) return c.sanitize();
            }
        } catch (Exception e) {
            MBTextureMenuClient.LOG.warn("Could not read config, using defaults", e);
        }
        return new MBConfig();
    }

    private MBConfig sanitize() {
        if (netherite == null) netherite = NetheriteMode.NORMAL;
        if (tools == null) tools = NetheriteMode.NORMAL;
        if (diamondArmor == null) diamondArmor = NetheriteMode.NORMAL;
        if (diamondTools == null) diamondTools = NetheriteMode.NORMAL;
        if (totemShield == null) totemShield = ExtraMode.NORMAL;
        if (glowMode == null) glowMode = GlowMode.TRIMS;
        if (cfgVersion < 3) { glowStrength = 10.0f; glowSpeed = 0.7f; glowMode = GlowMode.BOTH; cfgVersion = 3; }   // new defaults
        glowStrength = Math.max(0f, Math.min(10f, glowStrength));
        glowSpeed = Math.max(0f, Math.min(1f, glowSpeed));
        if (secretTheme == null) secretTheme = SecretTheme.OFF;
        return this;
    }

    /** Anything that changes which resource packs are on. */
    public String packKey() {
        return inventoryDark + "|" + netherite + "|" + tools + "|" + diamondArmor + "|" + diamondTools + "|" + totemShield + "|" + secretTheme + "|" + (glowMode.usesTrims() ? "neon" + neonLevel() + "s" + speedBucket() : "-")
                + "|" + (glowMode.usesShimmer() ? "shim" + neonLevel() : "-");
    }

    /** 1-5: how bright the neon trims, the animated wave and the shimmer are (changes when you press Done). */
    public int neonLevel() {
        return glowStrength < 1.5f ? 1 : glowStrength < 3.5f ? 2 : glowStrength < 6f ? 3 : glowStrength < 8.5f ? 4 : 5;
    }

    /** 0 = slow, 1 = normal, 2 = fast: the speed of the animated trim pack (applies when you press Done). */
    public int speedBucket() {
        return glowSpeed < 0.34f ? 0 : glowSpeed < 0.67f ? 1 : 2;
    }

    /** Inventory title text must be light on dark backgrounds. */
    public boolean lightLabels() {
        return inventoryDark || secretTheme == SecretTheme.RED;
    }

    public void save() {
        try {
            Files.writeString(FILE, GSON.toJson(this));
        } catch (Exception e) {
            MBTextureMenuClient.LOG.warn("Could not save config", e);
        }
    }
}
