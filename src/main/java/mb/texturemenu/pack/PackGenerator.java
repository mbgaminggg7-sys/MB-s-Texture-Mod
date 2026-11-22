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
    private static final String V = " v6.zip";   // bumped so older generated packs are rebuilt
    public static final String INVENTORY_DARK = "MB Dark Inventory" + V;
    public static final String NETHERITE_DARK = "MB Netherite Armor Dark" + V;
    public static final String NETHERITE_LIGHT = "MB Netherite Armor Light" + V;
    public static final String TOOLS_DARK = "MB Netherite Tools Dark" + V;
    public static final String TOOLS_LIGHT = "MB Netherite Tools Light" + V;
    public static final String DIAMOND_DARK = "MB Diamond Armor Dark" + V;
    public static final String DIAMOND_LIGHT = "MB Diamond Armor Light" + V;
    public static final String DIAMOND_TOOLS_DARK = "MB Diamond Tools Dark" + V;
    public static final String DIAMOND_TOOLS_LIGHT = "MB Diamond Tools Light" + V;
    public static final String TOTEM_DARK = "MB Totem Shield Dark" + V;
    public static final String TOTEM_LIGHT = "MB Totem Shield Light" + V;
    public static final String TOTEM_DIAMOND = "MB Totem Shield Diamond" + V;
    public static String neonName(int level) { return "MB Neon Trims " + level + V; }

    public static String secretName(SecretTheme t) {
        return switch (t) {
            case GOLD -> "MB Secret Gold" + V;
            case EMERALD -> "MB Secret Emerald" + V;
            case RED -> "MB Secret Red" + V;
            case MB -> "MB Secret MB" + V;
            case RAINBOW -> "MB Secret Rainbow" + V;
            case DIAMOND -> "MB Secret Diamond" + V;
            default -> null;
        };
    }

    /** Old pack names from the first version of the mod (they recolored ALL armor types - removed). */
    private static final List<String> LEGACY = List.of("MB Dark Inventory.zip", "MB Netherite Dark.zip",
            "MB Netherite Light.zip", "MB Tools Dark.zip", "MB Tools Light.zip");

    private interface Xform { BufferedImage apply(String path, BufferedImage src); }

    /** icon = file name (without .png) in assets/mbtexturemenu/packicons - every pack gets its own picture. */
    private record Spec(String description, List<String> folders, List<String> files, Xform xf, String icon) {}

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
        l.add("textures/item/" + mat + "_spear_in_hand.png"); // 32x32 texture used when held
        if (withMace) l.add("textures/item/mace.png");
        return l;
    }

    /** Totem of Undying + shield (the shield is drawn from its entity texture; both known locations are tried). */
    private static List<String> totemFiles() {
        return List.of("textures/item/totem_of_undying.png",
                "textures/entity/shield/shield_base_nopattern.png", "textures/entity/shield/shield_base.png",
                "textures/entity/shield_base_nopattern.png", "textures/entity/shield_base.png");
    }

    @SafeVarargs
    private static List<String> concat(List<String>... lists) {
        List<String> out = new ArrayList<>();
        for (List<String> l : lists) out.addAll(l);
        return out;
    }

    private static Xform grad(int[][] pal) {
        return (path, img) -> gradient(img, pal, 1.0f);
    }

    private static Spec specFor(String name) {
        if (name.equals(INVENTORY_DARK)) {
            return new Spec("MB's Texture Menu - dark inventory, hotbar and XP bar", GUI_FOLDERS,
                    concat(HOTBAR, List.of(XP_BG)), px(PackGenerator::darkGui), "inv_dark");
        }
        // dark = replaced with black, light = replaced with white (not just darkened/lightened)
        if (name.equals(NETHERITE_DARK)) return files("black netherite armor", armorFiles("netherite"), grad(DARK_P), "net_armor_dark");
        if (name.equals(NETHERITE_LIGHT)) return files("white netherite armor", armorFiles("netherite"), grad(TEAL_P), "net_armor_light");
        if (name.equals(TOOLS_DARK)) return files("black netherite tools", toolFiles("netherite", true), grad(DARK_P), "net_tools_dark");
        if (name.equals(TOOLS_LIGHT)) return files("white netherite tools", toolFiles("netherite", true), grad(TEAL_P), "net_tools_light");
        if (name.equals(DIAMOND_DARK)) return files("black diamond armor", armorFiles("diamond"), grad(DARK_P), "dia_armor_dark");
        if (name.equals(DIAMOND_LIGHT)) return files("white diamond armor", armorFiles("diamond"), grad(WHITE_P), "dia_armor_light");
        if (name.equals(DIAMOND_TOOLS_DARK)) return files("black diamond tools", toolFiles("diamond", false), grad(DARK_P), "dia_tools_dark");
        if (name.equals(DIAMOND_TOOLS_LIGHT)) return files("white diamond tools", toolFiles("diamond", false), grad(WHITE_P), "dia_tools_light");
        for (int lv = 1; lv <= 3; lv++) {
            if (!name.equals(neonName(lv))) continue;
            // trim palettes -> bright neon, so trims pop in any light (trim_palette.png is the KEY palette: untouched)
            final int level = lv;
            return new Spec("MB's Texture Menu - neon armor trims (level " + lv + ")", List.of("textures/trims/color_palettes"),
                    List.of(), (path, img) -> path.endsWith("/trim_palette.png") ? img : neonPalette(img, level), "trim_neon");
        }
        if (name.equals(TOTEM_DARK)) return files("black totem and shield", totemFiles(), grad(DARK_P), "totem_dark");
        if (name.equals(TOTEM_LIGHT)) return files("white totem and shield", totemFiles(), grad(WHITE_P), "totem_light");
        if (name.equals(TOTEM_DIAMOND)) return files("diamond totem and shield", totemFiles(), grad(DIAMOND_P), "totem_diamond");

        for (SecretTheme t : SecretTheme.values()) {
            if (t == SecretTheme.OFF || !name.equals(secretName(t))) continue;
            // every theme covers netherite AND diamond armor/tools, mace, spear, totem and shield
            List<String> items = concat(armorFiles("netherite"), armorFiles("diamond"),
                    toolFiles("netherite", true), toolFiles("diamond", false), totemFiles());
            String icon = "sec_" + t.name().toLowerCase();
            if (t == SecretTheme.MB) {
                return new Spec("MB's Texture Menu - MB texture", List.of(),
                        concat(items, List.of("textures/item/golden_apple.png", "textures/item/cooked_beef.png",
                                "textures/item/ender_pearl.png")), PackGenerator::mbXform, icon);
            }
            List<String> all = concat(HOTBAR, List.of(HOTBAR_SEL, XP_BG, XP_FG), items);
            if (t == SecretTheme.RAINBOW) {
                return new Spec("MB secret theme - rainbow", GUI_FOLDERS, all,
                        (path, img) -> rainbow(img, path.startsWith("textures/gui")), icon);
            }
            int[][] pal = switch (t) {
                case GOLD -> GOLD;
                case EMERALD -> EMERALD;
                case DIAMOND -> DIAMOND_P;
                default -> RED;
            };
            return new Spec("MB secret theme - " + t.name().toLowerCase(), GUI_FOLDERS, all,
                    (path, img) -> gradient(img, pal, path.startsWith("textures/gui") ? 0.85f : 1.0f), icon);
        }
        return null;
    }

    private static Spec files(String desc, List<String> files, Xform xf, String icon) {
        return new Spec("MB's Texture Menu - " + desc, List.of(), files, xf, icon);
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
            try (var stream = Files.list(dir)) {            // remove our packs from older mod versions
                for (Path f : (Iterable<Path>) stream::iterator) {
                    String n = f.getFileName().toString();
                    if (n.startsWith("MB ") && n.endsWith(".zip") && !n.endsWith(V)) Files.deleteIfExists(f);
                }
            }

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

    // black / white makeover palettes: colour is REPLACED, only the shading of the original is kept
    private static final int[][] DARK_P = {{4, 4, 6}, {14, 14, 18}, {30, 30, 38}, {62, 62, 74}, {120, 122, 138}};
    private static final int[][] WHITE_P = {{128, 134, 146}, {176, 182, 192}, {214, 219, 226}, {240, 243, 247}, {255, 255, 255}};
    private static final int[][] TEAL_P = {{70, 96, 94}, {128, 152, 150}, {176, 194, 192}, {222, 232, 231}, {250, 253, 253}};
    private static final int[][] DIAMOND_P = {{8, 70, 80}, {20, 150, 170}, {40, 210, 215}, {130, 245, 240}, {235, 255, 255}};

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

    /** Brighter, more saturated palette: same hue, shading kept but lifted (V 0.72-1.0), saturation boosted. */
    private static BufferedImage neonPalette(BufferedImage src, int level) {
        float satMul = level == 1 ? 1.15f : level == 2 ? 1.30f : 1.55f;
        float satAdd = level == 1 ? 0.05f : level == 2 ? 0.10f : 0.22f;
        float vBase = level == 1 ? 0.60f : level == 2 ? 0.72f : 0.88f;
        float vRange = 1f - vBase;
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int p = src.getRGB(x, y);
                int a = p >>> 24;
                if (a == 0) continue;
                float[] hsb = java.awt.Color.RGBtoHSB((p >> 16) & 0xFF, (p >> 8) & 0xFF, p & 0xFF, null);
                float sat = hsb[1] > 0.08f ? Math.min(1f, hsb[1] * satMul + satAdd) : hsb[1];   // grays stay gray (white glow)
                float val = vBase + vRange * hsb[2];
                out.setRGB(x, y, (a << 24) | (java.awt.Color.HSBtoRGB(hsb[0], sat, val) & 0xFFFFFF));
            }
        }
        return out;
    }

    // ================================================================ MB texture (black & white, glossy, gold, smile)

    private static final int YELLOW = 0xFFFFE200;
    private static final int WHITE = 0xFFF5F5F5;
    private static final int BLACK = 0x0E0E0E;

    private static BufferedImage mbXform(String path, BufferedImage src) {
        if (path.endsWith("humanoid/netherite.png") || path.endsWith("humanoid/diamond.png")) return mbArmorLayer1(src);
        if (path.endsWith("humanoid_leggings/netherite.png") || path.endsWith("humanoid_leggings/diamond.png")) return mbLeggings(src);
        if (path.endsWith("golden_apple.png")) return mbApple(src);
        if (path.endsWith("cooked_beef.png")) return mbSteak(src);
        if (path.endsWith("ender_pearl.png")) return mbPearl(src);
        if (path.endsWith("mace.png")) return mbMace(src);
        if (path.contains("_spear")) return mbSpear(src);
        if (path.endsWith("totem_of_undying.png")) return goldTopIcon(mbBW(src));   // totem gets a gold crown on top
        if (path.contains("shield")) return mbBW(src);
        if (path.endsWith("_helmet.png")) return goldTopIcon(mbBW(src));
        if (path.endsWith("_sword.png") || path.endsWith("_axe.png") || path.endsWith("_pickaxe.png")) return mbTool(src, true);
        if (path.endsWith("_shovel.png") || path.endsWith("_hoe.png")) return mbTool(src, false);
        return mbBW(src); // other armor icons
    }

    /** Wooden handle pixels (brown) - everything else on a tool is the metal head/blade. */
    private static boolean brownish(int argb) {
        int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        return r > b + 18 && r >= g;
    }

    /** Black & white tool; sword/axe/pickaxe get a sharp YELLOW edge along the top-left of the head/blade. */
    private static BufferedImage mbTool(BufferedImage src, boolean yellowEdge) {
        BufferedImage out = mbBW(src);
        if (!yellowEdge) return out;
        float[] rg = range(src);
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int p = src.getRGB(x, y);
                if ((p >>> 24) < 40 || brownish(p)) continue;
                boolean top = y == 0 || (src.getRGB(x, y - 1) >>> 24) < 40;
                boolean left = x == 0 || (src.getRGB(x - 1, y) >>> 24) < 40;
                if (!top && !left) continue;
                float t = Math.max(0f, Math.min(1f, (lum(p) - rg[0]) / (rg[1] - rg[0])));
                float k = 0.78f + 0.22f * t;                              // keeps a little shading in the yellow
                int r = Math.round(255 * k), g = Math.round(214 * k);
                out.setRGB(x, y, ((p >>> 24) << 24) | (r << 16) | (g << 8));
            }
        }
        return out;
    }

    /** Rainbow bands running diagonally, with the original's shading kept. */
    private static BufferedImage rainbow(BufferedImage src, boolean gui) {
        float[] rg = range(src);
        float period = gui ? 64f : 24f;
        float vmin = gui ? 0.60f : 0.35f;
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int p = src.getRGB(x, y);
                int a = p >>> 24;
                if (a == 0) continue;
                float t = Math.max(0f, Math.min(1f, (lum(p) - rg[0]) / (rg[1] - rg[0])));
                float hue = ((x + y) / period) % 1f;
                out.setRGB(x, y, (a << 24) | (java.awt.Color.HSBtoRGB(hue, 0.85f, vmin + (1f - vmin) * t) & 0xFFFFFF));
            }
        }
        return out;
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

    private static int mix(int argb, int rgb, float t) {
        int a = argb >>> 24;
        int r = Math.round(((argb >> 16) & 0xFF) * (1 - t) + ((rgb >> 16) & 0xFF) * t);
        int g = Math.round(((argb >> 8) & 0xFF) * (1 - t) + ((rgb >> 8) & 0xFF) * t);
        int b = Math.round((argb & 0xFF) * (1 - t) + (rgb & 0xFF) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /** Glossy fade: bright sheen at the top of every face, fading to deeper black at the bottom. */
    private static void gloss(BufferedImage img) {
        int[][] regions = {{0, 31, 8, 15}, {0, 55, 20, 31}}; // head faces, body/arms/legs faces
        for (int[] g : regions) {
            for (int y = g[2]; y <= g[3]; y++) {
                float f = (y - g[2]) / (float) (g[3] - g[2]);       // 0 top -> 1 bottom
                float sheen = 0.34f * (float) Math.pow(1 - f, 1.5);
                float dark = 0.80f + 0.20f * (1 - f);
                for (int x = g[0]; x <= g[1] && x < img.getWidth(); x++) {
                    int p = img.getRGB(x, y);
                    if ((p >>> 24) == 0) continue;
                    int c = mix(p, 0x000000, 1 - dark);
                    img.setRGB(x, y, mix(c, 0xFFFFFF, sheen));
                }
            }
        }
    }

    private static int goldAt(int argb) {
        float v = (argb & 0xFF) / 255f;
        return sample(GOLD, 0.45f + 0.55f * v);
    }

    /** Normal-looking helmet (black/white mix) with gold coming down from the top, like a crown. */
    private static void goldHelmet(BufferedImage img) {
        for (int y = 0; y < 8; y++) {                                   // top face
            for (int x = 8; x < 16; x++) {
                int p = img.getRGB(x, y);
                if ((p >>> 24) != 0) img.setRGB(x, y, (p & 0xFF000000) | goldAt(p));
            }
        }
        int[] faceX = {0, 8, 16, 24};                                  // right, front, left, back
        float[] amt = {0.85f, 0.55f, 0.22f};
        for (int fx : faceX) {
            for (int r = 0; r < amt.length; r++) {
                for (int x = fx; x < fx + 8; x++) {
                    int p = img.getRGB(x, 8 + r);
                    if ((p >>> 24) != 0) img.setRGB(x, 8 + r, mix(p, goldAt(p), amt[r]));
                }
            }
        }
        for (int x = 8; x < 16; x += 2) {                               // little crown teeth on the front
            int p = img.getRGB(x, 11);
            if ((p >>> 24) != 0) img.setRGB(x, 11, mix(p, goldAt(p), 0.6f));
        }
    }

    /** 64x32 armor layer: black/white mix + glossy fade everywhere, gold on top of the helmet. */
    private static BufferedImage mbArmorLayer1(BufferedImage src) {
        BufferedImage out = mbBW(src);
        if (out.getWidth() != 64 || out.getHeight() != 32) return out;
        gloss(out);
        goldHelmet(out);
        return out;
    }

    private static BufferedImage mbLeggings(BufferedImage src) {
        BufferedImage out = mbBW(src);
        if (out.getWidth() == 64 && out.getHeight() == 32) gloss(out);
        return out;
    }

    /** Helmet icon: a bit of gold on the top rows. */
    private static BufferedImage goldTopIcon(BufferedImage img) {
        int minY = -1;
        for (int y = 0; y < img.getHeight() && minY < 0; y++) {
            for (int x = 0; x < img.getWidth(); x++) if ((img.getRGB(x, y) >>> 24) > 40) { minY = y; break; }
        }
        if (minY < 0) return img;
        float[] amt = {0.85f, 0.5f, 0.2f};
        for (int r = 0; r < amt.length; r++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int p = img.getRGB(x, minY + r);
                if ((p >>> 24) > 40) img.setRGB(x, minY + r, mix(p, goldAt(p), amt[r]));
            }
        }
        return img;
    }

    /** Steak -> a black noodle pack (stylized pixel art: black bag, red-orange label, green/yellow stripes). */
    private static BufferedImage mbSteak(BufferedImage src) {
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        if (w != 16 || h != 16) return mbBW(src);                      // only the normal 16x16 icon is redrawn
        int K = 0xFF101012, KG = 0xFF3C3C44, RED = 0xFFE2401C, RD = 0xFFA82410, ORG = 0xFFF0961E,
                GRN = 0xFF28A03C, YEL = 0xFFFFD000, WH = 0xFFF5F5F5, CRM = 0xFFFFE8B0;
        for (int y = 3; y <= 12; y++) {                                 // bag
            for (int x = 1; x <= 14; x++) out.setRGB(x, y, K);
            if (y % 2 == 0) { out.setRGB(0, y, K); out.setRGB(15, y, K); }   // crimped ends
        }
        for (int x = 3; x <= 12; x++) out.setRGB(x, 4, KG);             // glossy top edge
        for (int y = 5; y <= 8; y++) for (int x = 2; x <= 6; x++) out.setRGB(x, y, y == 8 ? RD : RED);   // label
        put(out, 3, 6, WH); put(out, 4, 7, WH); put(out, 5, 6, WH);     // squiggle "logo"
        for (int x = 2; x <= 6; x++) { out.setRGB(x, 9, YEL); out.setRGB(x, 10, GRN); }   // stripes
        int[][] noodles = {{8,6},{9,5},{10,6},{11,5},{12,6},{13,5},          // two wavy noodle lines
                {8,9},{9,8},{10,9},{11,8},{12,9},{13,8}};
        for (int[] n : noodles) out.setRGB(n[0], n[1], ORG);
        for (int x = 9; x <= 12; x++) out.setRGB(x, 7, CRM);                  // chicken strip between them
        for (int x = 11; x <= 13; x++) { out.setRGB(x, 11, GRN); }
        out.setRGB(12, 12, YEL); out.setRGB(13, 12, YEL);
        for (int x = 7; x <= 9; x++) out.setRGB(x, 11, WH);             // tiny "text"
        return out;
    }

    /** Ender pearl -> mostly black, a little white shine, yellow rim on the lower right. */
    private static BufferedImage mbPearl(BufferedImage src) {
        int w = src.getWidth(), h = src.getHeight();
        float[] rg = range(src);
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int minX = 99, maxX = -1, minY = 99, maxY = -1;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int p = src.getRGB(x, y);
                int a = p >>> 24;
                if (a < 40) continue;
                minX = Math.min(minX, x); maxX = Math.max(maxX, x); minY = Math.min(minY, y); maxY = Math.max(maxY, y);
                float v = Math.max(0f, Math.min(1f, (lum(p) - rg[0]) / (rg[1] - rg[0])));
                int g = Math.round(10 + v * 46);
                if (v > 0.85f) g = 232;                                // its own shine becomes white
                out.setRGB(x, y, (a << 24) | (g << 16) | (g << 8) | Math.min(255, g + 4));
            }
        }
        if (maxX < 0) return out;
        for (int y = 0; y < h; y++) {                                  // yellow rim, lower right
            for (int x = 0; x < w; x++) {
                if ((src.getRGB(x, y) >>> 24) < 40) continue;
                boolean right = x + 1 >= w || (src.getRGB(x + 1, y) >>> 24) < 40;
                boolean below = y + 1 >= h || (src.getRGB(x, y + 1) >>> 24) < 40;
                if ((right || below) && x >= (minX + maxX) / 2 && y >= (minY + maxY) / 2 - 1) out.setRGB(x, y, 0xFFFFD000);
            }
        }
        int cx = (minX + maxX) / 2, cy = (minY + maxY) / 2;
        int[][] glint = {{-2, -3}, {-3, -2}, {-2, -2}};                // white shine, upper left
        for (int[] d : glint) {
            int x = cx + d[0], y = cy + d[1];
            if (x >= 0 && y >= 0 && x < w && y < h && (src.getRGB(x, y) >>> 24) >= 40) out.setRGB(x, y, 0xFFF5F5F5);
        }
        return out;
    }

    /** Golden apple -> glossy black apple (keeps the original's shading) with a crown, eyes and a smile. */
    private static BufferedImage mbApple(BufferedImage src) {
        float[] rg = range(src);
        int minX = 99, maxX = -1, minY = 99;
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                if ((src.getRGB(x, y) >>> 24) > 40) {
                    minX = Math.min(minX, x); maxX = Math.max(maxX, x); minY = Math.min(minY, y);
                }
            }
        }
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        int cx = (minX + maxX) / 2, top = minY;
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int p = src.getRGB(x, y);
                int a = p >>> 24;
                if (a == 0) continue;
                float v = Math.max(0f, Math.min(1f, (lum(p) - rg[0]) / (rg[1] - rg[0])));
                float g = 20 + v * 70;                       // charcoal -> grey, keeps the apple's own shading
                if (v > 0.9f) g = 215 + (v - 0.9f) * 400;    // its original shine becomes a white glint
                if (x > cx + 2 && y > top + 7) g *= 0.72f;   // darker rim bottom-right for roundness
                int gi = Math.max(0, Math.min(255, Math.round(g)));
                int r = Math.round(gi * 0.93f), b = Math.min(255, Math.round(gi * 1.14f));  // faint cool tint = glossy
                out.setRGB(x, y, (a << 24) | (r << 16) | (gi << 8) | b);
            }
        }
        if (maxX < 0) return out;
        // gloss streak, top-left
        int[][] streak = {{-4, 3}, {-4, 4}, {-3, 2}, {-2, 2}};
        for (int[] d : streak) {
            int x = cx + d[0], y = top + d[1];
            if (x >= 0 && y >= 0 && x < out.getWidth() && y < out.getHeight() && (out.getRGB(x, y) >>> 24) != 0) {
                out.setRGB(x, y, 0xFFCFD6E0);
            }
        }
        String[] crown = {"Y.YY.Y", "YYYYYY", "YYYYYY"};
        for (int r = 0; r < crown.length; r++) {
            for (int c = 0; c < 6; c++) if (crown[r].charAt(c) == 'Y') put(out, cx - 3 + c, top + 1 + r, YELLOW);
        }
        // eyes + smile
        int[][] face = {{cx - 3, top + 6}, {cx + 2, top + 6}, {cx - 3, top + 8}, {cx + 2, top + 8},
                {cx - 2, top + 9}, {cx - 1, top + 9}, {cx, top + 9}, {cx + 1, top + 9}};
        for (int[] d : face) put(out, d[0], d[1], WHITE);
        return out;
    }

    /** Spear -> black & white with a little smile on the blade (works for the 16x16 icon and the 32x32 in-hand texture). */
    private static BufferedImage mbSpear(BufferedImage src) {
        BufferedImage out = mbBW(src);
        int w = src.getWidth(), h = src.getHeight(), k = Math.max(1, w / 16);
        long sx = 0, sy = 0;
        int n = 0;
        for (int y = 0; y < h * 2 / 5; y++) {               // the blade is in the upper part
            for (int x = 0; x < w; x++) {
                if ((src.getRGB(x, y) >>> 24) > 40) { sx += x; sy += y; n++; }
            }
        }
        if (n == 0) return out;
        int hx = (int) (sx / n), hy = (int) (sy / n);
        int[][] smile = {{-1, 0}, {0, 1}, {1, 0}};
        for (int[] d : smile) {                       // thin dark backing first so the smile reads clearly
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int a = 0; a < k; a++) {
                        for (int b = 0; b < k; b++) {
                            int x = hx + d[0] * k + dx * k + a, y = hy + d[1] * k + dy * k + b;
                            if (x >= 0 && y >= 0 && x < w && y < h && (src.getRGB(x, y) >>> 24) > 40) {
                                out.setRGB(x, y, 0xFF000000 | BLACK);
                            }
                        }
                    }
                }
            }
        }
        for (int[] d : smile) {
            for (int a = 0; a < k; a++) {
                for (int b = 0; b < k; b++) {
                    int x = hx + d[0] * k + a, y = hy + d[1] * k + b;
                    if (nearOpaque(src, x, y)) put(out, x, y, WHITE);
                }
            }
        }
        return out;
    }

    private static boolean nearOpaque(BufferedImage img, int x, int y) {
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int px = x + dx, py = y + dy;
                if (px >= 0 && py >= 0 && px < img.getWidth() && py < img.getHeight()
                        && (img.getRGB(px, py) >>> 24) > 40) return true;
            }
        }
        return false;
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
        byte[] icon = readIcon(spec.icon());
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

    /** The pack's own picture, falling back to the general MB logo. */
    private static byte[] readIcon(String key) {
        for (String path : new String[]{"/assets/mbtexturemenu/packicons/" + key + ".png", "/assets/mbtexturemenu/icon.png"}) {
            try (InputStream in = PackGenerator.class.getResourceAsStream(path)) {
                if (in != null) return in.readAllBytes();
            } catch (IOException ignored) {
            }
        }
        return null;
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
