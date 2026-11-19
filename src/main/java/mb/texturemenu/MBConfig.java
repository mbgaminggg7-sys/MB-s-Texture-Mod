package mb.texturemenu;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public class MBConfig {
    public enum NetheriteMode { NORMAL, DARK, LIGHT }

    public boolean inventoryDark = false;
    public NetheriteMode netherite = NetheriteMode.NORMAL;
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
        glow = Math.max(0f, Math.min(1f, glow));
        return this;
    }

    public void save() {
        try {
            Files.writeString(FILE, GSON.toJson(this));
        } catch (Exception e) {
            MBTextureMenuClient.LOG.warn("Could not save config", e);
        }
    }
}
