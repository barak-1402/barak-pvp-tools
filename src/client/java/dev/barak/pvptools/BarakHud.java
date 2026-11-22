package dev.barak.pvptools;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/** HUD: Logo oben links, Auktions-/Giveaway-Anzeige oben rechts. */
public final class BarakHud {
    private BarakHud() {
    }

    public static void render(GuiGraphics g, DeltaTracker deltaTracker) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.player == null || mc.options.hideGui) {
                return;
            }
            int y = 4;
            if (BarakConfig.showLogo) {
                drawLogo(g, 4, 4, 1);
                g.drawString(mc.font, "Barak PvP Tools", 4 + LogoData.SIZE + 5, 4 + 12, 0xFFFFFFFF);
                y = 4 + LogoData.SIZE + 4;
            }
            if (BarakConfig.showInfo) {
                g.drawString(mc.font, "XYZ: " + mc.player.getBlockX() + " " + mc.player.getBlockY() + " " + mc.player.getBlockZ(), 4, y, 0xFFFFFFFF);
                g.drawString(mc.font, "Item-Größe: " + String.format(Locale.ROOT, "%.2f", BarakConfig.itemScale) + "x", 4, y + 10, 0xFF2DD4BF);
            }
            if (BarakConfig.showAuctionHud) {
                int top = 4;
                if (AuctionState.phase != AuctionState.Phase.IDLE) {
                    top = drawAuction(g, mc, top);
                }
                if (GiveawayState.phase != GiveawayState.Phase.IDLE) {
                    drawGiveaway(g, mc, top);
                }
            }
        } catch (Exception ignored) {
            // Ein Fehler im HUD darf nie das Spiel zum Absturz bringen.
        }
    }

    /** Zeichnet das Logo; step = 1 volle Größe, 2 halbe Größe. */
    public static void drawLogo(GuiGraphics g, int x, int y, int step) {
        int size = LogoData.SIZE;
        for (int py = 0; py < size; py += step) {
            for (int px = 0; px < size; px += step) {
                int argb = LogoData.PIXELS[py * size + px];
                if (argb != 0) {
                    int dx = x + px / step;
                    int dy = y + py / step;
                    g.fill(dx, dy, dx + 1, dy + 1, argb);
                }
            }
        }
    }

    private static int drawAuction(GuiGraphics g, Minecraft mc, int top) {
        int w = 160;
        int x = mc.getWindow().getGuiScaledWidth() - w - 4;
        List<Map.Entry<String, Long>> rank = AuctionState.ranking();
        int lines = Math.min(3, rank.size());
        int h = 62 + lines * 10;

        g.fill(x, top, x + w, top + h, 0xCC15121C);
        g.fill(x, top, x + w, top + 2, 0xFF6D28D9);

        g.renderItem(AuctionState.item, x + 4, top + 6);
        g.drawString(mc.font, AuctionState.itemName(), x + 24, top + 10, 0xFFFFFFFF);

        boolean running = AuctionState.phase == AuctionState.Phase.RUNNING;
        String time = running ? "Zeit: " + AuctionState.formatTime(AuctionState.secondsLeft()) : "Beendet";
        g.drawString(mc.font, time, x + 4, top + 26, running ? 0xFF2DD4BF : 0xFFFF6B6B);
        g.drawString(mc.font, "Start: " + AuctionState.money(AuctionState.startBid), x + 80, top + 26, 0xFFAAAAAA);

        // Fortschrittsbalken
        g.fill(x + 4, top + 38, x + w - 4, top + 41, 0xFF2A2536);
        int bar = (int) ((w - 8) * AuctionState.progress());
        if (bar > 0) {
            g.fill(x + 4, top + 38, x + 4 + bar, top + 41, 0xFF2DD4BF);
        }

        g.drawString(mc.font, "Einnahmen: " + AuctionState.money(AuctionState.income), x + 4, top + 46, 0xFFFFFFFF);
        for (int i = 0; i < lines; i++) {
            Map.Entry<String, Long> e = rank.get(i);
            g.drawString(mc.font, (i + 1) + ". " + e.getKey() + "  " + AuctionState.money(e.getValue()), x + 4, top + 58 + i * 10, i == 0 ? 0xFFFFD166 : 0xFFDDDDDD);
        }
        return top + h + 4;
    }

    private static void drawGiveaway(GuiGraphics g, Minecraft mc, int top) {
        int w = 160;
        int x = mc.getWindow().getGuiScaledWidth() - w - 4;
        int h = 38;
        g.fill(x, top, x + w, top + h, 0xCC15121C);
        g.fill(x, top, x + w, top + 2, 0xFFEC4899);
        g.drawString(mc.font, "Giveaway: " + GiveawayState.prize, x + 4, top + 6, 0xFFFFFFFF);
        boolean running = GiveawayState.phase == GiveawayState.Phase.RUNNING;
        String line = running
                ? AuctionState.formatTime(GiveawayState.secondsLeft()) + " | " + GiveawayState.participants().size() + " Teilnehmer"
                : (GiveawayState.winner == null ? "Kein Gewinner" : "Gewinner: " + GiveawayState.winner);
        g.drawString(mc.font, line, x + 4, top + 18, running ? 0xFF2DD4BF : 0xFFFFD166);
        g.fill(x + 4, top + 30, x + w - 4, top + 33, 0xFF2A2536);
        int bar = (int) ((w - 8) * GiveawayState.progress());
        if (bar > 0) {
            g.fill(x + 4, top + 30, x + 4 + bar, top + 33, 0xFFEC4899);
        }
    }
}
