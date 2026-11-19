package mb.texturemenu.pack;

import mb.texturemenu.MBConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.PackRepository;

import java.util.ArrayList;
import java.util.List;

/** Turns the generated packs on/off to match the config, then reloads resources. */
public final class PackManager {
    private static final String PREFIX = "file/MB ";

    private PackManager() {}

    public static void apply(Minecraft mc) {
        PackGenerator.ensureAll(mc);
        MBConfig cfg = MBConfig.get();
        PackRepository repo = mc.getResourcePackRepository();
        repo.reload(); // pick up the zips we just wrote

        List<String> selected = new ArrayList<>(repo.getSelectedIds());
        selected.removeIf(id -> id.startsWith(PREFIX));

        if (cfg.inventoryDark) selected.add("file/" + PackGenerator.INVENTORY_DARK);
        switch (cfg.netherite) {
            case DARK -> selected.add("file/" + PackGenerator.NETHERITE_DARK);
            case LIGHT -> selected.add("file/" + PackGenerator.NETHERITE_LIGHT);
            default -> {}
        }

        repo.setSelected(selected);
        mc.options.updateResourcePacks(repo);
        mc.reloadResourcePacks();
    }
}
