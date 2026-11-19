package mb.texturemenu.gui;

import mb.texturemenu.MBConfig;
import mb.texturemenu.MBConfig.NetheriteMode;
import mb.texturemenu.pack.PackManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class MBTextureScreen extends Screen {
    private final Screen parent;
    private final MBConfig cfg = MBConfig.get();
    private final boolean startInventoryDark;
    private final NetheriteMode startNetherite;

    public MBTextureScreen(Screen parent) {
        super(Component.literal("MB's Texture Menu"));
        this.parent = parent;
        this.startInventoryDark = cfg.inventoryDark;
        this.startNetherite = cfg.netherite;
    }

    @Override
    protected void init() {
        int w = 220, x = this.width / 2 - w / 2, y = this.height / 4;

        // 1) inventory dark / light switch
        addRenderableWidget(Button.builder(inventoryLabel(), b -> {
            cfg.inventoryDark = !cfg.inventoryDark;
            b.setMessage(inventoryLabel());
        }).bounds(x, y, w, 20).build());

        // 2) netherite armor: normal -> dark -> light
        addRenderableWidget(Button.builder(netheriteLabel(), b -> {
            cfg.netherite = switch (cfg.netherite) {
                case NORMAL -> NetheriteMode.DARK;
                case DARK -> NetheriteMode.LIGHT;
                case LIGHT -> NetheriteMode.NORMAL;
            };
            b.setMessage(netheriteLabel());
        }).bounds(x, y + 28, w, 20).build());

        // 3) trim glow slider
        addRenderableWidget(new GlowSlider(x, y + 56, w, 20, cfg.glow));

        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(x, y + 96, w, 20).build());
    }

    private Component inventoryLabel() {
        return Component.literal("Inventory: " + (cfg.inventoryDark ? "Dark Mode" : "Light Mode"));
    }

    private Component netheriteLabel() {
        String m = switch (cfg.netherite) {
            case NORMAL -> "Normal";
            case DARK -> "Dark (blackout)";
            case LIGHT -> "Light (white)";
        };
        return Component.literal("Netherite Armor: " + m);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        g.drawCenteredString(this.font, this.title, this.width / 2, this.height / 4 - 24, 0xFFFFFFFF);
        g.drawCenteredString(this.font, Component.literal("Texture changes apply when you press Done"),
                this.width / 2, this.height / 4 + 124, 0xFFAAAAAA);
    }

    @Override
    public void onClose() {
        cfg.save();
        boolean packsChanged = cfg.inventoryDark != startInventoryDark || cfg.netherite != startNetherite;
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
