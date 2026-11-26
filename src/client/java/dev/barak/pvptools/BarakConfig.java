package dev.barak.pvptools;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Einstellungen, gespeichert in config/barakpvptools.json. */
public final class BarakConfig {
    public static final float MIN_SCALE = 0.25f;
    public static final float MAX_SCALE = 4.0f;
    public static final int MAX_INVSEE_ITEMS = 12;

    /** Item-ID (z.B. minecraft:end_crystal) -> Größe für fallengelassene Items. */
    public static final Map<String, Float> itemScales = new LinkedHashMap<>();
    /** Items, die beim Invsee gezählt werden. */
    public static final List<String> invseeItems = new ArrayList<>();

    public static volatile String language = "de";
    public static volatile boolean glowDropped = false;
    public static volatile int themeIndex = 0;
    public static volatile boolean showLogo = true;
    public static volatile boolean showAuctionHud = true;
    public static volatile boolean showJoinMessage = true;
    public static volatile boolean invseeEnabled = true;
    public static volatile boolean invseeAlways = false;
    /** Befehl zum Teleportieren zum Gewinner, {name} wird ersetzt. */
    public static volatile String tpCommand = "/tpa {name}";

    // Animationen
    public static volatile boolean animEnabled = true;
    public static volatile boolean animPopout = true;
    public static volatile boolean animParticles = true;
    public static volatile boolean animBorder = true;
    public static volatile boolean animPulse = true;
    public static volatile boolean animShimmer = true;
    public static volatile boolean animTitle = true;
    public static volatile boolean animHover = true;
    /** 0 = langsam, 1 = normal, 2 = schnell. */
    public static volatile int animSpeed = 1;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final class Data {
        Map<String, Float> itemScales = new LinkedHashMap<>();
        List<String> invseeItems = null;
        String language = "de";
        boolean glowDropped = false;
        int themeIndex = 0;
        boolean showLogo = true;
        boolean showAuctionHud = true;
        boolean showJoinMessage = true;
        boolean invseeEnabled = true;
        boolean invseeAlways = false;
        String tpCommand = "/tpa {name}";
        boolean animEnabled = true;
        boolean animPopout = true;
        boolean animParticles = true;
        boolean animBorder = true;
        boolean animPulse = true;
        boolean animShimmer = true;
        boolean animTitle = true;
        boolean animHover = true;
        int animSpeed = 1;
    }

    static {
        resetInvseeDefaults();
    }

    private BarakConfig() {
    }

    private static void resetInvseeDefaults() {
        invseeItems.clear();
        invseeItems.add("minecraft:end_crystal");
        invseeItems.add("minecraft:totem_of_undying");
        invseeItems.add("minecraft:enchanted_golden_apple");
        invseeItems.add("minecraft:golden_apple");
        invseeItems.add("minecraft:ender_pearl");
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("barakpvptools.json");
    }

    public static float clampScale(float value) {
        if (Float.isNaN(value)) {
            return 1.0f;
        }
        float clamped = Math.max(MIN_SCALE, Math.min(MAX_SCALE, value));
        return Math.round(clamped * 100f) / 100f;
    }

    public static String keyOf(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    public static float scaleOf(Item item) {
        if (itemScales.isEmpty()) {
            return 1.0f;
        }
        Float f = itemScales.get(keyOf(item));
        return f == null ? 1.0f : f;
    }

    public static float scaleFor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 1.0f;
        }
        return scaleOf(stack.getItem());
    }

    public static void setScale(Item item, float value) {
        float v = clampScale(value);
        String key = keyOf(item);
        if (Math.abs(v - 1.0f) < 0.001f) {
            itemScales.remove(key);
        } else {
            itemScales.put(key, v);
        }
        save();
    }

    public static void removeScale(Item item) {
        itemScales.remove(keyOf(item));
        save();
    }

    public static boolean isInvseeItem(Item item) {
        return invseeItems.contains(keyOf(item));
    }

    public static void toggleInvseeItem(Item item) {
        String key = keyOf(item);
        if (invseeItems.contains(key)) {
            invseeItems.remove(key);
        } else if (invseeItems.size() < MAX_INVSEE_ITEMS) {
            invseeItems.add(key);
        }
        save();
    }

    public static void load() {
        try {
            Path path = file();
            if (Files.exists(path)) {
                Data data = GSON.fromJson(Files.readString(path), Data.class);
                if (data != null) {
                    itemScales.clear();
                    if (data.itemScales != null) {
                        for (Map.Entry<String, Float> e : data.itemScales.entrySet()) {
                            if (e.getKey() != null && e.getValue() != null) {
                                itemScales.put(e.getKey(), clampScale(e.getValue()));
                            }
                        }
                    }
                    if (data.invseeItems != null) {
                        invseeItems.clear();
                        for (String s : data.invseeItems) {
                            if (s != null && invseeItems.size() < MAX_INVSEE_ITEMS) {
                                invseeItems.add(s);
                            }
                        }
                    }
                    language = "en".equals(data.language) ? "en" : "de";
                    glowDropped = data.glowDropped;
                    themeIndex = (data.themeIndex < 0 || data.themeIndex >= Ui.THEMES.length) ? 0 : data.themeIndex;
                    showLogo = data.showLogo;
                    showAuctionHud = data.showAuctionHud;
                    showJoinMessage = data.showJoinMessage;
                    invseeEnabled = data.invseeEnabled;
                    invseeAlways = data.invseeAlways;
                    if (data.tpCommand != null && !data.tpCommand.isBlank()) {
                        tpCommand = data.tpCommand;
                    }
                    animEnabled = data.animEnabled;
                    animPopout = data.animPopout;
                    animParticles = data.animParticles;
                    animBorder = data.animBorder;
                    animPulse = data.animPulse;
                    animShimmer = data.animShimmer;
                    animTitle = data.animTitle;
                    animHover = data.animHover;
                    animSpeed = (data.animSpeed < 0 || data.animSpeed > 2) ? 1 : data.animSpeed;
                }
            }
        } catch (Exception e) {
            BarakPvPTools.LOGGER.warn("Konfiguration konnte nicht geladen werden, nutze Standardwerte.", e);
        }
    }

    public static void save() {
        try {
            Data data = new Data();
            data.itemScales = new LinkedHashMap<>(itemScales);
            data.invseeItems = new ArrayList<>(invseeItems);
            data.language = language;
            data.glowDropped = glowDropped;
            data.themeIndex = themeIndex;
            data.showLogo = showLogo;
            data.showAuctionHud = showAuctionHud;
            data.showJoinMessage = showJoinMessage;
            data.invseeEnabled = invseeEnabled;
            data.invseeAlways = invseeAlways;
            data.tpCommand = tpCommand;
            data.animEnabled = animEnabled;
            data.animPopout = animPopout;
            data.animParticles = animParticles;
            data.animBorder = animBorder;
            data.animPulse = animPulse;
            data.animShimmer = animShimmer;
            data.animTitle = animTitle;
            data.animHover = animHover;
            data.animSpeed = animSpeed;
            Files.writeString(file(), GSON.toJson(data));
        } catch (Exception e) {
            BarakPvPTools.LOGGER.warn("Konfiguration konnte nicht gespeichert werden.", e);
        }
    }
}
