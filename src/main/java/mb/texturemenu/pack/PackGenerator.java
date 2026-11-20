package mb.texturemenu.pack;

import mb.texturemenu.MBConfig.SecretTheme;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntUnaryOperator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Builds every resource pack from the player's OWN game textures (no Mojang assets are shipped in the jar).
 * Each pack is only generated the first time it is needed.
 */
public final class PackGenerator {
    private static final String V = " v2.zip";
    public static final String INVENTORY_DARK = "MB Dark Inventory" + V;
    public static final String NETHERITE_DARK = "MB Netherite Armor Dark" + V;
    public static final String NETHERITE_LIGHT = "MB Netherite Armor Light" + V;
    public static final String TOOLS_DARK = "MB Netherite Tools Dark" + V;
    public static final String TOOLS_LIGHT = "MB Netherite Tools Light" + V;
    public static final String DIAMOND_DARK = "MB Diamond Armor Dark" + V;
    public static final String DIAMOND_LIGHT = "MB Diamond Armor Light" + V;
    public static final String DIAMOND_TOOLS_DARK = "MB Diamond Tools Dark" + V;
    public static final String DIAMOND_TOOLS_LIGHT = "MB Diamond Tools Light" + V;

    public static String secretName(SecretTheme t) {
        return switch (t) {
            case GOLD -> "MB Secret Gold" + V;
            case EMERALD -> "MB Secret Emerald" + V;
            case RED -> "MB Secret Red" + V;
            case MB -> "MB Secret MB" + V;
            default -> null;
        };
    }

    /** Old pack names from the first version of the mod (they recolored ALL armor types - removed). */
    private static final List<String> LEGACY = List.of("MB Dark Inventory.zip", "MB Netherite Dark.zip",
            "MB Netherite Light.zip", "MB Tools Dark.zip", "MB Tools Light.zip");

    private interface Xform { BufferedImage apply(String path, BufferedImage src); }

    private record Spec(String description, List<String> folders, List<String> files, Xform xf) {}

    private PackGenerator() {}

    // ================================================================ file lists

    private static final List<String> GUI_FOLDERS =
            List.of("textures/gui/container", "textures/gui/sprites/container");

    private static final String HUD = "textures/gui/sprites/hud/";
    private static final List<String> HOTBAR = List.of(HUD + "hotbar.png", HUD + "hotbar_offhand_left.png",
            HUD + "hotbar_offhand_right.png");
    private static final String XP_BG = HUD + "experience_bar_background.png";
    private static final String XP_FG = HUD + "experience_bar_progress.png";
    private static final String HOTBAR_SEL = HUD + "hotbar_selection.png";

    private static List<String> armorFiles(String mat) {
        return List.of("textures/entity/equipment/humanoid/" + mat + ".png",
                "textures/entity/equipment/humanoid_leggings/" + mat + ".png",
                "textures/item/" + mat + "_helmet.png", "textures/item/" + mat + "_chestplate.png",
                "textures/item/" + mat + "_leggings.png", "textures/item/" + mat + "_boots.png");
    }

    /** sword, pickaxe, axe, shovel, hoe, spear (no shears). Missing files are skipped. */
    private static List<String> toolFiles(String mat, boolean withMace) {
        List<String> l = new ArrayList<>();
        for (String t : List.of("sword", "pickaxe", "axe", "shovel", "hoe", "spear")) {
            l.add("textures/item/" + mat + "_" + t + ".png");
        }
        if (withMace) l.add("textures/item/mace.png");
        return l;
    }

    @SafeVarargs
    private static List<String> concat(List<String>... lists) {
        List<String> out = new ArrayList<>();
        for (List<String> l : lists) out.addAll(l);
        return out;
    }

    private static Spec specFor(String name) {
        if (name.equals(INVENTORY_DARK)) {
            return new Spec("MB's Texture Menu - dark inventory, hotbar and XP bar", GUI_FOLDERS,
                    concat(HOTBAR, List.of(XP_BG)), px(PackGenerator::darkGui));
        }
        if (name.equals(NETHERITE_DARK)) return files("dark netherite armor", armorFiles("netherite"), px(PackGenerator::netheriteDark));
        if (name.equals(NETHERITE_LIGHT)) return files("light netherite armor", armorFiles("netherite"), px(PackGenerator::netheriteLight));
        if (name.equals(TOOLS_DARK)) return files("dark netherite tools", toolFiles("netherite", true), px(PackGenerator::netheriteDark));
        if (name.equals(TOOLS_LIGHT)) return files("light netherite tools", toolFiles("netherite", true), px(PackGenerator::netheriteLight));
        if (name.equals(DIAMOND_DARK)) return files("dark diamond armor", armorFiles("diamond"), px(PackGenerator::diamondDark));
        if (name.equals(DIAMOND_LIGHT)) return files("light diamond armor", armorFiles("diamond"), px(PackGenerator::diamondLight));
        if (name.equals(DIAMOND_TOOLS_DARK)) return files("dark diamond tools", toolFiles("diamond", false), px(PackGenerator::diamondDark));
        if (name.equals(DIAMOND_TOOLS_LIGHT)) return files("light diamond tools", toolFiles("diamond", false), px(PackGenerator::diamondLight));

        for (SecretTheme t : SecretTheme.values()) {
            if (t == SecretTheme.OFF || !name.equals(secretName(t))) continue;
            if (t == SecretTheme.MB) {
                return files("MB texture", concat(armorFiles("netherite"),
                        List.of("textures/item/golden_apple.png", "textures/item/mace.png")), PackGenerator::mbXform);
            }
            int[][] pal = switch (t) {
                case GOLD -> GOLD;
                case EMERALD -> EMERALD;
                default -> RED;
            };
            return new Spec("MB secret theme - " + t.name().toLowerCase(), GUI_FOLDERS,
                    concat(HOTBAR, List.of(HOTBAR_SEL, XP_BG, XP_FG), armorFiles("netherite"), toolFiles("netherite", true)),
                    (path, img) -> gradient(img, pal, path.startsWith("textures/gui") ? 0.85f : 1.0f));
        }
        return null;
    }

    private static Spec files(String desc, List<String> files, Xform xf) {
        return new Spec("MB's Texture Menu - " + desc, List.of(), files, xf);
    }

    private static Xform px(IntUnaryOperator f) {
        return (path, img) -> recolor(img, f);
    }

    // ================================================================ public entry

    public static void ensure(Minecraft mc, String zipName) {
        try {
            Path dir = mc.getResourcePackDirectory();
            Files.createDirectories(dir);
            for (String old : LEGACY) Files.deleteIfExists(dir.resolve(old));

            Path out = dir.resolve(zipName);
            if (Files.exists(out)) return;
            Spec spec = specFor(zipName);
            if (spec == null) return;
            build(out, spec, mc.getVanillaPackResources());
        } catch (Exception e) {
            MBTextureMenuClient.LOG.error("Failed to generate {}", zipName, e);
        }
    }

    // ================================================================ colour maths (simple)

    private static int darkGui(int argb) { return scale(argb, 0.36f, 8); }

    private static int netheriteDark(int argb) { return scale(argb, 0.36f, 1); }

    /** Netherite -> pale gray-green armor with darker teal shadows. */
    private static int netheriteLight(int argb) {
        int a = argb >>> 24;
        int c = (((argb >> 16) & 0xFF) + ((argb >> 8) & 0xFF) + (argb & 0xFF)) / 3;
        float v = 62 + (c - 35) * 1.75f;
        int r = clamp(Math.round(v * 0.90f));
        int g = clamp(Math.round(v * 1.00f));
        int b = clamp(Math.round(v * 0.99f));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int diamondDark(int argb) { return scale(argb, 0.38f, 0); }

    /** Diamond -> pale icy white-blue. */
    private static int diamondLight(int argb) {
        int a = argb >>> 24;
        int r = clamp(Math.round(((argb >> 16) & 0xFF) * 0.48f) + 135);
        int g = clamp(Math.round(((argb >> 8) & 0xFF) * 0.48f) + 150);
        int b = clamp(Math.round((argb & 0xFF) * 0.48f) + 155);
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

    // ================================================================ gradient themes (gold / emerald / red)

    private static final float[] POS = {0f, 0.35f, 0.65f, 0.85f, 1f};
    private static final int[][] GOLD = {{70, 30, 0}, {150, 72, 0}, {215, 130, 0}, {255, 205, 45}, {255, 247, 175}};
    private static final int[][] EMERALD = {{6, 52, 22}, {10, 120, 45}, {20, 190, 80}, {80, 235, 130}, {200, 255, 220}};
    private static final int[][] RED = {{30, 4, 10}, {92, 10, 26}, {160, 18, 40}, {212, 50, 64}, {255, 150, 150}};

    private static float lum(int argb) {
        return ((argb >> 16) & 0xFF) * 0.299f + ((argb >> 8) & 0xFF) * 0.587f + (argb & 0xFF) * 0.114f;
    }

    /** min/max luminance of the visible pixels, so every texture uses the full colour range. */
    private static float[] range(BufferedImage img) {
        float min = 255, max = 0;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int p = img.getRGB(x, y);
                if ((p >>> 24) < 16) continue;
                float l = lum(p);
                min = Math.min(min, l);
                max = Math.max(max, l);
            }
        }
        if (max - min < 12) { min = Math.max(0, min - 30); max = Math.min(255, max + 30); }
        return new float[]{min, max};
    }

    private static BufferedImage gradient(BufferedImage src, int[][] pal, float gain) {
        float[] r = range(src);
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int p = src.getRGB(x, y);
                int a = p >>> 24;
                if (a == 0) { out.setRGB(x, y, 0); continue; }
                float v = Math.max(0f, Math.min(1f, (lum(p) - r[0]) / (r[1] - r[0]))) * gain;
                out.setRGB(x, y, (a << 24) | sample(pal, v));
            }
        }
        return out;
    }

    private static int sample(int[][] pal, float v) {
        for (int i = 0; i < 4; i++) {
            if (v <= POS[i + 1]) {
                float t = (v - POS[i]) / (POS[i + 1] - POS[i]);
                int r = Math.round(pal[i][0] + (pal[i + 1][0] - pal[i][0]) * t);
                int g = Math.round(pal[i][1] + (pal[i + 1][1] - pal[i][1]) * t);
                int b = Math.round(pal[i][2] + (pal[i + 1][2] - pal[i][2]) * t);
                return (r << 16) | (g << 8) | b;
            }
        }
        return (pal[4][0] << 16) | (pal[4][1] << 8) | pal[4][2];
    }

    // ================================================================ MB texture (black & white + smile/crown)

    private static final int YELLOW = 0xFFFFE200;
    private static final int WHITE = 0xFFF5F5F5;
    private static final int BLACK = 0x0E0E0E;

    private static BufferedImage mbXform(String path, BufferedImage src) {
        if (path.endsWith("humanoid/netherite.png")) return mbArmorLayer1(src);
        if (path.endsWith("golden_apple.png")) return mbApple(src);
        if (path.endsWith("mace.png")) return mbMace(src);
        return mbBW(src); // leggings layer + armor icons
    }

    /** Mostly black with white highlights. */
    private static BufferedImage mbBW(BufferedImage src) {
        float[] r = range(src);
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int p = src.getRGB(x, y);
                int a = p >>> 24;
                if (a == 0) { out.setRGB(x, y, 0); continue; }
                float v = Math.max(0f, Math.min(1f, (lum(p) - r[0]) / (r[1] - r[0])));
                float t = Math.max(0f, Math.min(1f, (v - 0.40f) / 0.50f));
                t = t * t * (3 - 2 * t); // smoothstep
                int c = Math.round(14 + t * 236);
                out.setRGB(x, y, (a << 24) | (c << 16) | (c << 8) | c);
            }
        }
        return out;
    }

    private static void put(BufferedImage img, int x, int y, int argb) {
        if (x >= 0 && y >= 0 && x < img.getWidth() && y < img.getHeight()) img.setRGB(x, y, argb);
    }

    /** 64x32 armor layer: helmet fully black with a little crown, chestplate with a smile. */
    private static BufferedImage mbArmorLayer1(BufferedImage src) {
        BufferedImage out = mbBW(src);
        if (out.getWidth() != 64 || out.getHeight() != 32) return out;

        for (int y = 0; y < 16; y++) {            // whole head area -> black
            for (int x = 0; x < 64; x++) {
                int a = out.getRGB(x, y) >>> 24;
                if (a != 0) out.setRGB(x, y, (a << 24) | BLACK);
            }
        }
        String[] crown = {".Y.YY.Y.", ".YYYYYY.", ".YYYYYY."};   // head front face starts at (8,8)
        for (int r = 0; r < crown.length; r++) {
            for (int c = 0; c < 8; c++) if (crown[r].charAt(c) == 'Y') put(out, 8 + c, 9 + r, YELLOW);
        }
        // body front face starts at (20,20), 8 wide x 12 tall - black patch so the smile stands out
        for (int y = 20; y < 32; y++) {
            for (int x = 20; x < 28; x++) {
                int a = out.getRGB(x, y) >>> 24;
                if (a != 0) out.setRGB(x, y, (a << 24) | BLACK);
            }
        }
        put(out, 22, 23, WHITE); put(out, 25, 23, WHITE);                     // eyes
        put(out, 22, 26, WHITE); put(out, 22, 27, WHITE);                     // smile
        put(out, 23, 28, WHITE); put(out, 24, 28, WHITE);
        put(out, 25, 27, WHITE); put(out, 25, 26, WHITE);
        return out;
    }

    /** Golden apple -> black apple with a crown, eyes and a smile (shape comes from the original's outline). */
    private static BufferedImage mbApple(BufferedImage src) {
        int minX = 99, maxX = -1, minY = 99;
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                if ((src.getRGB(x, y) >>> 24) > 40) {
                    minX = Math.min(minX, x); maxX = Math.max(maxX, x); minY = Math.min(minY, y);
                }
            }
        }
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int a = src.getRGB(x, y) >>> 24;
                if (a != 0) out.setRGB(x, y, (a << 24) | 0x121212);
            }
        }
        if (maxX < 0) return out;
        int cx = (minX + maxX) / 2, top = minY;
        String[] crown = {"Y.YY.Y", "YYYYYY", "YYYYYY"};
        for (int r = 0; r < crown.length; r++) {
            for (int c = 0; c < 6; c++) if (crown[r].charAt(c) == 'Y') put(out, cx - 3 + c, top + 1 + r, YELLOW);
        }
        put(out, cx - 3, top + 6, WHITE); put(out, cx + 2, top + 6, WHITE);   // eyes
        put(out, cx - 3, top + 8, WHITE); put(out, cx + 2, top + 8, WHITE);   // smile
        for (int x = cx - 2; x <= cx + 1; x++) put(out, x, top + 9, WHITE);
        return out;
    }

    /** Mace -> black & white, with a tiny crown and smile on the head (head = upper right of the icon). */
    private static BufferedImage mbMace(BufferedImage src) {
        BufferedImage out = mbBW(src);
        int sx = 0, sy = 0, n = 0;
        for (int y = 0; y < src.getHeight() / 2; y++) {
            for (int x = src.getWidth() / 2; x < src.getWidth(); x++) {
                if ((src.getRGB(x, y) >>> 24) > 40) { sx += x; sy += y; n++; }
            }
        }
        if (n == 0) return out;
        int hx = sx / n, hy = sy / n;
        for (int dy = -2; dy <= 3; dy++) {            // dark patch so the face reads clearly
            for (int dx = -2; dx <= 2; dx++) {
                int px = hx + dx, py = hy + dy;
                if (px >= 0 && py >= 0 && px < src.getWidth() && py < src.getHeight()
                        && (src.getRGB(px, py) >>> 24) > 40) {
                    out.setRGB(px, py, 0xFF000000 | BLACK);
                }
            }
        }
        put(out, hx - 1, hy - 3, YELLOW); put(out, hx + 1, hy - 3, YELLOW);
        put(out, hx - 1, hy - 2, YELLOW); put(out, hx, hy - 2, YELLOW); put(out, hx + 1, hy - 2, YELLOW);
        put(out, hx - 1, hy, WHITE); put(out, hx + 1, hy, WHITE);
        put(out, hx - 1, hy + 2, WHITE); put(out, hx, hy + 3, WHITE); put(out, hx + 1, hy + 2, WHITE);
        return out;
    }

    // ================================================================ zip building

    private static void build(Path out, Spec spec, VanillaPackResources vanilla) throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();

        for (String prefix : spec.folders()) {
            vanilla.listResources(PackType.CLIENT_RESOURCES, "minecraft", prefix, (id, supplier) -> {
                String path = id.getPath();
                try (InputStream in = supplier.get()) {
                    if (path.endsWith(".png")) {
                        BufferedImage img = ImageIO.read(in);
                        if (img != null) entries.put("assets/minecraft/" + path, toPng(spec.xf().apply(path, img)));
                    } else if (path.endsWith(".mcmeta")) {
                        entries.put("assets/minecraft/" + path, in.readAllBytes()); // keep nine-slice info
                    }
                } catch (Exception e) {
                    MBTextureMenuClient.LOG.warn("Skipping {}", path, e);
                }
            });
        }

        for (String path : spec.files()) {
            try {
                var sup = vanilla.getResource(PackType.CLIENT_RESOURCES, Identifier.fromNamespaceAndPath("minecraft", path));
                if (sup == null) continue;
                try (InputStream in = sup.get()) {
                    BufferedImage img = ImageIO.read(in);
                    if (img != null) entries.put("assets/minecraft/" + path, toPng(spec.xf().apply(path, img)));
                }
                var meta = vanilla.getResource(PackType.CLIENT_RESOURCES,
                        Identifier.fromNamespaceAndPath("minecraft", path + ".mcmeta"));
                if (meta != null) {
                    try (InputStream in = meta.get()) {
                        entries.put("assets/minecraft/" + path + ".mcmeta", in.readAllBytes());
                    }
                }
            } catch (Exception e) {
                MBTextureMenuClient.LOG.warn("Skipping {}", path, e);
            }
        }

        String mcmeta = "{\"pack\":{\"description\":\"" + spec.description() + "\",\"pack_format\":75,"
                + "\"min_format\":63,\"max_format\":999}}";
        byte[] icon = readIcon();
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(out))) {
            zip.putNextEntry(new ZipEntry("pack.mcmeta"));
            zip.write(mcmeta.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            if (icon != null) {
                zip.putNextEntry(new ZipEntry("pack.png"));
                zip.write(icon);
                zip.closeEntry();
            }
            for (var e : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(e.getKey()));
                zip.write(e.getValue());
                zip.closeEntry();
            }
        }
        MBTextureMenuClient.LOG.info("Generated {} ({} files)", out.getFileName(), entries.size());
    }

    private static byte[] readIcon() {
        try (InputStream in = PackGenerator.class.getResourceAsStream("/assets/mbtexturemenu/icon.png")) {
            return in == null ? null : in.readAllBytes();
        } catch (IOException e) {
            return null;
        }
    }

    private static BufferedImage recolor(BufferedImage src, IntUnaryOperator f) {
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                out.setRGB(x, y, f.applyAsInt(src.getRGB(x, y)));
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
