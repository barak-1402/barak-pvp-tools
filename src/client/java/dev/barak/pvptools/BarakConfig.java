package dev.barak.pvptools;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/** Einstellungen, gespeichert in config/barakpvptools.json. */
public final class BarakConfig {
    public static final float MIN_SCALE = 0.2f;
    public static final float MAX_SCALE = 3.0f;

    public static volatile float itemScale = 1.0f;
    public static volatile boolean showLogo = true;
    public static volatile boolean showInfo = true;
    public static volatile boolean showAuctionHud = true;
    public static volatile boolean showJoinMessage = true;
    /** Befehl zum Teleportieren zum Gewinner, {name} wird ersetzt. */
    public static volatile String tpCommand = "/tpa {name}";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final class Data {
        float itemScale = 1.0f;
        boolean showLogo = true;
        boolean showInfo = true;
        boolean showAuctionHud = true;
        boolean showJoinMessage = true;
        String tpCommand = "/tpa {name}";
    }

    private BarakConfig() {
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

    public static void load() {
        try {
            Path path = file();
            if (Files.exists(path)) {
                Data data = GSON.fromJson(Files.readString(path), Data.class);
                if (data != null) {
                    itemScale = clampScale(data.itemScale);
                    showLogo = data.showLogo;
                    showInfo = data.showInfo;
                    showAuctionHud = data.showAuctionHud;
                    showJoinMessage = data.showJoinMessage;
                    if (data.tpCommand != null && !data.tpCommand.isBlank()) {
                        tpCommand = data.tpCommand;
                    }
                }
            }
        } catch (Exception e) {
            BarakPvPTools.LOGGER.warn("Konfiguration konnte nicht geladen werden, nutze Standardwerte.", e);
        }
    }

    public static void save() {
        try {
            Data data = new Data();
            data.itemScale = itemScale;
            data.showLogo = showLogo;
            data.showInfo = showInfo;
            data.showAuctionHud = showAuctionHud;
            data.showJoinMessage = showJoinMessage;
            data.tpCommand = tpCommand;
            Files.writeString(file(), GSON.toJson(data));
        } catch (Exception e) {
            BarakPvPTools.LOGGER.warn("Konfiguration konnte nicht gespeichert werden.", e);
        }
    }
}
