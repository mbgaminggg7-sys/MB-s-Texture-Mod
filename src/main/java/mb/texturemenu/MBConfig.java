package mb.texturemenu;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public class MBConfig {
    public enum NetheriteMode { NORMAL, DARK, LIGHT }

    /** OFF, TRIMS = only the trims glow + pulse in a wave, SHIMMER = the enchantment-style shimmer on the whole armor. */
    public enum GlowMode { OFF, TRIMS, SHIMMER }

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
    public GlowMode glowMode = GlowMode.TRIMS;
    /** 0.0 - 2.5  (0% - 250%). Above 100% the pulses get brighter. */
    public float glowStrength = 1.5f;
    /** 0.0 - 1.0, how fast the wave runs. */
    public float glowSpeed = 0.5f;
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
        glowStrength = Math.max(0f, Math.min(2.5f, glowStrength));
        glowSpeed = Math.max(0f, Math.min(1f, glowSpeed));
        if (secretTheme == null) secretTheme = SecretTheme.OFF;
        return this;
    }

    /** Anything that changes which resource packs are on. */
    public String packKey() {
        return inventoryDark + "|" + netherite + "|" + tools + "|" + diamondArmor + "|" + diamondTools + "|" + totemShield + "|" + secretTheme + "|" + (glowMode == GlowMode.TRIMS);
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
