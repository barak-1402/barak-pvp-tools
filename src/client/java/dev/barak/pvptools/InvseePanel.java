package dev.barak.pvptools;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Zeigt links neben einem geöffneten Kisten-Inventar (z.B. /invsee Name) an, wie viele
 * Crystals, Totems usw. darin liegen. Welche Items gezählt werden, stellst du im Menü ein.
 */
public final class InvseePanel {
    private static long recentUntil = 0L;
    private static String recentTarget = "";
    private static final Map<String, ItemStack> ICONS = new HashMap<>();

    private final long created = System.currentTimeMillis();
    private final String target;

    private InvseePanel(String target) {
        this.target = target;
    }

    /** Wird für jeden Befehl aufgerufen, den du im Chat abschickst (ohne führendes "/"). */
    public static void onCommand(String command) {
        if (command == null) {
            return;
        }
        String[] parts = command.trim().split("\\s+");
        if (parts.length == 0) {
            return;
        }
        String cmd = parts[0].toLowerCase(Locale.ROOT);
        if (cmd.startsWith("/")) {
            cmd = cmd.substring(1);
        }
        if (cmd.equals("invsee") || cmd.equals("openinv") || cmd.equals("oi") || cmd.equals("inspect") || cmd.equals("inv")) {
            recentUntil = System.currentTimeMillis() + 10_000L;
            recentTarget = parts.length > 1 ? parts[1] : "";
        }
    }

    /** Wird für jeden neuen Bildschirm aufgerufen. */
    public static void attach(Screen screen) {
        if (!(screen instanceof ContainerScreen)) {
            return;
        }
        if (!BarakConfig.invseeEnabled) {
            return;
        }
        boolean recent = System.currentTimeMillis() < recentUntil;
        if (!recent && !BarakConfig.invseeAlways) {
            return;
        }
        InvseePanel panel = new InvseePanel(recent ? recentTarget : "");
        ScreenEvents.afterRender(screen).register(panel::render);
    }

    private static ItemStack iconFor(String key) {
        ItemStack cached = ICONS.get(key);
        if (cached != null) {
            return cached;
        }
        ItemStack found = ItemStack.EMPTY;
        for (Item item : BuiltInRegistries.ITEM) {
            if (BarakConfig.keyOf(item).equals(key)) {
                found = new ItemStack(item);
                break;
            }
        }
        ICONS.put(key, found);
        return found;
    }

    private void render(Screen screen, GuiGraphics g, int mouseX, int mouseY, float tickDelta) {
        try {
            if (!(screen instanceof ContainerScreen containerScreen)) {
                return;
            }
            Ui.apply();
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            AbstractContainerMenu menu = containerScreen.getMenu();

            Map<String, Integer> counts = new LinkedHashMap<>();
            for (String key : BarakConfig.invseeItems) {
                counts.put(key, 0);
            }
            for (Slot slot : menu.slots) {
                if (slot.container instanceof Inventory) {
                    continue;
                }
                ItemStack stack = slot.getItem();
                if (stack.isEmpty()) {
                    continue;
                }
                String key = BarakConfig.keyOf(stack.getItem());
                Integer old = counts.get(key);
                if (old != null) {
                    counts.put(key, old + stack.getCount());
                }
            }

            int rows = Math.max(1, counts.size());
            int w = 142;
            int h = 34 + rows * 18 + 6;
            int left = (screen.width - 176) / 2;
            int fullX = Math.max(2, left - w - 8);
            int y = Math.max(2, (screen.height - 166) / 2);

            double p = Ui.fx(BarakConfig.animPopout) ? (System.currentTimeMillis() - created) / (320.0 / Ui.speed()) : 1.0;
            double e = p >= 1.0 ? 1.0 : Ui.easeOutBack(p);
            int curW = (int) Math.max(8, Math.min(w + 6, w * e));
            // Pop-out: das Fenster wächst vom Inventar nach links heraus
            int x = fullX + (w - curW);

            Ui.card(g, x, y, curW, h, Ui.BG | 0xF0000000, Ui.alpha(Ui.PINK, 0.9f));
            if (p < 0.6) {
                return;
            }

            g.drawString(mc.font, Lang.t("Invsee"), x + 8, y + 6, Ui.PINK);
            String who = target.isEmpty() ? Lang.t("Inventar") : target;
            g.drawString(mc.font, mc.font.plainSubstrByWidth(who, w - 16), x + 8, y + 18, Ui.MUTED);

            int row = 0;
            for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                int ry = y + 32 + row * 18;
                ItemStack icon = iconFor(entry.getKey());
                int amount = entry.getValue();
                Ui.card(g, x + 6, ry, w - 12, 16, Ui.mix(Ui.CARD, 0xFF000000, 0.2f), amount > 0 ? Ui.PINK_DARK : Ui.CARD_LINE);
                if (!icon.isEmpty()) {
                    g.renderItem(icon, x + 8, ry);
                }
                String text = amount + "x " + (icon.isEmpty() ? entry.getKey() : icon.getHoverName().getString());
                text = mc.font.plainSubstrByWidth(text, w - 38);
                g.drawString(mc.font, text, x + 28, ry + 4, amount > 0 ? Ui.WHITE : Ui.MUTED);
                row++;
            }
            if (counts.isEmpty()) {
                g.drawString(mc.font, Lang.t("Nichts ausgewählt"), x + 8, y + 36, Ui.MUTED);
            }
        } catch (Exception ex) {
            BarakPvPTools.LOGGER.warn("Fehler im Invsee-Panel", ex);
        }
    }
}
