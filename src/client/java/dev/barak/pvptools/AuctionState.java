package dev.barak.pvptools;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Clientseitige Auktion: Zahlungen im Chat ("Du hast $10 von Name erhalten.") werden mitgezählt,
 * wer am Ende am meisten bezahlt hat (mindestens das Startgebot), gewinnt das Item.
 */
public final class AuctionState {
    public enum Phase { IDLE, RUNNING, ENDED }

    public static Phase phase = Phase.IDLE;
    public static ItemStack item = ItemStack.EMPTY;
    public static long startBid;
    public static int durationSeconds;
    public static long endMillis;
    public static long income;
    public static String winner;
    public static long winnerAmount;

    /** Eingaben im Fenster bleiben erhalten, auch wenn man den Tab wechselt. */
    public static String draftBid = "";
    public static String draftSeconds = "60";
    public static String status = "";

    private static final Map<String, Long> TOTALS = new LinkedHashMap<>();

    private static final Pattern PAYMENT = Pattern.compile(
            "Du hast \\$([0-9][0-9.,]*)\\s*([kKmMbB]?)\\s+von\\s+(\\S+)\\s+erhalten");
    private static final Pattern MONEY = Pattern.compile("^\\s*\\$?([0-9][0-9.,]*)\\s*([kKmMbB]?)\\s*$");

    private AuctionState() {
    }

    public static long parseMoney(String text) {
        if (text == null) {
            return -1;
        }
        Matcher m = MONEY.matcher(text);
        if (!m.find()) {
            return -1;
        }
        return toLong(m.group(1), m.group(2));
    }

    private static long toLong(String number, String suffix) {
        try {
            double value = Double.parseDouble(number.replace(",", ""));
            double mult = 1;
            switch (suffix.toLowerCase(Locale.ROOT)) {
                case "k" -> mult = 1_000d;
                case "m" -> mult = 1_000_000d;
                case "b" -> mult = 1_000_000_000d;
                default -> mult = 1d;
            }
            return Math.round(value * mult);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static String money(long amount) {
        return "$" + String.format(Locale.ROOT, "%,d", amount);
    }

    public static String formatTime(long seconds) {
        long s = Math.max(0, seconds);
        return String.format(Locale.ROOT, "%d:%02d", s / 60, s % 60);
    }

    public static String itemName() {
        if (item.isEmpty()) {
            return "-";
        }
        return item.getCount() + "x " + item.getHoverName().getString();
    }

    public static long secondsLeft() {
        return Math.max(0, (endMillis - System.currentTimeMillis() + 999) / 1000);
    }

    public static float progress() {
        if (phase != Phase.RUNNING || durationSeconds <= 0) {
            return 0f;
        }
        float left = (endMillis - System.currentTimeMillis()) / (durationSeconds * 1000f);
        return Math.max(0f, Math.min(1f, left));
    }

    public static void start(ItemStack held, long bid, int seconds) {
        TOTALS.clear();
        income = 0;
        winner = null;
        winnerAmount = 0;
        item = held.copy();
        startBid = bid;
        durationSeconds = seconds;
        endMillis = System.currentTimeMillis() + seconds * 1000L;
        phase = Phase.RUNNING;
        localMessage("[Barak] Auktion gestartet: " + itemName() + " | Startgebot " + money(bid));
    }

    public static void cancel() {
        phase = Phase.IDLE;
        TOTALS.clear();
        income = 0;
        winner = null;
        localMessage("[Barak] Auktion abgebrochen.");
    }

    public static void reset() {
        phase = Phase.IDLE;
        TOTALS.clear();
        income = 0;
        winner = null;
        winnerAmount = 0;
        item = ItemStack.EMPTY;
    }

    public static List<Map.Entry<String, Long>> ranking() {
        List<Map.Entry<String, Long>> list = new ArrayList<>(TOTALS.entrySet());
        list.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        return list;
    }

    public static String teleportCommand() {
        if (winner == null) {
            return "";
        }
        return BarakConfig.tpCommand.replace("{name}", winner);
    }

    /** Wird für jede Chatzeile aufgerufen. */
    public static void onLine(String line) {
        if (phase != Phase.RUNNING || line == null) {
            return;
        }
        Matcher m = PAYMENT.matcher(line);
        if (!m.find()) {
            return;
        }
        long amount = toLong(m.group(1), m.group(2));
        if (amount <= 0) {
            return;
        }
        String name = m.group(3);
        TOTALS.merge(name, amount, Long::sum);
        income += amount;
    }

    public static void tick() {
        if (phase == Phase.RUNNING && System.currentTimeMillis() >= endMillis) {
            finish();
        }
    }

    private static void finish() {
        phase = Phase.ENDED;
        List<Map.Entry<String, Long>> rank = ranking();
        if (!rank.isEmpty() && rank.get(0).getValue() >= startBid) {
            winner = rank.get(0).getKey();
            winnerAmount = rank.get(0).getValue();
            localMessage("[Barak] Auktion beendet! Gewinner: " + winner + " mit " + money(winnerAmount)
                    + " | Einnahmen: " + money(income));
        } else {
            winner = null;
            localMessage("[Barak] Auktion beendet – kein Gebot über dem Startgebot. Einnahmen: " + money(income));
        }
    }

    public static void localMessage(String text) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal(text), false);
        }
    }
}
