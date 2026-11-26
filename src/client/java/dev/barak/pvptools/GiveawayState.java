package dev.barak.pvptools;

import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Clientseitiges Giveaway: Wer das Schlüsselwort in den Chat schreibt, nimmt teil. */
public final class GiveawayState {
    public enum Phase { IDLE, RUNNING, ENDED }

    public static Phase phase = Phase.IDLE;
    public static String prize = "";
    public static String keyword = "join";
    public static int durationSeconds;
    public static long endMillis;
    public static String winner;

    public static String draftPrize = "";
    public static String draftKeyword = "join";
    public static String draftSeconds = "60";
    public static String status = "";

    private static final Set<String> PARTICIPANTS = new LinkedHashSet<>();

    // z.B. "[VIP] Steve: join" oder "<Steve> join" oder "Steve » join"
    private static final Pattern CHAT = Pattern.compile(
            "^(?:\\[[^\\]]*\\]\\s*)*<?([A-Za-z0-9_]{3,16})>?\\s*[:»>]\\s*(.+)$");

    private GiveawayState() {
    }

    public static List<String> participants() {
        return new ArrayList<>(PARTICIPANTS);
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

    public static void start(String prizeText, String word, int seconds) {
        PARTICIPANTS.clear();
        winner = null;
        prize = prizeText;
        keyword = word;
        durationSeconds = seconds;
        endMillis = System.currentTimeMillis() + seconds * 1000L;
        phase = Phase.RUNNING;
        AuctionState.localMessage(Lang.t("[Barak] Giveaway gestartet: ") + prize + Lang.t(" | Wort: ") + keyword);
    }

    public static void cancel() {
        phase = Phase.IDLE;
        PARTICIPANTS.clear();
        winner = null;
        AuctionState.localMessage(Lang.t("[Barak] Giveaway abgebrochen."));
    }

    public static void reset() {
        phase = Phase.IDLE;
        PARTICIPANTS.clear();
        winner = null;
    }

    public static void onLine(String line) {
        if (phase != Phase.RUNNING || line == null) {
            return;
        }
        Matcher m = CHAT.matcher(line.trim());
        if (!m.find()) {
            return;
        }
        String name = m.group(1);
        String message = m.group(2).trim();
        if (!message.equalsIgnoreCase(keyword)) {
            return;
        }
        String self = Minecraft.getInstance().getUser().getName();
        if (name.equalsIgnoreCase(self)) {
            return;
        }
        PARTICIPANTS.add(name);
    }

    public static void tick() {
        if (phase == Phase.RUNNING && System.currentTimeMillis() >= endMillis) {
            draw();
        }
    }

    /** Lost einen Gewinner aus (auch vorzeitig per Knopf). */
    public static void draw() {
        if (phase != Phase.RUNNING) {
            return;
        }
        phase = Phase.ENDED;
        if (PARTICIPANTS.isEmpty()) {
            winner = null;
            AuctionState.localMessage(Lang.t("[Barak] Giveaway beendet – niemand hat teilgenommen."));
            return;
        }
        List<String> list = participants();
        winner = list.get(ThreadLocalRandom.current().nextInt(list.size()));
        AuctionState.localMessage(Lang.t("[Barak] Giveaway beendet! Gewinner: ") + winner + " (" + list.size() + Lang.t(" Teilnehmer)"));
    }
}
