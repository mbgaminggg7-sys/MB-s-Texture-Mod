package mb.texturemenu;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public class MBConfig {
    public enum NetheriteMode { NORMAL, DARK, LIGHT }

    public enum WaveSpeed {
        SLOW(5200L), NORMAL(3200L), FAST(1800L);
        public final long ms;
        WaveSpeed(long ms) { this.ms = ms; }
    }

    public enum SecretTheme { OFF, GOLD, EMERALD, RED, MB }

    /** Dark inventory + hotbar + XP bar background. */
    public boolean inventoryDark = false;
    public NetheriteMode netherite = NetheriteMode.NORMAL;
    /** Netherite tools: sword, pickaxe, axe, shovel, hoe, spear, mace (no shears). */
    public NetheriteMode tools = NetheriteMode.NORMAL;
    public NetheriteMode diamondArmor = NetheriteMode.NORMAL;
    public NetheriteMode diamondTools = NetheriteMode.NORMAL;
    public WaveSpeed waveSpeed = WaveSpeed.NORMAL;
    public SecretTheme secretTheme = SecretTheme.OFF;
    /** 0.0 - 1.0, how strong the trim glow is. */
    public float glow = 0.35f;

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
        if (waveSpeed == null) waveSpeed = WaveSpeed.NORMAL;
        if (secretTheme == null) secretTheme = SecretTheme.OFF;
        glow = Math.max(0f, Math.min(1f, glow));
        return this;
    }

    /** Anything that changes which resource packs are on. */
    public String packKey() {
        return inventoryDark + "|" + netherite + "|" + tools + "|" + diamondArmor + "|" + diamondTools + "|" + secretTheme;
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
