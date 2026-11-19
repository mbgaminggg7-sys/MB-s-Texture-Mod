package mb.texturemenu.pack;

import mb.texturemenu.MBTextureMenuClient;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.VanillaPackResources;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Builds the resource packs from the player's OWN game textures (no assets are shipped with the mod).
 */
public final class PackGenerator {
    public static final String INVENTORY_DARK = "MB Dark Inventory.zip";
    public static final String NETHERITE_DARK = "MB Netherite Dark.zip";
    public static final String NETHERITE_LIGHT = "MB Netherite Light.zip";

    private interface Px { int map(int argb); }

    private PackGenerator() {}

    public static void ensureAll(Minecraft mc) {
        Path dir = mc.getResourcePackDirectory();
        try {
            Files.createDirectories(dir);
            VanillaPackResources vanilla = mc.getVanillaPackResources();

            if (!Files.exists(dir.resolve(INVENTORY_DARK))) {
                build(dir.resolve(INVENTORY_DARK), "MB's Texture Menu - dark inventory", vanilla,
                        List.of("textures/gui/container", "textures/gui/sprites/container"),
                        List.of(), PackGenerator::darkGui);
            }
            if (!Files.exists(dir.resolve(NETHERITE_DARK))) {
                build(dir.resolve(NETHERITE_DARK), "MB's Texture Menu - dark netherite", vanilla,
                        List.of("textures/entity/equipment/humanoid", "textures/entity/equipment/humanoid_leggings"),
                        netheriteItems(), PackGenerator::netheriteDark);
            }
            if (!Files.exists(dir.resolve(NETHERITE_LIGHT))) {
                build(dir.resolve(NETHERITE_LIGHT), "MB's Texture Menu - light netherite", vanilla,
                        List.of("textures/entity/equipment/humanoid", "textures/entity/equipment/humanoid_leggings"),
                        netheriteItems(), PackGenerator::netheriteLight);
            }
        } catch (Exception e) {
            MBTextureMenuClient.LOG.error("Failed to generate texture packs", e);
        }
    }

    private static List<String> netheriteItems() {
        return List.of("textures/item/netherite_helmet.png", "textures/item/netherite_chestplate.png",
                "textures/item/netherite_leggings.png", "textures/item/netherite_boots.png");
    }

    // ---- colour maths ----------------------------------------------------------------------

    private static int darkGui(int argb) { return scale(argb, 0.36f, 8); }

    private static int netheriteDark(int argb) { return scale(argb, 0.36f, 1); }

    /** Netherite -> pale gray-green armor with darker teal shadows (stretches contrast, keeps shading). */
    private static int netheriteLight(int argb) {
        int a = argb >>> 24;
        int c = (((argb >> 16) & 0xFF) + ((argb >> 8) & 0xFF) + (argb & 0xFF)) / 3;
        float v = 62 + (c - 35) * 1.75f;          // dark pixels -> ~60 (teal shadow), bright -> ~190
        int r = clamp(Math.round(v * 0.90f));
        int g = clamp(Math.round(v * 1.00f));
        int b = clamp(Math.round(v * 0.99f));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int scale(int argb, float mul, int add) {
        int a = argb >>> 24;
        int r = clamp(Math.round(((argb >> 16) & 0xFF) * mul) + add);
        int g = clamp(Math.round(((argb >> 8) & 0xFF) * mul) + add);
        int b = clamp(Math.round((argb & 0xFF) * mul) + add);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }

    // ---- zip building ----------------------------------------------------------------------

    private static void build(Path out, String description, VanillaPackResources vanilla,
                              List<String> folderPrefixes, List<String> singleFiles, Px px) throws IOException {
        Map<String, byte[]> files = new LinkedHashMap<>();

        for (String prefix : folderPrefixes) {
            vanilla.listResources(PackType.CLIENT_RESOURCES, "minecraft", prefix, (id, supplier) -> {
                String path = id.getPath();
                try (InputStream in = supplier.get()) {
                    if (path.endsWith(".png")) {
                        files.put("assets/minecraft/" + path, toPng(recolor(ImageIO.read(in), px)));
                    } else if (path.endsWith(".mcmeta")) {
                        // keep nine-slice / animation info for the sprites we replace
                        files.put("assets/minecraft/" + path, in.readAllBytes());
                    }
                } catch (IOException e) {
                    MBTextureMenuClient.LOG.warn("Skipping {}", path, e);
                }
            });
        }

        for (String path : singleFiles) {
            var sup = vanilla.getResource(PackType.CLIENT_RESOURCES, Identifier.fromNamespaceAndPath("minecraft", path));
            if (sup == null) continue;
            try (InputStream in = sup.get()) {
                files.put("assets/minecraft/" + path, toPng(recolor(ImageIO.read(in), px)));
            }
        }

        String mcmeta = "{\"pack\":{\"description\":\"" + description + "\",\"pack_format\":75,"
                + "\"min_format\":63,\"max_format\":999}}";
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(out))) {
            zip.putNextEntry(new ZipEntry("pack.mcmeta"));
            zip.write(mcmeta.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            for (var e : files.entrySet()) {
                zip.putNextEntry(new ZipEntry(e.getKey()));
                zip.write(e.getValue());
                zip.closeEntry();
            }
        }
        MBTextureMenuClient.LOG.info("Generated {} ({} files)", out.getFileName(), files.size());
    }

    private static BufferedImage recolor(BufferedImage src, Px px) {
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                out.setRGB(x, y, px.map(src.getRGB(x, y)));
            }
        }
        return out;
    }

    private static byte[] toPng(BufferedImage img) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", bos);
        return bos.toByteArray();
    }
}
