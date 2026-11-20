package mb.texturemenu.gui;

import mb.texturemenu.MBConfig;
import mb.texturemenu.MBConfig.SecretTheme;
import mb.texturemenu.pack.PackManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Hidden menu - opened by holding Alt while clicking "MB's Texture Menu". */
public class MBSecretScreen extends Screen {
    private final Screen parent;
    private final MBConfig cfg = MBConfig.get();
    private final String startKey;

    public MBSecretScreen(Screen parent) {
        super(Component.literal("MB's Secret Menu"));
        this.parent = parent;
        this.startKey = cfg.packKey();
    }

    @Override
    protected void init() {
        int w = 220, x = this.width / 2 - w / 2, y = this.height / 4;

        addRenderableWidget(Button.builder(themeLabel(), b -> {
            cfg.secretTheme = switch (cfg.secretTheme) {
                case OFF -> SecretTheme.GOLD;
                case GOLD -> SecretTheme.EMERALD;
                case EMERALD -> SecretTheme.RED;
                case RED -> SecretTheme.MB;
                case MB -> SecretTheme.OFF;
            };
            b.setMessage(themeLabel());
        }).bounds(x, y, w, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(x, y + 90, w, 20).build());
    }

    private Component themeLabel() {
        String n = switch (cfg.secretTheme) {
            case OFF -> "Off";
            case GOLD -> "Gold";
            case EMERALD -> "Emerald";
            case RED -> "Red";
            case MB -> "MB Texture";
        };
        return Component.literal("Secret Theme: " + n);
    }

    private String description() {
        return switch (cfg.secretTheme) {
            case OFF -> "Pick a theme";
            case GOLD -> "Gold inventory, hotbar, XP bar, armor and tools";
            case EMERALD -> "Emerald inventory, hotbar, XP bar, armor and tools";
            case RED -> "Red inventory, hotbar, XP bar, armor and tools";
            case MB -> "Black & white armor (crown helmet, smile chest), MB golden apple and mace";
        };
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        int y = this.height / 4;
        g.drawCenteredString(this.font, this.title, this.width / 2, y - 24, 0xFFFFE200);
        g.drawCenteredString(this.font, Component.literal(description()), this.width / 2, y + 36, 0xFFDDDDDD);
        g.drawCenteredString(this.font, Component.literal("Applies when you press Done"),
                this.width / 2, y + 120, 0xFFAAAAAA);
    }

    @Override
    public void onClose() {
        cfg.save();
        boolean packsChanged = !cfg.packKey().equals(startKey);
        this.minecraft.setScreen(parent);
        if (packsChanged) PackManager.apply(this.minecraft);
    }
}
