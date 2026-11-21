package mb.texturemenu.gui;

import mb.texturemenu.MBConfig;
import mb.texturemenu.MBConfig.ExtraMode;
import mb.texturemenu.MBConfig.NetheriteMode;
import mb.texturemenu.MBConfig.SecretTheme;
import mb.texturemenu.MBConfig.WaveSpeed;
import mb.texturemenu.pack.PackManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class MBTextureScreen extends Screen {
    private final Screen parent;
    private final MBConfig cfg = MBConfig.get();
    private final String startKey;

    public MBTextureScreen(Screen parent) {
        super(Component.literal("MB's Texture Menu"));
        this.parent = parent;
        this.startKey = cfg.packKey();
    }

    /** Last choice wins: if a secret theme was covering this setting, it gets switched off quietly. */
    private void releaseSecret(boolean isInventory) {
        if (cfg.secretTheme == SecretTheme.OFF) return;
        if (cfg.secretTheme == SecretTheme.MB && isInventory) return; // MB never touches the inventory
        cfg.secretTheme = SecretTheme.OFF;
    }

    @Override
    protected void init() {
        int bw = 150, gap = 8, full = bw * 2 + gap;
        int x1 = this.width / 2 - full / 2, x2 = x1 + bw + gap;
        int y = this.height / 6 + 4, row = 26;

        // row 0: inventory/hotbar/xp switch | wave speed
        addRenderableWidget(Button.builder(inventoryLabel(), b -> {
            cfg.inventoryDark = !cfg.inventoryDark;
            releaseSecret(true);
            b.setMessage(inventoryLabel());
        }).bounds(x1, y, bw, 20).build());

        addRenderableWidget(Button.builder(waveLabel(), b -> {
            cfg.waveSpeed = switch (cfg.waveSpeed) {
                case SLOW -> WaveSpeed.NORMAL;
                case NORMAL -> WaveSpeed.FAST;
                case FAST -> WaveSpeed.SLOW;
            };
            b.setMessage(waveLabel());
        }).bounds(x2, y, bw, 20).build());

        // row 1: netherite armor | netherite tools (sword, pickaxe, axe, shovel, hoe, spear, mace)
        addMode(x1, y + row, bw, "Netherite Armor", () -> cfg.netherite, m -> cfg.netherite = m, () -> releaseSecret(false));
        addMode(x2, y + row, bw, "Netherite Tools", () -> cfg.tools, m -> cfg.tools = m, () -> releaseSecret(false));

        // row 2: diamond armor | diamond tools
        addMode(x1, y + row * 2, bw, "Diamond Armor", () -> cfg.diamondArmor, m -> cfg.diamondArmor = m, () -> releaseSecret(false));
        addMode(x2, y + row * 2, bw, "Diamond Tools", () -> cfg.diamondTools, m -> cfg.diamondTools = m, () -> releaseSecret(false));

        // row 3: totem + shield (normal / dark / light / diamond)
        addRenderableWidget(Button.builder(totemLabel(), b -> {
            cfg.totemShield = switch (cfg.totemShield) {
                case NORMAL -> ExtraMode.DARK;
                case DARK -> ExtraMode.LIGHT;
                case LIGHT -> ExtraMode.DIAMOND;
                case DIAMOND -> ExtraMode.NORMAL;
            };
            releaseSecret(false);
            b.setMessage(totemLabel());
        }).bounds(x1, y + row * 3, full, 20).build());

        // row 4: trim glow slider
        addRenderableWidget(new GlowSlider(x1, y + row * 4, full, 20, cfg.glow));

        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(x1, y + row * 5 + 6, full, 20).build());
    }

    private void addMode(int x, int y, int w, String name, Supplier<NetheriteMode> get,
                         Consumer<NetheriteMode> set, Runnable after) {
        addRenderableWidget(Button.builder(modeLabel(name, get.get()), b -> {
            set.accept(next(get.get()));
            after.run();
            b.setMessage(modeLabel(name, get.get()));
        }).bounds(x, y, w, 20).build());
    }

    private static NetheriteMode next(NetheriteMode m) {
        return switch (m) {
            case NORMAL -> NetheriteMode.DARK;
            case DARK -> NetheriteMode.LIGHT;
            case LIGHT -> NetheriteMode.NORMAL;
        };
    }

    private Component inventoryLabel() {
        return Component.literal("Inventory/Hotbar: " + (cfg.inventoryDark ? "Dark" : "Light"));
    }

    private static Component modeLabel(String name, NetheriteMode mode) {
        String m = switch (mode) {
            case NORMAL -> "Normal";
            case DARK -> "Dark";
            case LIGHT -> "Light";
        };
        return Component.literal(name + ": " + m);
    }

    private Component totemLabel() {
        String m = switch (cfg.totemShield) {
            case NORMAL -> "Normal";
            case DARK -> "Dark";
            case LIGHT -> "Light";
            case DIAMOND -> "Diamond";
        };
        return Component.literal("Totem & Shield: " + m);
    }

    private Component waveLabel() {
        String m = switch (cfg.waveSpeed) {
            case SLOW -> "Slow";
            case NORMAL -> "Normal";
            case FAST -> "Fast";
        };
        return Component.literal("Wave Speed: " + m);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        int y = this.height / 6 + 4;
        g.drawCenteredString(this.font, this.title, this.width / 2, y - 18, 0xFFFFFFFF);
        g.drawCenteredString(this.font, Component.literal("Texture changes apply when you press Done"),
                this.width / 2, y + 26 * 5 + 32, 0xFFAAAAAA);
    }

    @Override
    public void onClose() {
        cfg.save();
        boolean packsChanged = !cfg.packKey().equals(startKey);
        this.minecraft.setScreen(parent);
        if (packsChanged) PackManager.apply(this.minecraft);
    }

    private static class GlowSlider extends AbstractSliderButton {
        GlowSlider(int x, int y, int w, int h, double value) {
            super(x, y, w, h, Component.empty(), value);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal("Trim Glow: " + Math.round(this.value * 100) + "%"));
        }

        @Override
        protected void applyValue() {
            MBConfig.get().glow = (float) this.value; // live, no reload needed
        }
    }
}
