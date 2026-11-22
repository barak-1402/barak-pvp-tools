package dev.barak.pvptools;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Hauptfenster: Item-Größe, Auktion, Giveaway, Einstellungen (mit wählbaren Designs und Animationen). */
public class BarakScreen extends Screen {
    private static final int PW = 440;
    private static final int PH = 256;
    private static final long AD_COOLDOWN_MS = 10_000L;

    private enum Kind { PRIMARY, OUTLINE, BLUE, DANGER }

    private record Fancy(Button button, Supplier<String> label, Kind kind, float[] hover) {
    }

    private record Cell(Button button, int x, int y, int size, Supplier<ItemStack> stack, BooleanSupplier selected) {
    }

    private record Swatch(Button button, int x, int y, int index) {
    }

    private record BoxBg(EditBox box, int x, int y, int w) {
    }

    private static final Set<String> CURATED = Set.of(
            "minecraft:end_crystal", "minecraft:totem_of_undying", "minecraft:obsidian", "minecraft:respawn_anchor",
            "minecraft:glowstone", "minecraft:ender_pearl", "minecraft:golden_apple", "minecraft:enchanted_golden_apple",
            "minecraft:netherite_sword", "minecraft:netherite_axe", "minecraft:netherite_helmet", "minecraft:netherite_chestplate",
            "minecraft:netherite_leggings", "minecraft:netherite_boots", "minecraft:diamond_sword", "minecraft:elytra",
            "minecraft:experience_bottle", "minecraft:tnt", "minecraft:shield", "minecraft:bow", "minecraft:crossbow",
            "minecraft:mace", "minecraft:wind_charge", "minecraft:trident");

    private static int tab = 0;
    private static String toast = "";
    private static long toastUntil = 0L;
    private static long auctionAdUntil = 0L;
    private static long giveawayAdUntil = 0L;
    private static boolean errorLogged = false;

    private static String itemQuery = "";
    private static Item selectedItem = null;
    private static final List<Item> RESULTS = new ArrayList<>();
    private static final List<ItemStack> RESULT_STACKS = new ArrayList<>();

    private int px;
    private int py;
    private int bx;
    private Button adButton;
    private final long openMillis = System.currentTimeMillis();
    private final List<Fancy> fancies = new ArrayList<>();
    private final List<Cell> cells = new ArrayList<>();
    private final List<Swatch> swatches = new ArrayList<>();
    private final List<BoxBg> boxes = new ArrayList<>();

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

    // ================================================================ Aufbau

    private int by() {
        return py + PH - 32;
    }

    /** Unsichtbarer, klickbarer Bereich. Wird absichtlich NICHT gezeichnet (addWidget statt addRenderableWidget). */
    private Button hit(int x, int y, int w, int h, Runnable action) {
        Button b = Button.builder(Component.empty(), btn -> action.run()).bounds(x, y, w, h).build();
        this.addWidget(b);
        return b;
    }

    private Button fancy(Supplier<String> label, int x, int y, int w, int h, Kind kind, Runnable action) {
        Button b = hit(x, y, w, h, action);
        this.fancies.add(new Fancy(b, label, kind, new float[]{0f}));
        return b;
    }

    private Button bottomButton(String label, int w, Kind kind, Runnable action) {
        Button b = fancy(() -> label, bx, by(), w, 20, kind, action);
        bx += w + 6;
        return b;
    }

    private void bottomStart() {
        bx = px + 12;
        fancy(() -> "Join my Discord", bx, by(), 122, 20, Kind.BLUE, this::openDiscord);
        bx += 128;
    }

    private void cell(int x, int y, int size, Supplier<ItemStack> stack, BooleanSupplier selected, Runnable action) {
        Button b = hit(x, y, size, size, action);
        this.cells.add(new Cell(b, x, y, size, stack, selected));
    }

    private EditBox box(int x, int y, int w, String hint, String value, Consumer<String> responder, String allowed) {
        EditBox b = new EditBox(this.font, x + 5, y, w - 10, 18, Component.literal(hint));
        b.setBordered(false);
        b.setMaxLength(60);
        if (allowed != null) {
            b.setFilter(s -> s.matches(allowed));
        }
        b.setHint(Component.literal(hint));
        b.setValue(value);
        b.setResponder(responder);
        this.addRenderableWidget(b);
        this.boxes.add(new BoxBg(b, x, y, w));
        return b;
    }

    @Override
    protected void init() {
        px = Math.max(2, (this.width - PW) / 2);
        py = Math.max(2, (this.height - PH) / 2);
        fancies.clear();
        cells.clear();
        swatches.clear();
        boxes.clear();
        adButton = null;

        String[] names = {"Item-Größe", "Auktion", "Giveaway", "Einstellungen"};
        for (int i = 0; i < names.length; i++) {
            final int index = i;
            fancy(() -> names[index], px + 12 + i * 88, py + 34, 84, 18, i == tab ? Kind.PRIMARY : Kind.OUTLINE, () -> {
                tab = index;
                this.rebuildWidgets();
            });
        }

        switch (tab) {
            case 0 -> initSize();
            case 1 -> initAuction();
            case 2 -> initGiveaway();
            default -> initSettings();
        }
    }

    private void toast(String text) {
        toast = text;
        toastUntil = System.currentTimeMillis() + 3500L;
    }

    private void openDiscord() {
        try {
            new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", BarakPvPTools.DISCORD_URL).start();
            toast("Discord wird im Browser geöffnet …");
        } catch (Throwable t) {
            Minecraft.getInstance().keyboardHandler.setClipboard(BarakPvPTools.DISCORD_URL);
            toast("Browser nicht verfügbar – Link kopiert: " + BarakPvPTools.DISCORD_URL);
        }
    }

    private static String me() {
        return Minecraft.getInstance().getUser().getName();
    }

    private static void say(String text) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.connection.sendChat(text);
        }
    }

    /** Sicherer Zugriff auf einen Inventar-Slot (ungültige Slots geben ein leeres Item zurück). */
    private static ItemStack invStack(int slot) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || slot < 0) {
            return ItemStack.EMPTY;
        }
        if (slot >= mc.player.getInventory().getContainerSize()) {
            return ItemStack.EMPTY;
        }
        return mc.player.getInventory().getItem(slot);
    }

    // ---------------------------------------------------------------- Tab 0: Item-Größe

    private static void refreshResults() {
        RESULTS.clear();
        RESULT_STACKS.clear();
        String raw = itemQuery.trim().toLowerCase(Locale.ROOT);
        String q = raw.replace(' ', '_');
        for (Item item : BuiltInRegistries.ITEM) {
            if (RESULTS.size() >= 24) {
                break;
            }
            String key = BuiltInRegistries.ITEM.getKey(item).toString();
            if (key.equals("minecraft:air")) {
                continue;
            }
            boolean ok;
            if (q.isEmpty()) {
                ok = CURATED.contains(key);
            } else {
                String path = key.substring(key.indexOf(':') + 1);
                ok = path.contains(q) || item.getDefaultInstance().getHoverName().getString().toLowerCase(Locale.ROOT).contains(raw);
            }
            if (ok) {
                RESULTS.add(item);
                RESULT_STACKS.add(new ItemStack(item));
            }
        }
    }

    private static List<Item> savedItems() {
        List<Item> list = new ArrayList<>();
        if (BarakConfig.itemScales.isEmpty()) {
            return list;
        }
        for (Item item : BuiltInRegistries.ITEM) {
            if (BarakConfig.itemScales.containsKey(BarakConfig.keyOf(item))) {
                list.add(item);
                if (list.size() >= 8) {
                    break;
                }
            }
        }
        return list;
    }

    private void initSize() {
        if (RESULTS.isEmpty()) {
            refreshResults();
        }
        box(px + 16, py + 68, 224, "Item suchen (z.B. crystal)", itemQuery, s -> {
            itemQuery = s;
            refreshResults();
        }, null);

        for (int i = 0; i < 24; i++) {
            final int index = i;
            int cx = px + 20 + (i % 8) * 26;
            int cy = py + 94 + (i / 8) * 26;
            cell(cx, cy, 24,
                    () -> index < RESULT_STACKS.size() ? RESULT_STACKS.get(index) : ItemStack.EMPTY,
                    () -> index < RESULTS.size() && RESULTS.get(index) == selectedItem,
                    () -> {
                        if (index < RESULTS.size()) {
                            selectedItem = RESULTS.get(index);
                        }
                    });
        }

        int rx = px + 258;
        sizeButton("-0.5", -0.5f, rx, py + 118);
        sizeButton("-0.25", -0.25f, rx + 42, py + 118);
        sizeButton("+0.25", 0.25f, rx + 84, py + 118);
        sizeButton("+0.5", 0.5f, rx + 126, py + 118);
        presetButton("1x", 1.0f, rx, py + 140);
        presetButton("2x", 2.0f, rx + 42, py + 140);
        presetButton("3x", 3.0f, rx + 84, py + 140);
        fancy(() -> "Weg", rx + 126, py + 140, 40, 18, Kind.DANGER, () -> {
            if (selectedItem != null) {
                BarakConfig.removeScale(selectedItem);
            }
        });
        fancy(() -> "Leuchten: " + (BarakConfig.glowDropped ? "AN" : "AUS"), rx, py + 162, 166, 18, Kind.OUTLINE, () -> {
            BarakConfig.glowDropped = !BarakConfig.glowDropped;
            BarakConfig.save();
        });

        List<Item> saved = savedItems();
        for (int i = 0; i < saved.size(); i++) {
            final Item item = saved.get(i);
            final ItemStack stack = new ItemStack(item);
            cell(rx + i * 21, py + 186, 20, () -> stack, () -> item == selectedItem, () -> selectedItem = item);
        }

        bottomStart();
        bottomButton("Schließen", 56, Kind.OUTLINE, this::onClose);
    }

    private void sizeButton(String label, float delta, int x, int y) {
        fancy(() -> label, x, y, 40, 18, Kind.OUTLINE, () -> {
            if (selectedItem != null) {
                BarakConfig.setScale(selectedItem, BarakConfig.scaleOf(selectedItem) + delta);
            }
        });
    }

    private void presetButton(String label, float value, int x, int y) {
        fancy(() -> label, x, y, 40, 18, Kind.OUTLINE, () -> {
            if (selectedItem != null) {
                BarakConfig.setScale(selectedItem, value);
            }
        });
    }

    // ---------------------------------------------------------------- Tab 1: Auktion

    private void initAuction() {
        AuctionState.Phase phase = AuctionState.phase;
        if (phase == AuctionState.Phase.IDLE) {
            for (int row = 0; row < 4; row++) {
                for (int col = 0; col < 9; col++) {
                    final int slot = row < 3 ? 9 + row * 9 + col : col;
                    cell(px + 20 + col * 24, py + 84 + row * 24 + (row == 3 ? 4 : 0), 22,
                            () -> invStack(slot), () -> AuctionState.selectedSlot == slot, () -> AuctionState.selectedSlot = slot);
                }
            }
            box(px + 256, py + 88, 168, "Mindestgebot (z.B. 2m)", AuctionState.draftBid, s -> AuctionState.draftBid = s, "[0-9.,kKmMbB$]*");
            box(px + 256, py + 128, 168, "Dauer in Sekunden", AuctionState.draftSeconds, s -> AuctionState.draftSeconds = s, "[0-9]*");

            bottomStart();
            bottomButton("Auktion starten", 110, Kind.PRIMARY, this::startAuction);
            bottomButton("Schließen", 56, Kind.OUTLINE, this::onClose);
        } else if (phase == AuctionState.Phase.RUNNING) {
            bottomStart();
            bottomButton("Beenden", 50, Kind.DANGER, () -> {
                AuctionState.endNow();
                this.rebuildWidgets();
            });
            bottomButton("Zurücksetzen", 76, Kind.OUTLINE, () -> {
                AuctionState.reset();
                AuctionState.status = "";
                this.rebuildWidgets();
            });
            adButton = bottomButton("Werbung", 80, Kind.PRIMARY, this::advertiseAuction);
            bottomButton("Schließen", 56, Kind.OUTLINE, this::onClose);
        } else {
            bottomStart();
            if (AuctionState.winner != null) {
                bottomButton("TP-Command kopieren", 120, Kind.PRIMARY, () -> {
                    Minecraft.getInstance().keyboardHandler.setClipboard(AuctionState.teleportCommand());
                    toast("Kopiert: " + AuctionState.teleportCommand());
                });
            }
            bottomButton("Zurücksetzen", 76, Kind.OUTLINE, () -> {
                AuctionState.reset();
                AuctionState.status = "";
                this.rebuildWidgets();
            });
            bottomButton("Schließen", 56, Kind.OUTLINE, this::onClose);
        }
    }

    private void startAuction() {
        ItemStack chosen = invStack(AuctionState.selectedSlot);
        if (chosen.isEmpty()) {
            AuctionState.status = "Wähle zuerst ein Item aus deinem Inventar.";
            return;
        }
        long bid = AuctionState.parseMoney(AuctionState.draftBid);
        if (bid < 0) {
            AuctionState.status = "Ungültiges Mindestgebot.";
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
        AuctionState.start(chosen, bid, seconds);
        this.rebuildWidgets();
    }

    private void advertiseAuction() {
        if (System.currentTimeMillis() < auctionAdUntil) {
            return;
        }
        auctionAdUntil = System.currentTimeMillis() + AD_COOLDOWN_MS;
        say("Auktion: " + AuctionState.itemName() + " | Mindestgebot " + AuctionState.money(AuctionState.startBid)
                + " | Mache /pay " + me() + " <Betrag> | noch " + AuctionState.formatTime(AuctionState.secondsLeft()));
    }

    // ---------------------------------------------------------------- Tab 2: Giveaway

    private void initGiveaway() {
        GiveawayState.Phase phase = GiveawayState.phase;
        if (phase == GiveawayState.Phase.IDLE) {
            box(px + 16, py + 88, 408, "Preis (z.B. 3x End Crystal)", GiveawayState.draftPrize, s -> GiveawayState.draftPrize = s, null);
            box(px + 16, py + 130, 200, "Wort zum Mitmachen", GiveawayState.draftKeyword, s -> GiveawayState.draftKeyword = s, null);
            box(px + 224, py + 130, 200, "Dauer in Sekunden", GiveawayState.draftSeconds, s -> GiveawayState.draftSeconds = s, "[0-9]*");

            bottomStart();
            bottomButton("Giveaway starten", 116, Kind.PRIMARY, this::startGiveaway);
            bottomButton("Schließen", 56, Kind.OUTLINE, this::onClose);
        } else if (phase == GiveawayState.Phase.RUNNING) {
            bottomStart();
            bottomButton("Auslosen", 56, Kind.DANGER, () -> {
                GiveawayState.draw();
                this.rebuildWidgets();
            });
            bottomButton("Zurücksetzen", 76, Kind.OUTLINE, () -> {
                GiveawayState.reset();
                GiveawayState.status = "";
                this.rebuildWidgets();
            });
            adButton = bottomButton("Werbung", 76, Kind.PRIMARY, this::advertiseGiveaway);
            bottomButton("Schließen", 56, Kind.OUTLINE, this::onClose);
        } else {
            bottomStart();
            if (GiveawayState.winner != null) {
                bottomButton("Gewinner verkünden", 116, Kind.PRIMARY, () ->
                        say("Giveaway-Gewinner: " + GiveawayState.winner + " gewinnt " + GiveawayState.prize + "! Glückwunsch!"));
            }
            bottomButton("Zurücksetzen", 76, Kind.OUTLINE, () -> {
                GiveawayState.reset();
                GiveawayState.status = "";
                this.rebuildWidgets();
            });
            bottomButton("Schließen", 56, Kind.OUTLINE, this::onClose);
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

    private void advertiseGiveaway() {
        if (System.currentTimeMillis() < giveawayAdUntil) {
            return;
        }
        giveawayAdUntil = System.currentTimeMillis() + AD_COOLDOWN_MS;
        say("Giveaway: " + GiveawayState.prize + " | Schreibe \"" + GiveawayState.keyword + "\" in den Chat! | noch "
                + AuctionState.formatTime(GiveawayState.secondsLeft()));
    }

    // ---------------------------------------------------------------- Tab 3: Einstellungen

    private void initSettings() {
        fancy(() -> "Logo oben links: " + (BarakConfig.showLogo ? "AN" : "AUS"), px + 20, py + 66, 236, 20, Kind.OUTLINE, () -> {
            BarakConfig.showLogo = !BarakConfig.showLogo;
            BarakConfig.save();
        });
        fancy(() -> "Auktion/Giveaway-Anzeige: " + (BarakConfig.showAuctionHud ? "AN" : "AUS"), px + 20, py + 90, 236, 20, Kind.OUTLINE, () -> {
            BarakConfig.showAuctionHud = !BarakConfig.showAuctionHud;
            BarakConfig.save();
        });
        fancy(() -> "Discord-Hinweis beim Beitreten: " + (BarakConfig.showJoinMessage ? "AN" : "AUS"), px + 20, py + 114, 236, 20, Kind.OUTLINE, () -> {
            BarakConfig.showJoinMessage = !BarakConfig.showJoinMessage;
            BarakConfig.save();
        });
        box(px + 16, py + 160, 244, "Teleport-Command", BarakConfig.tpCommand, s -> BarakConfig.tpCommand = s, null);

        for (int i = 0; i < Ui.THEMES.length; i++) {
            final int index = i;
            int sx = px + 284 + (i % 4) * 34;
            int sy = py + 94 + (i / 4) * 28;
            Button b = hit(sx, sy, 28, 22, () -> {
                BarakConfig.themeIndex = index;
                BarakConfig.save();
            });
            swatches.add(new Swatch(b, sx, sy, i));
        }

        bottomStart();
        bottomButton("Schließen", 56, Kind.OUTLINE, this::onClose);
    }

    // ================================================================ Zeichnen

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        try {
            Ui.apply();
            double t = System.currentTimeMillis() / 1000.0;
            float pulse = (float) (0.6 + 0.4 * Math.sin(t * 2.5));
            Ui.card(g, px, py, PW, PH, Ui.BG, Ui.alpha(Ui.PINK, pulse));

            boolean live = (tab == 1 && AuctionState.phase != AuctionState.Phase.IDLE)
                    || (tab == 2 && GiveawayState.phase != GiveawayState.Phase.IDLE);
            if (tab == 0 || (tab == 1 && !live)) {
                Ui.card(g, px + 12, py + 58, 232, 154, Ui.CARD, Ui.CARD_LINE);
                Ui.card(g, px + 252, py + 58, 176, 154, Ui.CARD, Ui.CARD_LINE);
            } else if (live) {
                Ui.card(g, px + 12, py + 94, 168, 70, Ui.CARD, Ui.CARD_LINE);
                Ui.card(g, px + 12, py + 168, 168, 46, Ui.CARD, Ui.CARD_LINE);
                Ui.card(g, px + 188, py + 94, 240, 120, Ui.CARD, Ui.CARD_LINE);
            } else {
                Ui.card(g, px + 12, py + 58, 416, 154, Ui.CARD, Ui.CARD_LINE);
            }

            // schwebende Partikel
            for (int i = 0; i < 16; i++) {
                int sx = px + 10 + (i * 53 + 17) % (PW - 20);
                int sy = py + PH - 6 - (int) ((t * (8 + i % 5) + i * 37) % (PH - 12));
                float tw = (float) (0.25 + 0.2 * Math.sin(t * 2 + i));
                g.fill(sx, sy, sx + 1 + (i % 2), sy + 1 + (i % 2), Ui.alpha(Ui.PINK, tw));
            }

            // wandernder Rand-Glanz
            Ui.borderGlow(g, px, py, PW, PH, Ui.PINK, t);

            // Hintergrund der Eingabefelder
            for (BoxBg b : boxes) {
                int border = b.box().isFocused() ? Ui.PINK : Ui.CARD_LINE;
                Ui.card(g, b.x(), b.y(), b.w(), 18, Ui.mix(Ui.CARD, 0xFF000000, 0.35f), border);
            }
        } catch (Exception e) {
            logOnce(e);
        }
    }

    private static void logOnce(Exception e) {
        if (!errorLogged) {
            errorLogged = true;
            BarakPvPTools.LOGGER.warn("Fehler beim Zeichnen des Menüs", e);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        try {
            if (adButton != null) {
                long until = tab == 1 ? auctionAdUntil : giveawayAdUntil;
                adButton.active = System.currentTimeMillis() >= until;
            }

            drawHeader(g);

            switch (tab) {
                case 0 -> renderSize(g);
                case 1 -> renderAuction(g);
                case 2 -> renderGiveaway(g);
                default -> renderSettings(g);
            }

            for (Cell c : cells) {
                drawCell(g, c, mouseX, mouseY);
            }
            for (Swatch s : swatches) {
                drawSwatch(g, s, mouseX, mouseY);
            }
            for (Fancy f : fancies) {
                if (f.kind() == Kind.BLUE) {
                    drawDiscord(g, f, mouseX, mouseY);
                } else {
                    drawFancy(g, f, mouseX, mouseY);
                }
            }

            if (System.currentTimeMillis() < toastUntil) {
                String text = fit(toast, PW - 24);
                g.drawString(this.font, text, px + (PW - this.font.width(text)) / 2, py + PH - 9, Ui.GOLD);
            }
        } catch (Exception e) {
            logOnce(e);
        }
    }

    private String fit(String text, int width) {
        if (this.font.width(text) <= width) {
            return text;
        }
        return this.font.plainSubstrByWidth(text, Math.max(0, width - 8)) + "…";
    }

    private static boolean hovered(Button b, int mx, int my) {
        return b.active && b.isMouseOver(mx, my);
    }

    private void drawFancy(GuiGraphics g, Fancy f, int mx, int my) {
        Button b = f.button();
        boolean hover = hovered(b, mx, my);
        f.hover()[0] += ((hover ? 1f : 0f) - f.hover()[0]) * 0.25f;
        float h = f.hover()[0];

        int base;
        int border;
        if (!b.active) {
            base = 0xFF22101B;
            border = 0xFF4A2A3A;
        } else {
            switch (f.kind()) {
                case PRIMARY -> {
                    base = Ui.mix(Ui.PINK_DARK, Ui.PINK, h * 0.55f);
                    border = Ui.PINK;
                }
                case DANGER -> {
                    base = Ui.mix(0xFF6B1B2E, 0xFF9A2A45, h);
                    border = Ui.RED;
                }
                default -> {
                    base = Ui.mix(Ui.mix(Ui.CARD, 0xFF000000, 0.25f), Ui.mix(Ui.CARD, Ui.PINK, 0.25f), h);
                    border = Ui.mix(Ui.CARD_LINE, Ui.PINK, h * 0.7f);
                }
            }
        }
        int textColor = !b.active ? 0xFF8A6B7A : (Ui.luminance(base) > 150 ? 0xFF101010 : Ui.WHITE);

        Ui.card(g, b.getX(), b.getY(), b.getWidth(), b.getHeight(), base, border);
        // Glanz beim Darüberfahren
        if (hover) {
            double t = System.currentTimeMillis() / 1000.0;
            int sx = b.getX() + (int) (((t * 1.6) % 1.0) * (b.getWidth() + 12)) - 6;
            int lo = Math.max(b.getX() + 1, sx);
            int hi = Math.min(b.getX() + b.getWidth() - 1, sx + 7);
            if (hi > lo) {
                g.fill(lo, b.getY() + 1, hi, b.getY() + b.getHeight() - 1, 0x30FFFFFF);
            }
        }
        String label = fit(f.label().get(), b.getWidth() - 6);
        int tx = b.getX() + (b.getWidth() - this.font.width(label)) / 2;
        g.drawString(this.font, label, tx, b.getY() + (b.getHeight() - 8) / 2 + 1, textColor);
    }

    private void drawDiscord(GuiGraphics g, Fancy f, int mx, int my) {
        Button b = f.button();
        int x = b.getX();
        int y = b.getY();
        int w = b.getWidth();
        int h = b.getHeight();
        boolean hover = hovered(b, mx, my);
        f.hover()[0] += ((hover ? 1f : 0f) - f.hover()[0]) * 0.25f;
        float hv = f.hover()[0];
        double t = System.currentTimeMillis() / 1000.0;

        // sanftes Leuchten
        Ui.round(g, x - 2, y - 2, w + 4, h + 4, Ui.alpha(0xFF5865F2, (float) (0.22 + 0.16 * Math.sin(t * 3)) + hv * 0.2f));
        int fill = Ui.mix(Ui.mix(0xFF4752C4, 0xFF5865F2, (float) (0.5 + 0.5 * Math.sin(t * 2))), 0xFF7B86FF, hv);
        Ui.card(g, x, y, w, h, fill, 0xFFB3BAFF);
        g.fill(x + 2, y + 1, x + w - 2, y + 2, 0x55FFFFFF);

        // kleines Sprechblasen-Symbol
        int ix = x + 7;
        int iy = y + 4;
        Ui.round(g, ix, iy, 14, 10, Ui.WHITE);
        g.fill(ix + 3, iy + 10, ix + 7, iy + 12, Ui.WHITE);
        g.fill(ix + 4, iy + 3, ix + 6, iy + 6, fill);
        g.fill(ix + 8, iy + 3, ix + 10, iy + 6, fill);

        g.drawString(this.font, "Join my Discord", x + 27, y + (h - 8) / 2 + 1, Ui.WHITE);

        // Glanz, der alle 3 Sekunden über den Button wandert
        int sx = x + (int) (((t % 3.0) / 3.0) * (w + 24)) - 12;
        int lo = Math.max(x + 1, sx);
        int hi = Math.min(x + w - 1, sx + 9);
        if (hi > lo) {
            g.fill(lo, y + 1, hi, y + h - 1, 0x40FFFFFF);
        }
    }

    private void drawCell(GuiGraphics g, Cell c, int mx, int my) {
        boolean hover = hovered(c.button(), mx, my);
        boolean sel = c.selected().getAsBoolean();
        int border = sel ? Ui.PINK : (hover ? Ui.mix(Ui.CARD_LINE, Ui.PINK, 0.6f) : Ui.CARD_LINE);
        int fill = sel ? Ui.mix(Ui.CARD, Ui.PINK, 0.25f) : Ui.mix(Ui.CARD, 0xFF000000, 0.3f);
        Ui.card(g, c.x(), c.y(), c.size(), c.size(), fill, border);
        ItemStack stack = c.stack().get();
        if (stack != null && !stack.isEmpty()) {
            g.renderItem(stack, c.x() + (c.size() - 16) / 2, c.y() + (c.size() - 16) / 2);
        }
    }

    private void drawSwatch(GuiGraphics g, Swatch s, int mx, int my) {
        Ui.Theme t = Ui.THEMES[s.index()];
        boolean sel = s.index() == Ui.themeIndex();
        boolean hover = hovered(s.button(), mx, my);
        int accent = t.cycle() == 0 ? t.accent() : Ui.PINK;
        Ui.card(g, s.x(), s.y(), 28, 22, t.card(), sel ? Ui.WHITE : (hover ? Ui.MUTED : t.line()));
        g.fill(s.x() + 4, s.y() + 4, s.x() + 24, s.y() + 18, accent);
        g.fill(s.x() + 4, s.y() + 12, s.x() + 24, s.y() + 18, t.accentDark());
    }

    private void drawHeader(GuiGraphics g) {
        double t = System.currentTimeMillis() / 1000.0;
        int bob = (int) Math.round(Math.sin(t * 2.2));
        BarakHud.drawLogo(g, px + 10, py + 8 + bob, 2);

        boolean auctionLive = tab == 1 && AuctionState.phase != AuctionState.Phase.IDLE;
        boolean giveawayLive = tab == 2 && GiveawayState.phase != GiveawayState.Phase.IDLE;
        if (auctionLive) {
            g.drawString(this.font, "EINNAHMEN", px + 32, py + 8, Ui.MUTED);
            g.drawString(this.font, AuctionState.money(AuctionState.income), px + 32, py + 19, Ui.GOLD);
        } else if (giveawayLive) {
            g.drawString(this.font, "TEILNEHMER", px + 32, py + 8, Ui.MUTED);
            g.drawString(this.font, String.valueOf(GiveawayState.participants().size()), px + 32, py + 19, Ui.GOLD);
        } else {
            String title = "BARAK PVP TOOLS";
            int chars = (int) ((System.currentTimeMillis() - openMillis) / 45L);
            String shown = title.substring(0, Math.max(0, Math.min(title.length(), chars)));
            g.drawString(this.font, shown, px + 32, py + 8, Ui.PINK);
            g.drawString(this.font, "Version 1.0.0", px + 32, py + 19, Ui.MUTED);
        }

        String center;
        if (tab == 0) {
            center = "Item-Größe";
        } else if (tab == 1) {
            center = auctionLive ? AuctionState.seller + "'s Auktion" : "Neue Auktion";
        } else if (tab == 2) {
            center = giveawayLive ? me() + "'s Giveaway" : "Neues Giveaway";
        } else {
            center = "Einstellungen";
        }
        int cw = this.font.width(center) + 20;
        Ui.card(g, px + (PW - cw) / 2, py + 8, cw, 16, Ui.mix(Ui.CARD, 0xFF000000, 0.2f), Ui.CARD_LINE);
        g.drawString(this.font, center, px + (PW - cw) / 2 + 10, py + 12, Ui.WHITE);

        String made = "Made by Barak";
        int mw = this.font.width(made) + 16;
        Ui.card(g, px + PW - mw - 12, py + 8, mw, 16, Ui.mix(Ui.CARD, 0xFF000000, 0.2f), Ui.CARD_LINE);
        g.drawString(this.font, made, px + PW - mw - 4, py + 12, Ui.MUTED);
    }

    private void drawLiveRow(GuiGraphics g, boolean live, String instruction, long secondsLeft, float progress) {
        double t = System.currentTimeMillis() / 1000.0;
        Ui.card(g, px + 12, py + 58, 58, 18, Ui.mix(Ui.CARD, 0xFF000000, 0.2f), live ? Ui.PINK : Ui.CARD_LINE);
        int dot = live ? Ui.alpha(Ui.PINK, (float) (0.45 + 0.55 * Math.abs(Math.sin(t * 3.5)))) : 0xFF8A6B7A;
        int grow = live ? (int) Math.round(Math.abs(Math.sin(t * 3.5))) : 0;
        Ui.round(g, px + 20 - grow, py + 64 - grow, 6 + grow * 2, 6 + grow * 2, dot);
        g.drawString(this.font, live ? "LIVE" : "ENDE", px + 32, py + 63, live ? Ui.PINK : Ui.MUTED);
        g.drawString(this.font, fit(instruction, 250), px + 78, py + 63, Ui.WHITE);

        String time = AuctionState.formatTime(secondsLeft);
        if (time.length() < 5) {
            time = "0" + time;
        }
        boolean blink = live && secondsLeft <= 10 && ((long) (t * 2) % 2 == 0);
        int color = !live ? Ui.MUTED : (blink ? Ui.WHITE : Ui.PINK);
        Ui.bigTime(g, px + PW - 12 - Ui.bigTimeWidth(time), py + 56, time, color);

        int barW = PW - 24;
        g.fill(px + 12, py + 82, px + 12 + barW, py + 86, Ui.GHOST);
        int fillW = (int) (barW * progress);
        if (fillW > 0) {
            g.fill(px + 12, py + 82, px + 12 + fillW, py + 86, Ui.PINK);
            if (live && fillW > 24) {
                int sx = px + 12 + (int) ((t * 90) % fillW);
                int ex = Math.min(px + 12 + fillW, sx + 18);
                g.fill(sx, py + 82, ex, py + 86, 0xA0FFFFFF);
            }
        }
    }

    private void renderSize(GuiGraphics g) {
        g.drawString(this.font, "ITEM AUSWÄHLEN", px + 20, py + 62, Ui.MUTED);
        g.drawString(this.font, RESULTS.size() + " Treffer", px + 180, py + 62, Ui.MUTED);

        g.drawString(this.font, "Tipp: Item anklicken, dann Größe einstellen.", px + 20, py + 198, Ui.MUTED);

        int rx = px + 258;
        g.drawString(this.font, "AUSGEWÄHLT", rx, py + 62, Ui.MUTED);
        if (selectedItem == null) {
            g.drawString(this.font, "Wähle links ein Item.", rx, py + 82, Ui.WHITE);
            g.drawString(this.font, "Dann stellst du die", rx, py + 98, Ui.MUTED);
            g.drawString(this.font, "Größe für Drops ein.", rx, py + 108, Ui.MUTED);
        } else {
            ItemStack st = new ItemStack(selectedItem);
            g.renderItem(st, rx, py + 76);
            g.drawString(this.font, fit(st.getHoverName().getString(), 140), rx + 22, py + 76, Ui.WHITE);
            float scale = BarakConfig.scaleOf(selectedItem);
            g.drawString(this.font, String.format(Locale.ROOT, "Größe: %.2fx", scale), rx + 22, py + 88, Ui.GOLD);
            g.drawString(this.font, "Gilt für Items am Boden", rx, py + 104, Ui.MUTED);
        }
        g.drawString(this.font, "Gespeichert:", rx, py + 183, Ui.MUTED);
        if (BarakConfig.itemScales.isEmpty()) {
            g.drawString(this.font, "–", rx + 60, py + 183, Ui.MUTED);
        }
    }

    private void renderAuction(GuiGraphics g) {
        AuctionState.Phase phase = AuctionState.phase;
        if (phase == AuctionState.Phase.IDLE) {
            g.drawString(this.font, "ITEM AUS INVENTAR WÄHLEN", px + 20, py + 62, Ui.MUTED);
            ItemStack chosen = invStack(AuctionState.selectedSlot);
            String sel = chosen.isEmpty() ? "Noch nichts gewählt" : chosen.getCount() + "x " + chosen.getHoverName().getString();
            g.drawString(this.font, fit(sel, 216), px + 20, py + 196, chosen.isEmpty() ? Ui.MUTED : Ui.GOLD);

            g.drawString(this.font, "MINDESTGEBOT", px + 262, py + 74, Ui.MUTED);
            g.drawString(this.font, "DAUER (SEKUNDEN)", px + 262, py + 114, Ui.MUTED);
            g.drawString(this.font, "Käufer sollen schreiben:", px + 262, py + 158, Ui.MUTED);
            g.drawString(this.font, fit("/pay " + me() + " <Betrag>", 160), px + 262, py + 170, Ui.WHITE);
            if (!AuctionState.status.isEmpty()) {
                g.drawString(this.font, fit(AuctionState.status, 160), px + 262, py + 190, Ui.RED);
            }
            return;
        }

        boolean running = phase == AuctionState.Phase.RUNNING;
        drawLiveRow(g, running, "Mache /pay " + AuctionState.seller + " <Betrag>",
                running ? AuctionState.secondsLeft() : 0, AuctionState.progress());

        Ui.card(g, px + 20, py + 102, 26, 26, Ui.mix(Ui.CARD, 0xFF000000, 0.3f), Ui.PINK_DARK);
        g.renderItem(AuctionState.item, px + 25, py + 107);
        g.drawString(this.font, fit(AuctionState.item.getHoverName().getString(), 120), px + 54, py + 106, Ui.WHITE);
        g.drawString(this.font, "x" + AuctionState.item.getCount(), px + 54, py + 118, Ui.MUTED);
        g.drawString(this.font, "Mindestgebot: " + AuctionState.money(AuctionState.startBid), px + 20, py + 140, Ui.GOLD);

        List<Map.Entry<String, Long>> rank = AuctionState.ranking();

        if (!running && AuctionState.winner != null) {
            g.drawString(this.font, "GEWINNER", px + 20, py + 174, Ui.MUTED);
            g.drawString(this.font, fit(AuctionState.winner, 150), px + 20, py + 186, Ui.WHITE);
            g.drawString(this.font, AuctionState.money(AuctionState.winnerAmount), px + 20, py + 198, Ui.GOLD);
        } else if (rank.isEmpty()) {
            String none = "Noch keine Gebote";
            g.drawString(this.font, none, px + 12 + (168 - this.font.width(none)) / 2, py + 186, Ui.MUTED);
        } else {
            g.drawString(this.font, "HÖCHSTES GEBOT", px + 20, py + 174, Ui.MUTED);
            g.drawString(this.font, fit(rank.get(0).getKey(), 150), px + 20, py + 186, Ui.WHITE);
            g.drawString(this.font, AuctionState.money(rank.get(0).getValue()), px + 20, py + 198, Ui.GOLD);
        }

        g.drawString(this.font, "HÖCHSTGEBOTE", px + 196, py + 100, Ui.MUTED);
        String count = rank.size() + (rank.size() == 1 ? " Gebot" : " Gebote");
        g.drawString(this.font, count, px + 188 + 240 - 8 - this.font.width(count), py + 100, Ui.MUTED);
        if (rank.isEmpty()) {
            String[] lines = {"Noch keine Gebote", "Mache /pay " + AuctionState.seller + " <Betrag>", "Mindestgebot: " + AuctionState.money(AuctionState.startBid)};
            int[] colors = {Ui.WHITE, Ui.MUTED, Ui.GOLD};
            for (int i = 0; i < lines.length; i++) {
                String s = fit(lines[i], 220);
                g.drawString(this.font, s, px + 188 + (240 - this.font.width(s)) / 2, py + 138 + i * 12, colors[i]);
            }
        } else {
            for (int i = 0; i < Math.min(7, rank.size()); i++) {
                Map.Entry<String, Long> e = rank.get(i);
                int y = py + 116 + i * 13;
                g.drawString(this.font, (i + 1) + ". " + fit(e.getKey(), 120), px + 196, y, i == 0 ? Ui.GOLD : Ui.WHITE);
                String amount = AuctionState.money(e.getValue());
                g.drawString(this.font, amount, px + 188 + 240 - 8 - this.font.width(amount), y, i == 0 ? Ui.GOLD : Ui.MUTED);
            }
        }
    }

    private void renderGiveaway(GuiGraphics g) {
        GiveawayState.Phase phase = GiveawayState.phase;
        if (phase == GiveawayState.Phase.IDLE) {
            g.drawString(this.font, "PREIS", px + 20, py + 74, Ui.MUTED);
            g.drawString(this.font, "WORT ZUM MITMACHEN", px + 20, py + 116, Ui.MUTED);
            g.drawString(this.font, "DAUER (SEKUNDEN)", px + 224, py + 116, Ui.MUTED);
            g.drawString(this.font, "Wer das Wort in den Chat schreibt, nimmt teil.", px + 20, py + 164, Ui.MUTED);
            g.drawString(this.font, "Am Ende wird zufällig ein Gewinner gelost.", px + 20, py + 176, Ui.MUTED);
            if (!GiveawayState.status.isEmpty()) {
                g.drawString(this.font, fit(GiveawayState.status, 380), px + 20, py + 194, Ui.RED);
            }
            return;
        }

        boolean running = phase == GiveawayState.Phase.RUNNING;
        drawLiveRow(g, running, "Schreibe \"" + GiveawayState.keyword + "\" in den Chat",
                running ? GiveawayState.secondsLeft() : 0, GiveawayState.progress());

        List<String> list = GiveawayState.participants();

        g.drawString(this.font, "PREIS", px + 20, py + 102, Ui.MUTED);
        g.drawString(this.font, fit(GiveawayState.prize, 150), px + 20, py + 114, Ui.WHITE);
        g.drawString(this.font, "Wort: " + GiveawayState.keyword, px + 20, py + 134, Ui.GOLD);
        g.drawString(this.font, "Teilnehmer: " + list.size(), px + 20, py + 146, Ui.MUTED);

        if (!running) {
            g.drawString(this.font, "GEWINNER", px + 20, py + 174, Ui.MUTED);
            g.drawString(this.font, GiveawayState.winner == null ? "Niemand" : fit(GiveawayState.winner, 150), px + 20, py + 188, Ui.GOLD);
        } else {
            String s = list.isEmpty() ? "Noch niemand dabei" : "Zuletzt: " + list.get(list.size() - 1);
            g.drawString(this.font, fit(s, 150), px + 20, py + 186, Ui.MUTED);
        }

        g.drawString(this.font, "TEILNEHMER", px + 196, py + 100, Ui.MUTED);
        String count = list.size() + (list.size() == 1 ? " Person" : " Personen");
        g.drawString(this.font, count, px + 188 + 240 - 8 - this.font.width(count), py + 100, Ui.MUTED);
        if (list.isEmpty()) {
            String s = "Noch niemand dabei";
            g.drawString(this.font, s, px + 188 + (240 - this.font.width(s)) / 2, py + 150, Ui.MUTED);
        } else {
            for (int i = 0; i < Math.min(14, list.size()); i++) {
                int col = i / 7;
                int row = i % 7;
                g.drawString(this.font, fit(list.get(i), 105), px + 196 + col * 116, py + 116 + row * 13, Ui.WHITE);
            }
        }
    }

    private void renderSettings(GuiGraphics g) {
        g.drawString(this.font, "ANZEIGE", px + 20, py + 62 - 0, Ui.MUTED);
        g.drawString(this.font, "TELEPORT-COMMAND FÜR DEN GEWINNER ({name} = Spielername)", px + 20, py + 146, Ui.MUTED);
        g.drawString(this.font, "DESIGN", px + 284, py + 62, Ui.MUTED);
        g.drawString(this.font, Ui.THEMES[Ui.themeIndex()].name(), px + 284, py + 76, Ui.WHITE);
    }
}
