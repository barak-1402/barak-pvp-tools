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
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Hauptfenster: Home, Item-Größe, Auktion, Giveaway, Invsee, Optionen, Animationen. */
public class BarakScreen extends Screen {
    private static final int PW = 520;
    private static final int PH = 300;
    private static final long AD_COOLDOWN_MS = 10_000L;

    private static final int TAB_HOME = 0;
    private static final int TAB_SIZE = 1;
    private static final int TAB_AUCTION = 2;
    private static final int TAB_GIVEAWAY = 3;
    private static final int TAB_INVSEE = 4;
    private static final int TAB_OPTIONS = 5;
    private static final int TAB_ANIM = 6;
    private static final int TAB_COUNT = 7;

    private enum Kind { PRIMARY, OUTLINE, BLUE, DANGER }

    private record Fancy(Button button, Supplier<String> label, Kind kind, float[] hover, boolean chrome) {
    }

    private record Cell(Button button, int x, int y, int size, Supplier<ItemStack> stack, BooleanSupplier selected) {
    }

    private record Swatch(Button button, int x, int y, int index) {
    }

    private record BoxBg(EditBox box, int x, int y, int w) {
    }

    private record Tile(Button button, int x, int y, int w, int h, int target, ItemStack icon, String title, String desc, float[] hover) {
    }

    private static final Set<String> CURATED = Set.of(
            "minecraft:end_crystal", "minecraft:totem_of_undying", "minecraft:obsidian", "minecraft:respawn_anchor",
            "minecraft:glowstone", "minecraft:ender_pearl", "minecraft:golden_apple", "minecraft:enchanted_golden_apple",
            "minecraft:netherite_sword", "minecraft:netherite_axe", "minecraft:netherite_helmet", "minecraft:netherite_chestplate",
            "minecraft:netherite_leggings", "minecraft:netherite_boots", "minecraft:diamond_sword", "minecraft:elytra",
            "minecraft:experience_bottle", "minecraft:tnt", "minecraft:shield", "minecraft:bow", "minecraft:crossbow",
            "minecraft:mace", "minecraft:wind_charge", "minecraft:trident");

    private static int tab = TAB_HOME;
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
    private int l;
    private int cw;
    private int lw;
    private int rx;
    private int rw;
    private int ct;
    private int chh;
    private Button adButton;
    private final long openMillis = System.currentTimeMillis();
    private long tabMillis = System.currentTimeMillis();
    private final List<Fancy> fancies = new ArrayList<>();
    private final List<Cell> cells = new ArrayList<>();
    private final List<Swatch> swatches = new ArrayList<>();
    private final List<BoxBg> boxes = new ArrayList<>();
    private final List<Tile> tiles = new ArrayList<>();

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

    private static String t(String de) {
        return Lang.t(de);
    }

    // ================================================================ Pop-out-Animation

    private double panelP() {
        return Ui.fx(BarakConfig.animPopout) ? (System.currentTimeMillis() - openMillis) / (320.0 / Ui.speed()) : 1.0;
    }

    private double tabP() {
        return Ui.fx(BarakConfig.animPopout) ? (System.currentTimeMillis() - tabMillis) / (260.0 / Ui.speed()) : 1.0;
    }

    private boolean contentVisible() {
        return panelP() >= 0.7 && tabP() >= 0.55;
    }

    private double cardScale() {
        double p = tabP();
        return p >= 1.0 ? 1.0 : 0.9 + 0.1 * Ui.easeOutBack(p);
    }

    /** Karte, die beim Tabwechsel aus der Mitte "herausploppt". */
    private void popCard(GuiGraphics g, int x, int y, int w, int h, int fill, int border) {
        double s = cardScale();
        if (s >= 0.999) {
            Ui.card(g, x, y, w, h, fill, border);
            return;
        }
        int nw = Math.max(4, (int) (w * s));
        int nh = Math.max(4, (int) (h * s));
        Ui.card(g, x + (w - nw) / 2, y + (h - nh) / 2, nw, nh, fill, border);
    }

    // ================================================================ Aufbau

    private int by() {
        return py + PH - 32;
    }

    private Button hit(int x, int y, int w, int h, Runnable action) {
        Button b = Button.builder(Component.empty(), btn -> action.run()).bounds(x, y, w, h).build();
        this.addWidget(b);
        return b;
    }

    private Button fancy(Supplier<String> label, int x, int y, int w, int h, Kind kind, boolean chrome, Runnable action) {
        Button b = hit(x, y, w, h, action);
        this.fancies.add(new Fancy(b, label, kind, new float[]{0f}, chrome));
        return b;
    }

    private Button fancy(Supplier<String> label, int x, int y, int w, int h, Kind kind, Runnable action) {
        return fancy(label, x, y, w, h, kind, false, action);
    }

    private Button bottomButton(String deLabel, int w, Kind kind, Runnable action) {
        Button b = fancy(() -> t(deLabel), bx, by(), w, 20, kind, action);
        bx += w + 6;
        return b;
    }

    private void bottomStart() {
        bx = px + 12;
        fancy(() -> "Join my Discord", bx, by(), 130, 20, Kind.BLUE, true, this::openDiscord);
        bx += 136;
    }

    private void toggle(String deLabel, int x, int y, int w, BooleanSupplier get, Consumer<Boolean> set) {
        fancy(() -> t(deLabel) + ": " + (get.getAsBoolean() ? t("AN") : t("AUS")), x, y, w, 20, Kind.OUTLINE, () -> {
            set.accept(!get.getAsBoolean());
            BarakConfig.save();
        });
    }

    private void cell(int x, int y, int size, Supplier<ItemStack> stack, BooleanSupplier selected, Runnable action) {
        Button b = hit(x, y, size, size, action);
        this.cells.add(new Cell(b, x, y, size, stack, selected));
    }

    private EditBox box(int x, int y, int w, String deHint, String value, Consumer<String> responder, String allowed) {
        EditBox b = new EditBox(this.font, x + 5, y, w - 10, 18, Component.literal(t(deHint)));
        b.setBordered(false);
        b.setMaxLength(60);
        if (allowed != null) {
            b.setFilter(s -> s.matches(allowed));
        }
        b.setHint(Component.literal(t(deHint)));
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
        l = px + 12;
        cw = PW - 24;
        lw = 250;
        rx = l + lw + 8;
        rw = cw - lw - 8;
        ct = py + 58;
        chh = PH - 98;
        fancies.clear();
        cells.clear();
        swatches.clear();
        boxes.clear();
        tiles.clear();
        adButton = null;

        String[] names = {"Home", "Größe", "Auktion", "Giveaway", "Invsee", "Optionen", "Effekte"};
        int tabW = (cw - 6 * 4) / TAB_COUNT;
        for (int i = 0; i < TAB_COUNT; i++) {
            final int index = i;
            fancy(() -> t(names[index]), l + i * (tabW + 4), py + 34, tabW, 18, i == tab ? Kind.PRIMARY : Kind.OUTLINE, true, () -> switchTab(index));
        }

        switch (tab) {
            case TAB_HOME -> initHome();
            case TAB_SIZE -> initSize();
            case TAB_AUCTION -> initAuction();
            case TAB_GIVEAWAY -> initGiveaway();
            case TAB_INVSEE -> initInvsee();
            case TAB_OPTIONS -> initOptions();
            default -> initAnim();
        }
    }

    private void switchTab(int index) {
        tab = index;
        tabMillis = System.currentTimeMillis();
        this.rebuildWidgets();
    }

    private void toast(String text) {
        toast = text;
        toastUntil = System.currentTimeMillis() + 3500L;
    }

    private void openDiscord() {
        try {
            new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", BarakPvPTools.DISCORD_URL).start();
            toast(t("Discord wird im Browser geöffnet …"));
        } catch (Throwable ex) {
            Minecraft.getInstance().keyboardHandler.setClipboard(BarakPvPTools.DISCORD_URL);
            toast(t("Browser nicht verfügbar – Link kopiert: ") + BarakPvPTools.DISCORD_URL);
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

    // ---------------------------------------------------------------- Tab: Home

    private void initHome() {
        String[][] info = {
                {"Item-Größe", "Drops größer oder kleiner|machen. Mit Leuchten."},
                {"Auktion", "Items versteigern mit|Live-Anzeige und Timer."},
                {"Giveaway", "Items verlosen per|Chat-Wort."},
                {"Invsee", "Zeigt Totems, Crystals und|mehr neben dem Inventar."},
                {"Optionen", "Sprache, Design und|Anzeige einstellen."},
                {"Effekte", "Animationen ein- und|ausschalten."}
        };
        int[] targets = {TAB_SIZE, TAB_AUCTION, TAB_GIVEAWAY, TAB_INVSEE, TAB_OPTIONS, TAB_ANIM};
        ItemStack[] icons = {
                new ItemStack(Items.END_CRYSTAL), new ItemStack(Items.EMERALD), new ItemStack(Items.NAME_TAG),
                new ItemStack(Items.TOTEM_OF_UNDYING), new ItemStack(Items.COMPARATOR), new ItemStack(Items.FIREWORK_ROCKET)
        };
        int tw = (cw - 20) / 3;
        int th = (chh - 10) / 2;
        for (int i = 0; i < 6; i++) {
            final int target = targets[i];
            int x = l + (i % 3) * (tw + 10);
            int y = ct + (i / 3) * (th + 10);
            Button b = hit(x, y, tw, th, () -> switchTab(target));
            tiles.add(new Tile(b, x, y, tw, th, target, icons[i], info[i][0], info[i][1], new float[]{0f}));
        }
        bottomStart();
        bottomButton("Schließen", 64, Kind.OUTLINE, this::onClose);
    }

    // ---------------------------------------------------------------- Item-Auswahl (Größe + Invsee)

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

    private static List<Item> itemsOf(List<String> keys) {
        List<Item> list = new ArrayList<>();
        if (keys.isEmpty()) {
            return list;
        }
        java.util.Map<String, Item> map = new java.util.HashMap<>();
        for (Item item : BuiltInRegistries.ITEM) {
            String k = BarakConfig.keyOf(item);
            if (keys.contains(k)) {
                map.put(k, item);
            }
        }
        for (String k : keys) {
            Item it = map.get(k);
            if (it != null) {
                list.add(it);
            }
        }
        return list;
    }

    /** Suchfeld + Ergebnis-Raster. onPick wird beim Anklicken eines Items aufgerufen. */
    private void initPicker(Consumer<Item> onPick, java.util.function.Predicate<Item> isSelected) {
        if (RESULTS.isEmpty()) {
            refreshResults();
        }
        box(l + 8, ct + 20, lw - 16, "Item suchen (z.B. crystal)", itemQuery, s -> {
            itemQuery = s;
            refreshResults();
        }, null);
        for (int i = 0; i < 24; i++) {
            final int index = i;
            int cx = l + 8 + (i % 8) * 30;
            int cy = ct + 46 + (i / 8) * 30;
            cell(cx, cy, 28,
                    () -> index < RESULT_STACKS.size() ? RESULT_STACKS.get(index) : ItemStack.EMPTY,
                    () -> index < RESULTS.size() && isSelected.test(RESULTS.get(index)),
                    () -> {
                        if (index < RESULTS.size()) {
                            onPick.accept(RESULTS.get(index));
                        }
                    });
        }
    }

    // ---------------------------------------------------------------- Tab: Item-Größe

    private void initSize() {
        initPicker(item -> selectedItem = item, item -> item == selectedItem);

        int x0 = rx + 8;
        sizeButton("-0.5", -0.5f, x0, ct + 74);
        sizeButton("-0.25", -0.25f, x0 + 56, ct + 74);
        sizeButton("+0.25", 0.25f, x0 + 112, ct + 74);
        sizeButton("+0.5", 0.5f, x0 + 168, ct + 74);
        presetButton("1x", 1.0f, x0, ct + 98);
        presetButton("2x", 2.0f, x0 + 56, ct + 98);
        presetButton("3x", 3.0f, x0 + 112, ct + 98);
        fancy(() -> t("Weg"), x0 + 168, ct + 98, 52, 20, Kind.DANGER, () -> {
            if (selectedItem != null) {
                BarakConfig.removeScale(selectedItem);
            }
        });
        toggle("Leuchten", x0, ct + 128, 220, () -> BarakConfig.glowDropped, v -> BarakConfig.glowDropped = v);

        List<Item> saved = itemsOf(new ArrayList<>(BarakConfig.itemScales.keySet()));
        for (int i = 0; i < Math.min(8, saved.size()); i++) {
            final Item item = saved.get(i);
            final ItemStack stack = new ItemStack(item);
            cell(x0 + i * 28, ct + 170, 24, () -> stack, () -> item == selectedItem, () -> selectedItem = item);
        }

        bottomStart();
        bottomButton("Schließen", 64, Kind.OUTLINE, this::onClose);
    }

    private void sizeButton(String label, float delta, int x, int y) {
        fancy(() -> label, x, y, 52, 20, Kind.OUTLINE, () -> {
            if (selectedItem != null) {
                BarakConfig.setScale(selectedItem, BarakConfig.scaleOf(selectedItem) + delta);
            }
        });
    }

    private void presetButton(String label, float value, int x, int y) {
        fancy(() -> label, x, y, 52, 20, Kind.OUTLINE, () -> {
            if (selectedItem != null) {
                BarakConfig.setScale(selectedItem, value);
            }
        });
    }

    // ---------------------------------------------------------------- Tab: Invsee

    private void initInvsee() {
        initPicker(item -> {
            BarakConfig.toggleInvseeItem(item);
            this.rebuildWidgets();
        }, BarakConfig::isInvseeItem);

        int x0 = rx + 8;
        List<Item> chosen = itemsOf(BarakConfig.invseeItems);
        for (int i = 0; i < chosen.size(); i++) {
            final Item item = chosen.get(i);
            final ItemStack stack = new ItemStack(item);
            cell(x0 + (i % 6) * 36, ct + 24 + (i / 6) * 36, 32, () -> stack, () -> false, () -> {
                BarakConfig.toggleInvseeItem(item);
                this.rebuildWidgets();
            });
        }
        toggle("Invsee-Anzeige", x0, ct + 104, 220, () -> BarakConfig.invseeEnabled, v -> BarakConfig.invseeEnabled = v);
        toggle("Bei jeder Kiste", x0, ct + 130, 220, () -> BarakConfig.invseeAlways, v -> BarakConfig.invseeAlways = v);

        bottomStart();
        bottomButton("Schließen", 64, Kind.OUTLINE, this::onClose);
    }

    // ---------------------------------------------------------------- Tab: Auktion

    private void initAuction() {
        AuctionState.Phase phase = AuctionState.phase;
        if (phase == AuctionState.Phase.IDLE) {
            for (int row = 0; row < 4; row++) {
                for (int col = 0; col < 9; col++) {
                    final int slot = row < 3 ? 9 + row * 9 + col : col;
                    cell(l + 8 + col * 26, ct + 26 + row * 26 + (row == 3 ? 6 : 0), 24,
                            () -> invStack(slot), () -> AuctionState.selectedSlot == slot, () -> AuctionState.selectedSlot = slot);
                }
            }
            box(rx + 8, ct + 22, rw - 16, "Mindestgebot (z.B. 2m)", AuctionState.draftBid, s -> AuctionState.draftBid = s, "[0-9.,kKmMbB$]*");
            box(rx + 8, ct + 66, rw - 16, "Dauer in Sekunden", AuctionState.draftSeconds, s -> AuctionState.draftSeconds = s, "[0-9]*");

            bottomStart();
            bottomButton("Auktion starten", 120, Kind.PRIMARY, this::startAuction);
            bottomButton("Schließen", 64, Kind.OUTLINE, this::onClose);
        } else if (phase == AuctionState.Phase.RUNNING) {
            bottomStart();
            bottomButton("Beenden", 60, Kind.DANGER, () -> {
                AuctionState.endNow();
                this.rebuildWidgets();
            });
            bottomButton("Zurücksetzen", 90, Kind.OUTLINE, () -> {
                AuctionState.reset();
                AuctionState.status = "";
                this.rebuildWidgets();
            });
            adButton = bottomButton("Werbung", 90, Kind.PRIMARY, this::advertiseAuction);
            bottomButton("Schließen", 64, Kind.OUTLINE, this::onClose);
        } else {
            bottomStart();
            if (AuctionState.winner != null) {
                bottomButton("TP-Command kopieren", 140, Kind.PRIMARY, () -> {
                    Minecraft.getInstance().keyboardHandler.setClipboard(AuctionState.teleportCommand());
                    toast(t("Kopiert: ") + AuctionState.teleportCommand());
                });
            }
            bottomButton("Zurücksetzen", 90, Kind.OUTLINE, () -> {
                AuctionState.reset();
                AuctionState.status = "";
                this.rebuildWidgets();
            });
            bottomButton("Schließen", 64, Kind.OUTLINE, this::onClose);
        }
    }

    private void startAuction() {
        ItemStack chosen = invStack(AuctionState.selectedSlot);
        if (chosen.isEmpty()) {
            AuctionState.status = t("Wähle zuerst ein Item aus deinem Inventar.");
            return;
        }
        long bid = AuctionState.parseMoney(AuctionState.draftBid);
        if (bid < 0) {
            AuctionState.status = t("Ungültiges Mindestgebot.");
            return;
        }
        int seconds;
        try {
            seconds = Integer.parseInt(AuctionState.draftSeconds.trim());
        } catch (NumberFormatException ex) {
            AuctionState.status = t("Ungültige Dauer.");
            return;
        }
        if (seconds < 10 || seconds > 3600) {
            AuctionState.status = t("Dauer: 10 bis 3600 Sekunden.");
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
        say(t("Auktion") + ": " + AuctionState.itemName() + " | " + t("Mindestgebot") + " " + AuctionState.money(AuctionState.startBid)
                + " | " + t("Mache /pay") + " " + me() + " <" + t("Betrag") + "> | " + t("noch") + " " + AuctionState.formatTime(AuctionState.secondsLeft()));
    }

    // ---------------------------------------------------------------- Tab: Giveaway

    private void initGiveaway() {
        GiveawayState.Phase phase = GiveawayState.phase;
        if (phase == GiveawayState.Phase.IDLE) {
            box(l + 8, ct + 24, cw - 16, "Preis (z.B. 3x End Crystal)", GiveawayState.draftPrize, s -> GiveawayState.draftPrize = s, null);
            box(l + 8, ct + 74, (cw - 24) / 2, "Wort zum Mitmachen", GiveawayState.draftKeyword, s -> GiveawayState.draftKeyword = s, null);
            box(l + 16 + (cw - 24) / 2, ct + 74, (cw - 24) / 2, "Dauer in Sekunden", GiveawayState.draftSeconds, s -> GiveawayState.draftSeconds = s, "[0-9]*");

            bottomStart();
            bottomButton("Giveaway starten", 130, Kind.PRIMARY, this::startGiveaway);
            bottomButton("Schließen", 64, Kind.OUTLINE, this::onClose);
        } else if (phase == GiveawayState.Phase.RUNNING) {
            bottomStart();
            bottomButton("Auslosen", 64, Kind.DANGER, () -> {
                GiveawayState.draw();
                this.rebuildWidgets();
            });
            bottomButton("Zurücksetzen", 90, Kind.OUTLINE, () -> {
                GiveawayState.reset();
                GiveawayState.status = "";
                this.rebuildWidgets();
            });
            adButton = bottomButton("Werbung", 90, Kind.PRIMARY, this::advertiseGiveaway);
            bottomButton("Schließen", 64, Kind.OUTLINE, this::onClose);
        } else {
            bottomStart();
            if (GiveawayState.winner != null) {
                bottomButton("Gewinner verkünden", 140, Kind.PRIMARY, () ->
                        say(t("Giveaway-Gewinner") + ": " + GiveawayState.winner + " " + t("gewinnt") + " " + GiveawayState.prize + "! " + t("Glückwunsch!")));
            }
            bottomButton("Zurücksetzen", 90, Kind.OUTLINE, () -> {
                GiveawayState.reset();
                GiveawayState.status = "";
                this.rebuildWidgets();
            });
            bottomButton("Schließen", 64, Kind.OUTLINE, this::onClose);
        }
    }

    private void startGiveaway() {
        String prize = GiveawayState.draftPrize.trim();
        String word = GiveawayState.draftKeyword.trim();
        if (prize.isEmpty()) {
            GiveawayState.status = t("Gib einen Preis ein.");
            return;
        }
        if (word.isEmpty()) {
            GiveawayState.status = t("Gib ein Wort zum Mitmachen ein.");
            return;
        }
        int seconds;
        try {
            seconds = Integer.parseInt(GiveawayState.draftSeconds.trim());
        } catch (NumberFormatException ex) {
            GiveawayState.status = t("Ungültige Dauer.");
            return;
        }
        if (seconds < 10 || seconds > 3600) {
            GiveawayState.status = t("Dauer: 10 bis 3600 Sekunden.");
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
        say("Giveaway: " + GiveawayState.prize + " | " + t("Schreibe") + " \"" + GiveawayState.keyword + "\" " + t("in den Chat!") + " | "
                + t("noch") + " " + AuctionState.formatTime(GiveawayState.secondsLeft()));
    }

    // ---------------------------------------------------------------- Tab: Optionen

    private void initOptions() {
        int x0 = l + 8;
        fancy(() -> t("Sprache") + ": " + ("en".equals(BarakConfig.language) ? "English" : "Deutsch"), x0, ct + 8, 236, 20, Kind.PRIMARY, () -> {
            BarakConfig.language = "en".equals(BarakConfig.language) ? "de" : "en";
            BarakConfig.save();
            this.rebuildWidgets();
        });
        toggle("Logo oben links", x0, ct + 34, 236, () -> BarakConfig.showLogo, v -> BarakConfig.showLogo = v);
        toggle("Auktion/Giveaway-Anzeige", x0, ct + 60, 236, () -> BarakConfig.showAuctionHud, v -> BarakConfig.showAuctionHud = v);
        toggle("Discord-Hinweis beim Beitreten", x0, ct + 86, 236, () -> BarakConfig.showJoinMessage, v -> BarakConfig.showJoinMessage = v);
        box(x0, ct + 134, 236, "Teleport-Command", BarakConfig.tpCommand, s -> BarakConfig.tpCommand = s, null);

        for (int i = 0; i < Ui.THEMES.length; i++) {
            final int index = i;
            int sx = l + 264 + (i % 4) * 40;
            int sy = ct + 40 + (i / 4) * 32;
            Button b = hit(sx, sy, 34, 26, () -> {
                BarakConfig.themeIndex = index;
                BarakConfig.save();
            });
            swatches.add(new Swatch(b, sx, sy, i));
        }

        bottomStart();
        bottomButton("Schließen", 64, Kind.OUTLINE, this::onClose);
    }

    // ---------------------------------------------------------------- Tab: Effekte (Animationen)

    private void initAnim() {
        int x0 = l + 8;
        int x1 = l + 8 + 244;
        toggle("Animationen (alle)", x0, ct + 8, 236, () -> BarakConfig.animEnabled, v -> BarakConfig.animEnabled = v);
        toggle("Pop-out beim Öffnen", x0, ct + 34, 236, () -> BarakConfig.animPopout, v -> BarakConfig.animPopout = v);
        toggle("Partikel", x0, ct + 60, 236, () -> BarakConfig.animParticles, v -> BarakConfig.animParticles = v);
        toggle("Wandernder Rand-Glanz", x0, ct + 86, 236, () -> BarakConfig.animBorder, v -> BarakConfig.animBorder = v);
        toggle("Glow-Puls", x1, ct + 8, 236, () -> BarakConfig.animPulse, v -> BarakConfig.animPulse = v);
        toggle("Glanz-Effekt", x1, ct + 34, 236, () -> BarakConfig.animShimmer, v -> BarakConfig.animShimmer = v);
        toggle("Titel-Effekte", x1, ct + 60, 236, () -> BarakConfig.animTitle, v -> BarakConfig.animTitle = v);
        toggle("Hover-Übergang", x1, ct + 86, 236, () -> BarakConfig.animHover, v -> BarakConfig.animHover = v);
        fancy(() -> t("Geschwindigkeit") + ": " + speedName(), x0, ct + 118, 236, 20, Kind.PRIMARY, () -> {
            BarakConfig.animSpeed = (BarakConfig.animSpeed + 1) % 3;
            BarakConfig.save();
        });
        bottomStart();
        bottomButton("Schließen", 64, Kind.OUTLINE, this::onClose);
    }

    private static String speedName() {
        return switch (BarakConfig.animSpeed) {
            case 0 -> t("Langsam");
            case 2 -> t("Schnell");
            default -> t("Normal");
        };
    }

    // ================================================================ Zeichnen

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        try {
            Ui.apply();
            double t = Ui.time();
            float pulse = Ui.fx(BarakConfig.animPulse) ? (float) (0.6 + 0.4 * Math.sin(t * 2.5)) : 1.0f;

            // Fenster-Pop-out: wächst aus der Mitte (mit leichtem Überschwingen)
            double pp = panelP();
            double ps = pp >= 1.0 ? 1.0 : 0.8 + 0.2 * Ui.easeOutBack(pp);
            int fw = (int) (PW * ps);
            int fh = (int) (PH * ps);
            Ui.card(g, px + (PW - fw) / 2, py + (PH - fh) / 2, fw, fh, Ui.BG, Ui.alpha(Ui.PINK, pulse));
            if (pp < 0.7) {
                return;
            }

            if (Ui.fx(BarakConfig.animParticles)) {
                for (int i = 0; i < 18; i++) {
                    int sx = px + 10 + (i * 53 + 17) % (PW - 20);
                    int sy = py + PH - 6 - (int) ((t * (8 + i % 5) + i * 37) % (PH - 12));
                    float tw = (float) (0.25 + 0.2 * Math.sin(t * 2 + i));
                    g.fill(sx, sy, sx + 1 + (i % 2), sy + 1 + (i % 2), Ui.alpha(Ui.PINK, tw));
                }
            }
            if (Ui.fx(BarakConfig.animBorder)) {
                Ui.borderGlow(g, px, py, PW, PH, Ui.PINK, t);
            }

            drawCards(g);

            if (contentVisible()) {
                for (BoxBg b : boxes) {
                    int border = b.box().isFocused() ? Ui.PINK : Ui.CARD_LINE;
                    Ui.card(g, b.x(), b.y(), b.w(), 18, Ui.mix(Ui.CARD, 0xFF000000, 0.35f), border);
                }
            }
        } catch (Exception ex) {
            logOnce(ex);
        }
    }

    private boolean liveView() {
        return (tab == TAB_AUCTION && AuctionState.phase != AuctionState.Phase.IDLE)
                || (tab == TAB_GIVEAWAY && GiveawayState.phase != GiveawayState.Phase.IDLE);
    }

    private void drawCards(GuiGraphics g) {
        int card = Ui.CARD;
        int line = Ui.CARD_LINE;
        switch (tab) {
            case TAB_HOME -> {
                // Kacheln werden einzeln gezeichnet
            }
            case TAB_SIZE, TAB_INVSEE -> {
                popCard(g, l, ct, lw, chh, card, line);
                popCard(g, rx, ct, rw, chh, card, line);
            }
            case TAB_AUCTION, TAB_GIVEAWAY -> {
                if (liveView()) {
                    popCard(g, l, py + 94, 200, 84, card, line);
                    popCard(g, l, py + 182, 200, 78, card, line);
                    popCard(g, l + 208, py + 94, cw - 208, 166, card, line);
                } else if (tab == TAB_AUCTION) {
                    popCard(g, l, ct, lw, chh, card, line);
                    popCard(g, rx, ct, rw, chh, card, line);
                } else {
                    popCard(g, l, ct, cw, chh, card, line);
                }
            }
            default -> popCard(g, l, ct, cw, chh, card, line);
        }
    }

    private static void logOnce(Exception ex) {
        if (!errorLogged) {
            errorLogged = true;
            BarakPvPTools.LOGGER.warn("Fehler beim Zeichnen des Menüs", ex);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        boolean vis = contentVisible();
        for (BoxBg b : boxes) {
            b.box().visible = vis;
        }
        super.render(g, mouseX, mouseY, partialTick);
        if (panelP() < 0.7) {
            return;
        }
        try {
            if (adButton != null) {
                long until = tab == TAB_AUCTION ? auctionAdUntil : giveawayAdUntil;
                adButton.active = System.currentTimeMillis() >= until;
            }

            drawHeader(g);

            if (vis) {
                switch (tab) {
                    case TAB_HOME -> renderHome(g, mouseX, mouseY);
                    case TAB_SIZE -> renderSize(g, mouseX, mouseY);
                    case TAB_AUCTION -> renderAuction(g);
                    case TAB_GIVEAWAY -> renderGiveaway(g);
                    case TAB_INVSEE -> renderInvsee(g, mouseX, mouseY);
                    case TAB_OPTIONS -> renderOptions(g);
                    default -> renderAnim(g);
                }
                for (Cell c : cells) {
                    drawCell(g, c, mouseX, mouseY);
                }
                for (Swatch s : swatches) {
                    drawSwatch(g, s, mouseX, mouseY);
                }
            }
            for (Fancy f : fancies) {
                if (!f.chrome() && !vis) {
                    continue;
                }
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
        } catch (Exception ex) {
            logOnce(ex);
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

    private static void smooth(float[] hover, boolean on) {
        float target = on ? 1f : 0f;
        if (Ui.fx(BarakConfig.animHover)) {
            hover[0] += (target - hover[0]) * 0.25f;
        } else {
            hover[0] = target;
        }
    }

    private void shimmerRect(GuiGraphics g, int x, int y, int w, int h, double t, double speed) {
        if (!Ui.fx(BarakConfig.animShimmer)) {
            return;
        }
        int sx = x + (int) (((t * speed) % 1.0) * (w + 16)) - 8;
        int lo = Math.max(x + 1, sx);
        int hi = Math.min(x + w - 1, sx + 8);
        if (hi > lo) {
            g.fill(lo, y + 1, hi, y + h - 1, 0x38FFFFFF);
        }
    }

    private void drawFancy(GuiGraphics g, Fancy f, int mx, int my) {
        Button b = f.button();
        boolean hover = hovered(b, mx, my);
        smooth(f.hover(), hover);
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
        if (hover) {
            shimmerRect(g, b.getX(), b.getY(), b.getWidth(), b.getHeight(), Ui.time(), 1.6);
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
        smooth(f.hover(), hover);
        float hv = f.hover()[0];
        double t = Ui.time();
        boolean pulse = Ui.fx(BarakConfig.animPulse);

        Ui.round(g, x - 2, y - 2, w + 4, h + 4, Ui.alpha(0xFF5865F2, (pulse ? (float) (0.22 + 0.16 * Math.sin(t * 3)) : 0.25f) + hv * 0.2f));
        int fill = Ui.mix(Ui.mix(0xFF4752C4, 0xFF5865F2, pulse ? (float) (0.5 + 0.5 * Math.sin(t * 2)) : 0.6f), 0xFF7B86FF, hv);
        Ui.card(g, x, y, w, h, fill, 0xFFB3BAFF);
        g.fill(x + 2, y + 1, x + w - 2, y + 2, 0x55FFFFFF);

        int ix = x + 7;
        int iy = y + 4;
        Ui.round(g, ix, iy, 14, 10, Ui.WHITE);
        g.fill(ix + 3, iy + 10, ix + 7, iy + 12, Ui.WHITE);
        g.fill(ix + 4, iy + 3, ix + 6, iy + 6, fill);
        g.fill(ix + 8, iy + 3, ix + 10, iy + 6, fill);

        g.drawString(this.font, "Join my Discord", x + 27, y + (h - 8) / 2 + 1, Ui.WHITE);
        shimmerRect(g, x, y, w, h, t, 0.35);
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
        Ui.Theme th = Ui.THEMES[s.index()];
        boolean sel = s.index() == Ui.themeIndex();
        boolean hover = hovered(s.button(), mx, my);
        int accent = th.cycle() == 0 ? th.accent() : Ui.PINK;
        Ui.card(g, s.x(), s.y(), 34, 26, th.card(), sel ? Ui.WHITE : (hover ? Ui.MUTED : th.line()));
        g.fill(s.x() + 4, s.y() + 4, s.x() + 30, s.y() + 22, accent);
        g.fill(s.x() + 4, s.y() + 14, s.x() + 30, s.y() + 22, th.accentDark());
    }

    private void drawHeader(GuiGraphics g) {
        double t = Ui.time();
        boolean titleFx = Ui.fx(BarakConfig.animTitle);
        int bob = titleFx ? (int) Math.round(Math.sin(t * 2.2)) : 0;
        BarakHud.drawLogo(g, px + 10, py + 8 + bob, 2);

        boolean auctionLive = tab == TAB_AUCTION && AuctionState.phase != AuctionState.Phase.IDLE;
        boolean giveawayLive = tab == TAB_GIVEAWAY && GiveawayState.phase != GiveawayState.Phase.IDLE;
        if (auctionLive) {
            g.drawString(this.font, t("EINNAHMEN"), px + 32, py + 8, Ui.MUTED);
            g.drawString(this.font, AuctionState.money(AuctionState.income), px + 32, py + 19, Ui.GOLD);
        } else if (giveawayLive) {
            g.drawString(this.font, t("TEILNEHMER"), px + 32, py + 8, Ui.MUTED);
            g.drawString(this.font, String.valueOf(GiveawayState.participants().size()), px + 32, py + 19, Ui.GOLD);
        } else {
            String title = "BARAK PVP TOOLS";
            String shown = title;
            if (titleFx) {
                int chars = (int) ((System.currentTimeMillis() - openMillis) / 45L);
                shown = title.substring(0, Math.max(0, Math.min(title.length(), chars)));
            }
            g.drawString(this.font, shown, px + 32, py + 8, Ui.PINK);
            g.drawString(this.font, t("Version") + " 1.1.0", px + 32, py + 19, Ui.MUTED);
        }

        String center;
        switch (tab) {
            case TAB_HOME -> center = t("Hauptmenü");
            case TAB_SIZE -> center = t("Item-Größe");
            case TAB_AUCTION -> center = auctionLive ? AuctionState.seller + "'s " + t("Auktion") : t("Neue Auktion");
            case TAB_GIVEAWAY -> center = giveawayLive ? me() + "'s Giveaway" : t("Neues Giveaway");
            case TAB_INVSEE -> center = "Invsee";
            case TAB_OPTIONS -> center = t("Optionen");
            default -> center = t("Effekte");
        }
        int cwid = this.font.width(center) + 20;
        Ui.card(g, px + (PW - cwid) / 2, py + 8, cwid, 16, Ui.mix(Ui.CARD, 0xFF000000, 0.2f), Ui.CARD_LINE);
        g.drawString(this.font, center, px + (PW - cwid) / 2 + 10, py + 12, Ui.WHITE);

        String made = "Made by Barak";
        int mw = this.font.width(made) + 16;
        Ui.card(g, px + PW - mw - 12, py + 8, mw, 16, Ui.mix(Ui.CARD, 0xFF000000, 0.2f), Ui.CARD_LINE);
        g.drawString(this.font, made, px + PW - mw - 4, py + 12, Ui.MUTED);
    }

    private void drawProgress(GuiGraphics g, int x, int y, int w, int h, float progress, boolean live) {
        g.fill(x, y, x + w, y + h, Ui.GHOST);
        int fillW = (int) (w * progress);
        if (fillW > 0) {
            g.fill(x, y, x + fillW, y + h, Ui.PINK);
            if (live && fillW > 24 && Ui.fx(BarakConfig.animShimmer)) {
                int sx = x + (int) ((Ui.time() * 90) % fillW);
                int ex = Math.min(x + fillW, sx + 18);
                g.fill(sx, y, ex, y + h, 0xA0FFFFFF);
            }
        }
    }

    private void drawLiveRow(GuiGraphics g, boolean live, String instruction, long secondsLeft, float progress) {
        double t = Ui.time();
        boolean pulse = Ui.fx(BarakConfig.animPulse);
        Ui.card(g, l, py + 58, 58, 18, Ui.mix(Ui.CARD, 0xFF000000, 0.2f), live ? Ui.PINK : Ui.CARD_LINE);
        float wave = pulse ? (float) Math.abs(Math.sin(t * 3.5)) : 1f;
        int dot = live ? Ui.alpha(Ui.PINK, 0.45f + 0.55f * wave) : 0xFF8A6B7A;
        int grow = (live && pulse) ? (int) Math.round(wave) : 0;
        Ui.round(g, l + 8 - grow, py + 64 - grow, 6 + grow * 2, 6 + grow * 2, dot);
        g.drawString(this.font, live ? "LIVE" : t("ENDE"), l + 20, py + 63, live ? Ui.PINK : Ui.MUTED);
        g.drawString(this.font, fit(instruction, 300), l + 66, py + 63, Ui.WHITE);

        String time = AuctionState.formatTime(secondsLeft);
        if (time.length() < 5) {
            time = "0" + time;
        }
        boolean blink = live && secondsLeft <= 10 && ((long) (t * 2) % 2 == 0);
        int color = !live ? Ui.MUTED : (blink ? Ui.WHITE : Ui.PINK);
        Ui.bigTime(g, px + PW - 12 - Ui.bigTimeWidth(time), py + 56, time, color);

        drawProgress(g, l, py + 82, cw, 4, progress, live);
    }

    // ---------------------------------------------------------------- Rendern der Tabs

    private void renderHome(GuiGraphics g, int mx, int my) {
        for (Tile tile : tiles) {
            boolean hover = tile.button().isMouseOver(mx, my);
            smooth(tile.hover(), hover);
            float h = tile.hover()[0];
            int lift = Math.round(-3 * h);
            int x = tile.x();
            int y = tile.y() + lift;

            double s = cardScale();
            int w = tile.w();
            int hh = tile.h();
            if (s < 0.999) {
                int nw = Math.max(4, (int) (w * s));
                int nh = Math.max(4, (int) (hh * s));
                x += (w - nw) / 2;
                y += (hh - nh) / 2;
                w = nw;
                hh = nh;
            }
            int fill = Ui.mix(Ui.CARD, Ui.mix(Ui.CARD, Ui.PINK, 0.2f), h);
            int border = Ui.mix(Ui.CARD_LINE, Ui.PINK, h);
            Ui.card(g, x, y, w, hh, fill, border);
            if (s < 0.999) {
                continue;
            }
            g.fill(x + 6, y + 3, x + w - 6, y + 5, Ui.alpha(Ui.PINK, 0.5f + 0.5f * h));

            Ui.card(g, x + 10, y + 14, 30, 30, Ui.mix(Ui.CARD, 0xFF000000, 0.35f), Ui.PINK_DARK);
            g.renderItem(tile.icon(), x + 17, y + 21);
            g.drawString(this.font, fit(t(tile.title()), w - 60), x + 48, y + 18, Ui.PINK);
            g.drawString(this.font, t("Öffnen") + " >", x + 48, y + 30, Ui.MUTED);

            String[] lines = t(tile.desc()).split("\\|");
            for (int i = 0; i < lines.length && i < 3; i++) {
                g.drawString(this.font, fit(lines[i], w - 20), x + 10, y + 54 + i * 11, Ui.WHITE);
            }
            shimmerRect(g, x, y, w, hh, Ui.time() + tile.target() * 0.3, hover ? 1.2 : 0.0);
        }
    }

    private void renderPickerHeader(GuiGraphics g, int mx, int my) {
        g.drawString(this.font, t("ITEM AUSWÄHLEN"), l + 8, ct + 8, Ui.MUTED);
        g.drawString(this.font, RESULTS.size() + " " + t("Treffer"), l + lw - 8 - this.font.width(RESULTS.size() + " " + t("Treffer")), ct + 8, Ui.MUTED);
        boolean any = false;
        for (Cell c : cells) {
            if (c.button().isMouseOver(mx, my)) {
                ItemStack st = c.stack().get();
                if (st != null && !st.isEmpty()) {
                    g.drawString(this.font, fit(st.getHoverName().getString(), lw - 16), l + 8, ct + 146, Ui.WHITE);
                    any = true;
                }
                break;
            }
        }
        if (!any) {
            g.drawString(this.font, t("Tipp: Item anklicken."), l + 8, ct + 146, Ui.MUTED);
        }
    }

    private void renderSize(GuiGraphics g, int mx, int my) {
        renderPickerHeader(g, mx, my);

        int x0 = rx + 8;
        g.drawString(this.font, t("AUSGEWÄHLT"), x0, ct + 8, Ui.MUTED);
        if (selectedItem == null) {
            g.drawString(this.font, t("Wähle links ein Item."), x0, ct + 26, Ui.WHITE);
            g.drawString(this.font, t("Dann stellst du die"), x0, ct + 42, Ui.MUTED);
            g.drawString(this.font, t("Größe für Drops ein."), x0, ct + 52, Ui.MUTED);
        } else {
            ItemStack st = new ItemStack(selectedItem);
            g.renderItem(st, x0, ct + 24);
            g.drawString(this.font, fit(st.getHoverName().getString(), rw - 40), x0 + 22, ct + 24, Ui.WHITE);
            float scale = BarakConfig.scaleOf(selectedItem);
            g.drawString(this.font, t("Größe") + String.format(Locale.ROOT, ": %.2fx", scale), x0 + 22, ct + 36, Ui.GOLD);
            g.drawString(this.font, t("Gilt für Items am Boden"), x0, ct + 56, Ui.MUTED);
        }
        g.drawString(this.font, t("Gespeichert") + ":", x0, ct + 158, Ui.MUTED);
        if (BarakConfig.itemScales.isEmpty()) {
            g.drawString(this.font, "–", x0 + 70, ct + 158, Ui.MUTED);
        }
    }

    private void renderInvsee(GuiGraphics g, int mx, int my) {
        renderPickerHeader(g, mx, my);

        int x0 = rx + 8;
        g.drawString(this.font, t("ANZUZEIGEN") + " " + BarakConfig.invseeItems.size() + "/" + BarakConfig.MAX_INVSEE_ITEMS, x0, ct + 8, Ui.MUTED);
        g.drawString(this.font, t("Öffne /invsee <Name>: links neben dem"), x0, ct + 162, Ui.MUTED);
        g.drawString(this.font, t("Inventar steht, wie viele der gewählten"), x0, ct + 173, Ui.MUTED);
        g.drawString(this.font, t("Items der Spieler hat."), x0, ct + 184, Ui.MUTED);
    }

    private void renderAuction(GuiGraphics g) {
        AuctionState.Phase phase = AuctionState.phase;
        if (phase == AuctionState.Phase.IDLE) {
            g.drawString(this.font, t("ITEM AUS INVENTAR WÄHLEN"), l + 8, ct + 8, Ui.MUTED);
            ItemStack chosen = invStack(AuctionState.selectedSlot);
            String sel = chosen.isEmpty() ? t("Noch nichts gewählt") : chosen.getCount() + "x " + chosen.getHoverName().getString();
            g.drawString(this.font, fit(sel, lw - 16), l + 8, ct + 150, chosen.isEmpty() ? Ui.MUTED : Ui.GOLD);

            g.drawString(this.font, t("MINDESTGEBOT"), rx + 8, ct + 8, Ui.MUTED);
            g.drawString(this.font, t("DAUER (SEKUNDEN)"), rx + 8, ct + 52, Ui.MUTED);
            g.drawString(this.font, t("Käufer sollen schreiben:"), rx + 8, ct + 104, Ui.MUTED);
            g.drawString(this.font, fit("/pay " + me() + " <" + t("Betrag") + ">", rw - 16), rx + 8, ct + 116, Ui.WHITE);
            if (!AuctionState.status.isEmpty()) {
                g.drawString(this.font, fit(AuctionState.status, rw - 16), rx + 8, ct + 140, Ui.RED);
            }
            return;
        }

        boolean running = phase == AuctionState.Phase.RUNNING;
        drawLiveRow(g, running, t("Mache /pay") + " " + AuctionState.seller + " <" + t("Betrag") + ">",
                running ? AuctionState.secondsLeft() : 0, AuctionState.progress());

        Ui.card(g, l + 8, py + 102, 30, 30, Ui.mix(Ui.CARD, 0xFF000000, 0.3f), Ui.PINK_DARK);
        g.renderItem(AuctionState.item, l + 15, py + 109);
        g.drawString(this.font, fit(AuctionState.item.getHoverName().getString(), 140), l + 46, py + 106, Ui.WHITE);
        g.drawString(this.font, "x" + AuctionState.item.getCount(), l + 46, py + 118, Ui.MUTED);
        g.drawString(this.font, t("Mindestgebot") + ": " + AuctionState.money(AuctionState.startBid), l + 8, py + 150, Ui.GOLD);

        List<Map.Entry<String, Long>> rank = AuctionState.ranking();

        if (!running && AuctionState.winner != null) {
            g.drawString(this.font, t("GEWINNER"), l + 8, py + 192, Ui.MUTED);
            g.drawString(this.font, fit(AuctionState.winner, 180), l + 8, py + 205, Ui.WHITE);
            g.drawString(this.font, AuctionState.money(AuctionState.winnerAmount), l + 8, py + 218, Ui.GOLD);
        } else if (rank.isEmpty()) {
            String none = t("Noch keine Gebote");
            g.drawString(this.font, none, l + (200 - this.font.width(none)) / 2, py + 214, Ui.MUTED);
        } else {
            g.drawString(this.font, t("HÖCHSTES GEBOT"), l + 8, py + 192, Ui.MUTED);
            g.drawString(this.font, fit(rank.get(0).getKey(), 180), l + 8, py + 205, Ui.WHITE);
            g.drawString(this.font, AuctionState.money(rank.get(0).getValue()), l + 8, py + 218, Ui.GOLD);
        }

        int rxx = l + 208;
        int rww = cw - 208;
        g.drawString(this.font, t("HÖCHSTGEBOTE"), rxx + 8, py + 102, Ui.MUTED);
        String count = rank.size() + " " + (rank.size() == 1 ? t("Gebot") : t("Gebote"));
        g.drawString(this.font, count, rxx + rww - 8 - this.font.width(count), py + 102, Ui.MUTED);
        if (rank.isEmpty()) {
            String[] lines = {t("Noch keine Gebote"), t("Mache /pay") + " " + AuctionState.seller + " <" + t("Betrag") + ">", t("Mindestgebot") + ": " + AuctionState.money(AuctionState.startBid)};
            int[] colors = {Ui.WHITE, Ui.MUTED, Ui.GOLD};
            for (int i = 0; i < lines.length; i++) {
                String s = fit(lines[i], rww - 20);
                g.drawString(this.font, s, rxx + (rww - this.font.width(s)) / 2, py + 150 + i * 13, colors[i]);
            }
        } else {
            for (int i = 0; i < Math.min(9, rank.size()); i++) {
                Map.Entry<String, Long> e = rank.get(i);
                int y = py + 120 + i * 14;
                g.drawString(this.font, (i + 1) + ". " + fit(e.getKey(), 170), rxx + 8, y, i == 0 ? Ui.GOLD : Ui.WHITE);
                String amount = AuctionState.money(e.getValue());
                g.drawString(this.font, amount, rxx + rww - 8 - this.font.width(amount), y, i == 0 ? Ui.GOLD : Ui.MUTED);
            }
        }
    }

    private void renderGiveaway(GuiGraphics g) {
        GiveawayState.Phase phase = GiveawayState.phase;
        if (phase == GiveawayState.Phase.IDLE) {
            g.drawString(this.font, t("PREIS"), l + 8, ct + 10, Ui.MUTED);
            g.drawString(this.font, t("WORT ZUM MITMACHEN"), l + 8, ct + 60, Ui.MUTED);
            g.drawString(this.font, t("DAUER (SEKUNDEN)"), l + 16 + (cw - 24) / 2, ct + 60, Ui.MUTED);
            g.drawString(this.font, t("Wer das Wort in den Chat schreibt, nimmt teil."), l + 8, ct + 112, Ui.MUTED);
            g.drawString(this.font, t("Am Ende wird zufällig ein Gewinner gelost."), l + 8, ct + 124, Ui.MUTED);
            if (!GiveawayState.status.isEmpty()) {
                g.drawString(this.font, fit(GiveawayState.status, cw - 16), l + 8, ct + 150, Ui.RED);
            }
            return;
        }

        boolean running = phase == GiveawayState.Phase.RUNNING;
        drawLiveRow(g, running, t("Schreibe") + " \"" + GiveawayState.keyword + "\" " + t("in den Chat"),
                running ? GiveawayState.secondsLeft() : 0, GiveawayState.progress());

        List<String> list = GiveawayState.participants();

        g.drawString(this.font, t("PREIS"), l + 8, py + 104, Ui.MUTED);
        g.drawString(this.font, fit(GiveawayState.prize, 180), l + 8, py + 117, Ui.WHITE);
        g.drawString(this.font, t("Wort") + ": " + GiveawayState.keyword, l + 8, py + 140, Ui.GOLD);
        g.drawString(this.font, t("Teilnehmer") + ": " + list.size(), l + 8, py + 153, Ui.MUTED);

        if (!running) {
            g.drawString(this.font, t("GEWINNER"), l + 8, py + 192, Ui.MUTED);
            g.drawString(this.font, GiveawayState.winner == null ? t("Niemand") : fit(GiveawayState.winner, 180), l + 8, py + 205, Ui.GOLD);
        } else {
            String s = list.isEmpty() ? t("Noch niemand dabei") : t("Zuletzt") + ": " + list.get(list.size() - 1);
            g.drawString(this.font, fit(s, 180), l + 8, py + 210, Ui.MUTED);
        }

        int rxx = l + 208;
        int rww = cw - 208;
        g.drawString(this.font, t("TEILNEHMER"), rxx + 8, py + 102, Ui.MUTED);
        String count = list.size() + " " + (list.size() == 1 ? t("Person") : t("Personen"));
        g.drawString(this.font, count, rxx + rww - 8 - this.font.width(count), py + 102, Ui.MUTED);
        if (list.isEmpty()) {
            String s = t("Noch niemand dabei");
            g.drawString(this.font, s, rxx + (rww - this.font.width(s)) / 2, py + 170, Ui.MUTED);
        } else {
            for (int i = 0; i < Math.min(18, list.size()); i++) {
                int col = i / 9;
                int row = i % 9;
                g.drawString(this.font, fit(list.get(i), rww / 2 - 14), rxx + 8 + col * (rww / 2), py + 120 + row * 14, Ui.WHITE);
            }
        }
    }

    private void renderOptions(GuiGraphics g) {
        int x0 = l + 8;
        g.drawString(this.font, t("TELEPORT-COMMAND FÜR DEN GEWINNER"), x0, ct + 120, Ui.MUTED);
        g.drawString(this.font, "{name} = " + t("Spielername"), x0, ct + 158, Ui.MUTED);
        g.drawString(this.font, t("DESIGN"), l + 264, ct + 8, Ui.MUTED);
        g.drawString(this.font, Ui.THEMES[Ui.themeIndex()].name(), l + 264, ct + 24, Ui.WHITE);
    }

    private void renderAnim(GuiGraphics g) {
        g.drawString(this.font, t("Master AUS schaltet alle Effekte aus."), l + 8, ct + 148, Ui.MUTED);
        g.drawString(this.font, t("Vorschau"), l + 8, ct + 166, Ui.MUTED);
        drawProgress(g, l + 8, ct + 180, cw - 16, 6, 0.65f, true);
        double t = Ui.time();
        float wave = Ui.fx(BarakConfig.animPulse) ? (float) Math.abs(Math.sin(t * 3.5)) : 1f;
        Ui.round(g, l + 8, ct + 192 - 0, 8, 8, Ui.alpha(Ui.PINK, 0.4f + 0.6f * wave));
        g.drawString(this.font, "LIVE", l + 22, ct + 192, Ui.PINK);
    }
}
