package mb.texturemenu.pack;

import mb.texturemenu.MBConfig;
import mb.texturemenu.MBConfig.ExtraMode;
import mb.texturemenu.MBConfig.NetheriteMode;
import mb.texturemenu.MBConfig.SecretTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.PackRepository;

import java.util.ArrayList;
import java.util.List;

/** Turns the generated packs on/off to match the config, then reloads resources. */
public final class PackManager {
    private static final String PREFIX = "file/MB ";

    private PackManager() {}

    private static void add(List<String> wanted, NetheriteMode m, String dark, String light) {
        if (m == NetheriteMode.DARK) wanted.add(dark);
        else if (m == NetheriteMode.LIGHT) wanted.add(light);
    }

    public static void apply(Minecraft mc) {
        MBConfig cfg = MBConfig.get();

        List<String> wanted = new ArrayList<>();
        if (cfg.inventoryDark) wanted.add(PackGenerator.INVENTORY_DARK);
        add(wanted, cfg.netherite, PackGenerator.NETHERITE_DARK, PackGenerator.NETHERITE_LIGHT);
        add(wanted, cfg.tools, PackGenerator.TOOLS_DARK, PackGenerator.TOOLS_LIGHT);
        add(wanted, cfg.diamondArmor, PackGenerator.DIAMOND_DARK, PackGenerator.DIAMOND_LIGHT);
        add(wanted, cfg.diamondTools, PackGenerator.DIAMOND_TOOLS_DARK, PackGenerator.DIAMOND_TOOLS_LIGHT);
        switch (cfg.totemShield) {
            case DARK -> wanted.add(PackGenerator.TOTEM_DARK);
            case LIGHT -> wanted.add(PackGenerator.TOTEM_LIGHT);
            case DIAMOND -> wanted.add(PackGenerator.TOTEM_DIAMOND);
            default -> {}
        }
        if (cfg.glowMode == MBConfig.GlowMode.TRIMS) wanted.add(PackGenerator.NEON_TRIMS);
        if (cfg.secretTheme != SecretTheme.OFF) wanted.add(PackGenerator.secretName(cfg.secretTheme)); // last = on top

        for (String name : wanted) PackGenerator.ensure(mc, name);

        PackRepository repo = mc.getResourcePackRepository();
        repo.reload(); // pick up the zips we just wrote

        List<String> selected = new ArrayList<>(repo.getSelectedIds());
        selected.removeIf(id -> id.startsWith(PREFIX));
        for (String name : wanted) selected.add("file/" + name);

        repo.setSelected(selected);
        mc.options.updateResourcePacks(repo);
        mc.reloadResourcePacks();
    }
}
