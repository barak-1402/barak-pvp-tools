package dev.barak.pvptools;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Hauptfenster: Item-Größe, Auktion, Giveaway, Einstellungen. */
public class BarakScreen extends Screen {
    private static final int PW = 400;
    private static final int PH = 240;

    private static final int PURPLE = 0xFF6D28D9;
    private static final int TEAL = 0xFF2DD4BF;
    private static final int GRAY = 0xFFAAAAAA;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int RED = 0xFFFF6B6B;
    private static final int GOLD = 0xFFFFD166;

    private static int tab = 0;

    private int px;
    private int py;
    private int cx;

    public BarakScreen() {
        super(Component.literal("Barak PvP Tools"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        BarakConfig.save();
    }

    // ---------------------------------------------------------------- Aufbau

    @Override
    protected void init() {
        px = (this.width - PW) / 2;
        py = (this.height - PH) / 2;
        cx = px + 102;

        String[] names = {"Item-Größe", "Auktion", "Giveaway", "Einstellungen", "Discord"};
        for (int i = 0; i < names.length; i++) {
            final int index = i;
            this.addRenderableWidget(Button.builder(Component.literal(names[i]), b -> {
                tab = index;
                this.rebuildWidgets();
            }).bounds(px + 8, py + 44 + i * 24, 84, 20).build());
        }

        // X oben rechts: bricht bei laufender Auktion diese ab, sonst schließt es das Fenster
        this.addRenderableWidget(Button.builder(Component.literal("X"), b -> {
            if (tab == 1 && AuctionState.phase == AuctionState.Phase.RUNNING) {
                AuctionState.cancel();
                this.rebuildWidgets();
            } else {
                this.onClose();
            }
        }).bounds(px + PW - 22, py + 5, 16, 16).build());

        switch (tab) {
            case 0 -> initSize();
            case 1 -> initAuction();
            case 2 -> initGiveaway();
            case 3 -> initSettings();
            default -> initDiscord();
        }
    }

    private EditBox box(int x, int y, int w, String hint, String value, java.util.function.Consumer<String> responder, String allowed) {
        EditBox b = new EditBox(this.font, x, y, w, 18, Component.literal(hint));
        b.setMaxLength(60);
        if (allowed != null) {
            b.setFilter(s -> s.matches(allowed));
        }
        b.setHint(Component.literal(hint));
        b.setValue(value);
        b.setResponder(responder);
        this.addRenderableWidget(b);
        return b;
    }

    private void button(String label, int x, int y, int w, Runnable action) {
        this.addRenderableWidget(Button.builder(Component.literal(label), b -> action.run()).bounds(x, y, w, 20).build());
    }

    // ---------------------------------------------------------------- Tab 0: Item-Größe

    private void initSize() {
        int y = py + 78;
        sizeButton("-0.25", -0.25f, cx, y);
        sizeButton("-0.05", -0.05f, cx + 56, y);
        sizeButton("+0.05", 0.05f, cx + 112, y);
        sizeButton("+0.25", 0.25f, cx + 168, y);

        int y2 = py + 108;
        presetButton("0.5x", 0.5f, cx, y2);
        presetButton("1x", 1.0f, cx + 56, y2);
        presetButton("1.5x", 1.5f, cx + 112, y2);
        presetButton("2x", 2.0f, cx + 168, y2);

        button("Zurücksetzen", cx, py + 138, 108, () -> setScale(1.0f));
    }

    private void sizeButton(String label, float delta, int x, int y) {
        button(label, x, y, 52, () -> setScale(BarakConfig.itemScale + delta));
    }

    private void presetButton(String label, float value, int x, int y) {
        button(label, x, y, 52, () -> setScale(value));
    }

    private static void setScale(float value) {
        BarakConfig.itemScale = BarakConfig.clampScale(value);
        BarakConfig.save();
    }

    // ---------------------------------------------------------------- Tab 1: Auktion

    private void initAuction() {
        AuctionState.Phase phase = AuctionState.phase;
        if (phase == AuctionState.Phase.IDLE) {
            box(cx, py + 70, 130, "Startgebot (z.B. 5m)", AuctionState.draftBid, s -> AuctionState.draftBid = s, "[0-9.,kKmMbB$]*");
            box(cx, py + 96, 130, "Dauer in Sekunden", AuctionState.draftSeconds, s -> AuctionState.draftSeconds = s, "[0-9]*");
            button("Auktion starten", cx, py + 124, 130, this::startAuction);
        } else if (phase == AuctionState.Phase.RUNNING) {
            button("Im Chat ankündigen", cx, py + PH - 30, 140, this::announceAuction);
            button("Abbrechen", cx + 146, py + PH - 30, 100, () -> {
                AuctionState.cancel();
                this.rebuildWidgets();
            });
        } else {
            if (AuctionState.winner != null) {
                button("Teleport-Command kopieren", cx, py + PH - 30, 160, () -> {
                    Minecraft.getInstance().keyboardHandler.setClipboard(AuctionState.teleportCommand());
                    AuctionState.status = "Kopiert: " + AuctionState.teleportCommand();
                });
            }
            button("Neue Auktion", cx + 166, py + PH - 30, 110, () -> {
                AuctionState.reset();
                AuctionState.status = "";
                this.rebuildWidgets();
            });
        }
    }

    private void startAuction() {
        Minecraft mc = Minecraft.getInstance();
        ItemStack held = mc.player == null ? ItemStack.EMPTY : mc.player.getMainHandItem();
        if (held.isEmpty()) {
            AuctionState.status = "Halte das Item in der Hand, das du versteigern willst.";
            return;
        }
        long bid = AuctionState.parseMoney(AuctionState.draftBid);
        if (bid < 0) {
            AuctionState.status = "Ungültiges Startgebot.";
            return;
        }
        int seconds;
        try {
            seconds = Integer.parseInt(AuctionState.draftSeconds.trim());
        } catch (NumberFormatException e) {
            AuctionState.status = "Ungültige Dauer.";
            return;
        }
        if (seconds < 10 || seconds > 3600) {
            AuctionState.status = "Dauer: 10 bis 3600 Sekunden.";
            return;
        }
        AuctionState.status = "";
        AuctionState.start(held, bid, seconds);
        this.rebuildWidgets();
    }

    private void announceAuction() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        mc.player.connection.sendChat("Auktion: " + AuctionState.itemName() + " | Startgebot " + AuctionState.money(AuctionState.startBid)
                + " | " + AuctionState.formatTime(AuctionState.secondsLeft()) + " Min. | Wer am meisten zahlt, gewinnt! Zahle an " + mc.getUser().getName());
    }

    // ---------------------------------------------------------------- Tab 2: Giveaway

    private void initGiveaway() {
        GiveawayState.Phase phase = GiveawayState.phase;
        if (phase == GiveawayState.Phase.IDLE) {
            box(cx, py + 70, 240, "Preis (z.B. 1x Netherite-Schwert)", GiveawayState.draftPrize, s -> GiveawayState.draftPrize = s, null);
            box(cx, py + 96, 120, "Wort zum Mitmachen", GiveawayState.draftKeyword, s -> GiveawayState.draftKeyword = s, null);
            box(cx + 126, py + 96, 114, "Dauer in Sekunden", GiveawayState.draftSeconds, s -> GiveawayState.draftSeconds = s, "[0-9]*");
            button("Giveaway starten", cx, py + 124, 130, this::startGiveaway);
        } else if (phase == GiveawayState.Phase.RUNNING) {
            button("Im Chat ankündigen", cx, py + PH - 30, 130, this::announceGiveaway);
            button("Jetzt auslosen", cx + 136, py + PH - 30, 100, () -> {
                GiveawayState.draw();
                this.rebuildWidgets();
            });
            button("Abbrechen", cx + 242, py + PH - 30, 50, () -> {
                GiveawayState.cancel();
                this.rebuildWidgets();
            });
        } else {
            if (GiveawayState.winner != null) {
                button("Gewinner im Chat verkünden", cx, py + PH - 30, 170, () -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player != null) {
                        mc.player.connection.sendChat("Giveaway-Gewinner: " + GiveawayState.winner + " gewinnt " + GiveawayState.prize + "! Glückwunsch!");
                    }
                });
            }
            button("Neues Giveaway", cx + 176, py + PH - 30, 110, () -> {
                GiveawayState.reset();
                GiveawayState.status = "";
                this.rebuildWidgets();
            });
        }
    }

    private void startGiveaway() {
        String prize = GiveawayState.draftPrize.trim();
        String word = GiveawayState.draftKeyword.trim();
        if (prize.isEmpty()) {
            GiveawayState.status = "Gib einen Preis ein.";
            return;
        }
        if (word.isEmpty()) {
            GiveawayState.status = "Gib ein Wort zum Mitmachen ein.";
            return;
        }
        int seconds;
        try {
            seconds = Integer.parseInt(GiveawayState.draftSeconds.trim());
        } catch (NumberFormatException e) {
            GiveawayState.status = "Ungültige Dauer.";
            return;
        }
        if (seconds < 10 || seconds > 3600) {
            GiveawayState.status = "Dauer: 10 bis 3600 Sekunden.";
            return;
        }
        GiveawayState.status = "";
        GiveawayState.start(prize, word, seconds);
        this.rebuildWidgets();
    }

    private void announceGiveaway() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.connection.sendChat("Giveaway: " + GiveawayState.prize + " | Schreibe \"" + GiveawayState.keyword + "\" in den Chat, um teilzunehmen! ("
                    + AuctionState.formatTime(GiveawayState.secondsLeft()) + " Min.)");
        }
    }

    // ---------------------------------------------------------------- Tab 3: Einstellungen

    private void initSettings() {
        toggle(py + 36, "Logo anzeigen", BarakConfig.showLogo, v -> BarakConfig.showLogo = v);
        toggle(py + 60, "Info-Anzeige (Koordinaten)", BarakConfig.showInfo, v -> BarakConfig.showInfo = v);
        toggle(py + 84, "Auktion/Giveaway-Anzeige oben rechts", BarakConfig.showAuctionHud, v -> BarakConfig.showAuctionHud = v);
        toggle(py + 108, "Discord-Hinweis beim Beitreten", BarakConfig.showJoinMessage, v -> BarakConfig.showJoinMessage = v);
        box(cx, py + 154, 170, "Teleport-Command", BarakConfig.tpCommand, s -> BarakConfig.tpCommand = s, null);
    }

    // ---------------------------------------------------------------- Tab 4: Discord

    private static String discordStatus = "";

    private void initDiscord() {
        button("Discord beitreten", cx, py + 96, 140, () -> {
            try {
                new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", BarakPvPTools.DISCORD_URL).start();
                discordStatus = "Link im Browser geöffnet.";
            } catch (Throwable t) {
                Minecraft.getInstance().keyboardHandler.setClipboard(BarakPvPTools.DISCORD_URL);
                discordStatus = "Konnte den Browser nicht öffnen – Link kopiert.";
            }
        });
        button("Link kopieren", cx + 146, py + 96, 110, () -> {
            Minecraft.getInstance().keyboardHandler.setClipboard(BarakPvPTools.DISCORD_URL);
            discordStatus = "Link kopiert!";
        });
    }

    private void renderDiscord(GuiGraphics g) {
        g.drawString(this.font, "Join my Discord!", cx, py + 40, WHITE);
        g.drawString(this.font, BarakPvPTools.DISCORD_URL, cx, py + 62, TEAL);
        g.drawString(this.font, "Updates, Support und neue Features.", cx, py + 76, GRAY);
        if (!discordStatus.isEmpty()) {
            g.drawString(this.font, discordStatus, cx, py + 126, GOLD);
        }
    }

    private void toggle(int y, String label, boolean value, java.util.function.Consumer<Boolean> setter) {
        final boolean[] state = {value};
        Button b = Button.builder(Component.literal(label + ": " + (value ? "AN" : "AUS")), btn -> {
            state[0] = !state[0];
            setter.accept(state[0]);
            btn.setMessage(Component.literal(label + ": " + (state[0] ? "AN" : "AUS")));
            BarakConfig.save();
        }).bounds(cx, y, 250, 20).build();
        this.addRenderableWidget(b);
    }

    // ---------------------------------------------------------------- Zeichnen

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        g.fill(px, py, px + PW, py + PH, 0xF0121018);
        g.fill(px, py, px + PW, py + 26, PURPLE);
        g.fill(px, py + 26, px + 94, py + PH, 0xFF1B1726);
        g.fill(px + 2, py + 44 + tab * 24, px + 5, py + 64 + tab * 24, TEAL);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);

        BarakHud.drawLogo(g, px + 6, py + 5, 2);
        g.drawString(this.font, "Barak PvP Tools", px + 28, py + 9, WHITE);

        switch (tab) {
            case 0 -> renderSize(g);
            case 1 -> renderAuction(g);
            case 2 -> renderGiveaway(g);
            case 3 -> renderSettings(g);
            default -> renderDiscord(g);
        }
    }

    private void renderSize(GuiGraphics g) {
        g.drawString(this.font, "Größe des Items in deiner Hand", cx, py + 36, WHITE);
        g.drawString(this.font, "Aktuell: " + String.format(Locale.ROOT, "%.2f", BarakConfig.itemScale) + "x", cx, py + 54, TEAL);
        g.drawString(this.font, "Bereich: 0.2x bis 3.0x. Nur für dich sichtbar.", cx, py + PH - 18, GRAY);
    }

    private void renderAuction(GuiGraphics g) {
        AuctionState.Phase phase = AuctionState.phase;
        if (phase == AuctionState.Phase.IDLE) {
            Minecraft mc = Minecraft.getInstance();
            ItemStack held = mc.player == null ? ItemStack.EMPTY : mc.player.getMainHandItem();
            g.drawString(this.font, "Neue Auktion", cx, py + 36, WHITE);
            if (held.isEmpty()) {
                g.drawString(this.font, "Item: (nichts in der Hand)", cx, py + 52, GRAY);
            } else {
                g.renderItem(held, cx, py + 46);
                g.drawString(this.font, held.getCount() + "x " + held.getHoverName().getString(), cx + 22, py + 51, GOLD);
            }
            g.drawString(this.font, "Wer am meisten zahlt, bekommt das Item.", cx, py + 152, GRAY);
            g.drawString(this.font, "Zahlungen werden im Chat erkannt.", cx, py + 164, GRAY);
        } else {
            boolean running = phase == AuctionState.Phase.RUNNING;
            g.renderItem(AuctionState.item, cx, py + 32);
            g.drawString(this.font, AuctionState.itemName(), cx + 22, py + 37, WHITE);

            String time = running ? "Zeit: " + AuctionState.formatTime(AuctionState.secondsLeft()) : "Beendet";
            g.drawString(this.font, time, cx, py + 56, running ? TEAL : RED);
            g.drawString(this.font, "Startgebot: " + AuctionState.money(AuctionState.startBid), cx + 100, py + 56, GRAY);

            g.fill(cx, py + 68, cx + 270, py + 72, 0xFF2A2536);
            int bar = (int) (270 * AuctionState.progress());
            if (bar > 0) {
                g.fill(cx, py + 68, cx + bar, py + 72, TEAL);
            }

            g.drawString(this.font, "Einnahmen: " + AuctionState.money(AuctionState.income), cx, py + 80, WHITE);
            if (!running) {
                if (AuctionState.winner != null) {
                    g.drawString(this.font, "Gewinner: " + AuctionState.winner + " (" + AuctionState.money(AuctionState.winnerAmount) + ")", cx + 130, py + 80, GOLD);
                } else {
                    g.drawString(this.font, "Kein Gewinner", cx + 130, py + 80, RED);
                }
            }

            List<Map.Entry<String, Long>> rank = AuctionState.ranking();
            g.drawString(this.font, "Top 3", cx, py + 98, TEAL);
            if (rank.isEmpty()) {
                g.drawString(this.font, "Noch keine Zahlungen.", cx, py + 110, GRAY);
            }
            for (int i = 0; i < Math.min(3, rank.size()); i++) {
                Map.Entry<String, Long> e = rank.get(i);
                g.drawString(this.font, (i + 1) + ". " + e.getKey() + "  " + AuctionState.money(e.getValue()), cx, py + 110 + i * 11, i == 0 ? GOLD : WHITE);
            }
            if (rank.size() > 3) {
                g.drawString(this.font, "Weitere Gebote", cx + 150, py + 98, TEAL);
                for (int i = 3; i < Math.min(8, rank.size()); i++) {
                    Map.Entry<String, Long> e = rank.get(i);
                    g.drawString(this.font, (i + 1) + ". " + e.getKey() + "  " + AuctionState.money(e.getValue()), cx + 150, py + 110 + (i - 3) * 11, GRAY);
                }
            }
        }
        if (!AuctionState.status.isEmpty()) {
            g.drawString(this.font, AuctionState.status, cx, py + PH - 46, AuctionState.status.startsWith("Kopiert") ? TEAL : RED);
        }
    }

    private void renderGiveaway(GuiGraphics g) {
        GiveawayState.Phase phase = GiveawayState.phase;
        if (phase == GiveawayState.Phase.IDLE) {
            g.drawString(this.font, "Neues Giveaway", cx, py + 36, WHITE);
            g.drawString(this.font, "Wer das Wort in den Chat schreibt, nimmt teil.", cx, py + 152, GRAY);
        } else {
            boolean running = phase == GiveawayState.Phase.RUNNING;
            g.drawString(this.font, "Preis: " + GiveawayState.prize, cx, py + 36, GOLD);
            String time = running ? "Zeit: " + AuctionState.formatTime(GiveawayState.secondsLeft()) : "Beendet";
            g.drawString(this.font, time, cx, py + 52, running ? TEAL : RED);
            g.drawString(this.font, "Wort: " + GiveawayState.keyword, cx + 100, py + 52, GRAY);

            g.fill(cx, py + 64, cx + 270, py + 68, 0xFF2A2536);
            int bar = (int) (270 * GiveawayState.progress());
            if (bar > 0) {
                g.fill(cx, py + 64, cx + bar, py + 68, 0xFFEC4899);
            }

            List<String> list = GiveawayState.participants();
            g.drawString(this.font, "Teilnehmer: " + list.size(), cx, py + 78, WHITE);
            if (!running) {
                g.drawString(this.font, GiveawayState.winner == null ? "Kein Gewinner" : "Gewinner: " + GiveawayState.winner, cx + 130, py + 78, GOLD);
            }
            for (int row = 0; row < 4; row++) {
                StringBuilder sb = new StringBuilder();
                for (int col = 0; col < 3; col++) {
                    int i = row * 3 + col;
                    if (i < list.size()) {
                        if (sb.length() > 0) {
                            sb.append(", ");
                        }
                        sb.append(list.get(i));
                    }
                }
                if (sb.length() > 0) {
                    g.drawString(this.font, sb.toString(), cx, py + 92 + row * 11, GRAY);
                }
            }
            if (list.size() > 12) {
                g.drawString(this.font, "… und " + (list.size() - 12) + " weitere", cx, py + 92 + 4 * 11, GRAY);
            }
        }
        if (!GiveawayState.status.isEmpty()) {
            g.drawString(this.font, GiveawayState.status, cx, py + PH - 46, RED);
        }
    }

    private void renderSettings(GuiGraphics g) {
        g.drawString(this.font, "Teleport-Command für den Gewinner ({name} = Spielername):", cx, py + 142, GRAY);
    }
}
