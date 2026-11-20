package mb.texturemenu.gui;

import mb.texturemenu.MBConfig;
import mb.texturemenu.MBConfig.ExtraMode;
import mb.texturemenu.MBConfig.NetheriteMode;
import mb.texturemenu.MBConfig.SecretTheme;
import mb.texturemenu.pack.PackManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.EnumMap;
import java.util.Map;

/** Hidden menu - opened by holding Alt while clicking "MB's Texture Menu". Only ONE theme can be on at a time. */
public class MBSecretScreen extends Screen {
    private static final SecretTheme[] THEMES = {SecretTheme.GOLD, SecretTheme.EMERALD, SecretTheme.RED, SecretTheme.MB};

    private final Screen parent;
    private final MBConfig cfg = MBConfig.get();
    private final String startKey;
    private final Map<SecretTheme, Button> buttons = new EnumMap<>(SecretTheme.class);

    public MBSecretScreen(Screen parent) {
        super(Component.literal("MB's Secret Menu"));
        this.parent = parent;
        this.startKey = cfg.packKey();
    }

    @Override
    protected void init() {
        int w = 220, x = this.width / 2 - w / 2, y = this.height / 6 + 10, row = 24;

        for (int i = 0; i < THEMES.length; i++) {
            SecretTheme t = THEMES[i];
            Button b = Button.builder(label(t), btn -> {
                if (cfg.secretTheme == t) {
                    cfg.secretTheme = SecretTheme.OFF;          // pressing the active one turns it off
                } else {
                    cfg.secretTheme = t;                        // turning one on turns all the others off
                    claimItems(t);
                }
                refresh();
            }).bounds(x, y + i * row, w, 20).build();
            buttons.put(t, b);
            addRenderableWidget(b);
        }

        addRenderableWidget(Button.builder(Component.literal("Disable All Secret Textures"), b -> {
            cfg.secretTheme = SecretTheme.OFF;
            refresh();
        }).bounds(x, y + THEMES.length * row + 6, w, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(x, y + THEMES.length * row + 40, w, 20).build());
    }

    /** The theme now covers these items, so the matching normal dark/light settings are reset (no more fighting). */
    private void claimItems(SecretTheme t) {
        if (t != SecretTheme.MB) cfg.inventoryDark = false;   // MB does not touch the inventory
        cfg.netherite = NetheriteMode.NORMAL;
        cfg.tools = NetheriteMode.NORMAL;
        cfg.totemShield = ExtraMode.NORMAL;
    }

    private Component label(SecretTheme t) {
        String n = switch (t) {
            case GOLD -> "Gold";
            case EMERALD -> "Emerald";
            case RED -> "Red";
            case MB -> "MB Texture";
            default -> "";
        };
        return Component.literal(n + ": " + (cfg.secretTheme == t ? "ON" : "OFF"));
    }

    private void refresh() {
        buttons.forEach((t, b) -> b.setMessage(label(t)));
    }

    private String description() {
        return switch (cfg.secretTheme) {
            case OFF -> "No secret theme active";
            case GOLD -> "Gold inventory, hotbar, XP bar, armor, tools, totem and shield";
            case EMERALD -> "Emerald inventory, hotbar, XP bar, armor, tools, totem and shield";
            case RED -> "Red inventory, hotbar, XP bar, armor, tools, totem and shield";
            case MB -> "B&W armor with gold helmet, MB apple, mace, spear, totem and shield";
        };
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        int y = this.height / 6 + 10;
        g.drawCenteredString(this.font, this.title, this.width / 2, y - 20, 0xFFFFE200);
        g.drawCenteredString(this.font, Component.literal(description()), this.width / 2, y + 4 * 24 + 62, 0xFFDDDDDD);
        g.drawCenteredString(this.font, Component.literal("Applies when you press Done"),
                this.width / 2, y + 4 * 24 + 76, 0xFFAAAAAA);
    }

    @Override
    public void onClose() {
        cfg.save();
        boolean packsChanged = !cfg.packKey().equals(startKey);
        this.minecraft.setScreen(parent);
        if (packsChanged) PackManager.apply(this.minecraft);
    }
}
