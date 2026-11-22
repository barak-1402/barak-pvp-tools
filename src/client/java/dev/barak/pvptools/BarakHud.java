package dev.barak.pvptools;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;
import java.util.Map;

/** HUD: kleines Logo oben links, Auktions-/Giveaway-Anzeige oben rechts. */
public final class BarakHud {
    private BarakHud() {
    }

    public static void render(GuiGraphics g, DeltaTracker deltaTracker) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.player == null || mc.options.hideGui) {
                return;
            }
            if (BarakConfig.showLogo) {
                drawLogo(g, 4, 4, 2);
                g.drawString(mc.font, "Barak PvP Tools", 24, 8, Ui.PINK);
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

    /** Zeichnet das Logo; step = 1 volle Größe (32px), 2 halbe Größe (16px). */
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
        int w = 168;
        int x = mc.getWindow().getGuiScaledWidth() - w - 4;
        List<Map.Entry<String, Long>> rank = AuctionState.ranking();
        int lines = Math.min(3, rank.size());
        int h = 62 + lines * 10;
        double t = System.currentTimeMillis() / 1000.0;

        Ui.card(g, x, top, w, h, 0xE0240D1B, Ui.alpha(Ui.PINK, (float) (0.7 + 0.3 * Math.sin(t * 3))));

        g.renderItem(AuctionState.item, x + 5, top + 6);
        g.drawString(mc.font, AuctionState.itemName(), x + 26, top + 10, Ui.WHITE);

        boolean running = AuctionState.phase == AuctionState.Phase.RUNNING;
        String time = running ? AuctionState.formatTime(AuctionState.secondsLeft()) : "Beendet";
        g.drawString(mc.font, time, x + 6, top + 26, running ? Ui.PINK : Ui.RED);
        g.drawString(mc.font, "Min: " + AuctionState.money(AuctionState.startBid), x + 70, top + 26, Ui.GOLD);

        g.fill(x + 6, top + 38, x + w - 6, top + 41, Ui.GHOST);
        int bar = (int) ((w - 12) * AuctionState.progress());
        if (bar > 0) {
            g.fill(x + 6, top + 38, x + 6 + bar, top + 41, Ui.PINK);
        }

        g.drawString(mc.font, "Einnahmen: " + AuctionState.money(AuctionState.income), x + 6, top + 46, Ui.WHITE);
        for (int i = 0; i < lines; i++) {
            Map.Entry<String, Long> e = rank.get(i);
            g.drawString(mc.font, (i + 1) + ". " + e.getKey() + "  " + AuctionState.money(e.getValue()), x + 6, top + 58 + i * 10, i == 0 ? Ui.GOLD : Ui.MUTED);
        }
        return top + h + 4;
    }

    private static void drawGiveaway(GuiGraphics g, Minecraft mc, int top) {
        int w = 168;
        int x = mc.getWindow().getGuiScaledWidth() - w - 4;
        int h = 40;
        double t = System.currentTimeMillis() / 1000.0;
        Ui.card(g, x, top, w, h, 0xE0240D1B, Ui.alpha(Ui.PINK, (float) (0.7 + 0.3 * Math.sin(t * 3))));
        g.drawString(mc.font, "Giveaway: " + GiveawayState.prize, x + 6, top + 6, Ui.WHITE);
        boolean running = GiveawayState.phase == GiveawayState.Phase.RUNNING;
        String line = running
                ? AuctionState.formatTime(GiveawayState.secondsLeft()) + " | " + GiveawayState.participants().size() + " Teilnehmer"
                : (GiveawayState.winner == null ? "Kein Gewinner" : "Gewinner: " + GiveawayState.winner);
        g.drawString(mc.font, line, x + 6, top + 18, running ? Ui.PINK : Ui.GOLD);
        g.fill(x + 6, top + 31, x + w - 6, top + 34, Ui.GHOST);
        int bar = (int) ((w - 12) * GiveawayState.progress());
        if (bar > 0) {
            g.fill(x + 6, top + 31, x + 6 + bar, top + 34, Ui.PINK);
        }
    }
}
