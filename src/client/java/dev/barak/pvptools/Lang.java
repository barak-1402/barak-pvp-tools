package dev.barak.pvptools;

import java.util.HashMap;
import java.util.Map;

/** Sehr einfache Übersetzung: Deutsche Texte sind der Schlüssel, Englisch kommt aus der Tabelle. */
public final class Lang {
    private static final Map<String, String> EN = new HashMap<>();

    static {
        EN.put(" Teilnehmer)", " participants)");
        EN.put(" mit ", " with ");
        EN.put(" | Einnahmen: ", " | Income: ");
        EN.put(" | Startgebot ", " | Starting bid ");
        EN.put(" | Wort: ", " | Word: ");
        EN.put("AN", "ON");
        EN.put("AUS", "OFF");
        EN.put("ANZUZEIGEN", "TO SHOW");
        EN.put("AUSGEWÄHLT", "SELECTED");
        EN.put("Am Ende wird zufällig ein Gewinner gelost.", "A winner is drawn at random at the end.");
        EN.put("Auktion", "Auction");
        EN.put("Beendet", "Ended");
        EN.put("Betrag", "amount");
        EN.put("Browser nicht verfügbar – Link kopiert: ", "Browser unavailable – link copied: ");
        EN.put("DAUER (SEKUNDEN)", "DURATION (SECONDS)");
        EN.put("DESIGN", "THEME");
        EN.put("Dann stellst du die", "Then set the size");
        EN.put("Dauer: 10 bis 3600 Sekunden.", "Duration: 10 to 3600 seconds.");
        EN.put("Discord wird im Browser geöffnet …", "Opening Discord in your browser …");
        EN.put("EINNAHMEN", "INCOME");
        EN.put("ENDE", "ENDED");
        EN.put("Effekte", "Effects");
        EN.put("Einnahmen", "Income");
        EN.put("GEWINNER", "WINNER");
        EN.put("Gebot", "bid");
        EN.put("Gebote", "bids");
        EN.put("Geschwindigkeit", "Speed");
        EN.put("Gespeichert", "Saved");
        EN.put("Gewinner", "Winner");
        EN.put("Gib ein Wort zum Mitmachen ein.", "Enter a word to join.");
        EN.put("Gib einen Preis ein.", "Enter a prize.");
        EN.put("Gilt für Items am Boden", "Applies to items on the ground");
        EN.put("Giveaway-Gewinner", "Giveaway winner");
        EN.put("Glückwunsch!", "Congratulations!");
        EN.put("Größe", "Size");
        EN.put("Größe für Drops ein.", "for dropped items.");
        EN.put("Hauptmenü", "Main menu");
        EN.put("HÖCHSTES GEBOT", "HIGHEST BID");
        EN.put("HÖCHSTGEBOTE", "TOP BIDS");
        EN.put("ITEM AUS INVENTAR WÄHLEN", "PICK ITEM FROM INVENTORY");
        EN.put("ITEM AUSWÄHLEN", "SELECT ITEM");
        EN.put("Inventar", "Inventory");
        EN.put("Inventar steht, wie viele der gewählten", "inventory shows how many of the chosen");
        EN.put("Invsee", "Invsee");
        EN.put("Item-Größe", "Item size");
        EN.put("Items der Spieler hat.", "items the player has.");
        EN.put("Kein Gewinner", "No winner");
        EN.put("Kopiert: ", "Copied: ");
        EN.put("Käufer sollen schreiben:", "Buyers should type:");
        EN.put("Langsam", "Slow");
        EN.put("MINDESTGEBOT", "MINIMUM BID");
        EN.put("Mache /pay", "Do /pay");
        EN.put("Master AUS schaltet alle Effekte aus.", "Master OFF disables all effects.");
        EN.put("Min", "Min");
        EN.put("Mindestgebot", "Minimum bid");
        EN.put("Neue Auktion", "New auction");
        EN.put("Neues Giveaway", "New giveaway");
        EN.put("Nichts ausgewählt", "Nothing selected");
        EN.put("Niemand", "Nobody");
        EN.put("Noch keine Gebote", "No bids yet");
        EN.put("Noch nichts gewählt", "Nothing chosen yet");
        EN.put("Noch niemand dabei", "Nobody has joined yet");
        EN.put("Normal", "Normal");
        EN.put("Optionen", "Options");
        EN.put("PREIS", "PRIZE");
        EN.put("Person", "person");
        EN.put("Personen", "people");
        EN.put("Schnell", "Fast");
        EN.put("Schreibe", "Type");
        EN.put("Spielername", "player name");
        EN.put("Sprache", "Language");
        EN.put("TEILNEHMER", "PARTICIPANTS");
        EN.put("TELEPORT-COMMAND FÜR DEN GEWINNER", "TELEPORT COMMAND FOR THE WINNER");
        EN.put("Teilnehmer", "Participants");
        EN.put("Tipp: Item anklicken.", "Tip: click an item.");
        EN.put("Treffer", "results");
        EN.put("Ungültige Dauer.", "Invalid duration.");
        EN.put("Ungültiges Mindestgebot.", "Invalid minimum bid.");
        EN.put("Version", "Version");
        EN.put("Vorschau", "Preview");
        EN.put("WORT ZUM MITMACHEN", "WORD TO JOIN");
        EN.put("Weg", "Del");
        EN.put("Wer das Wort in den Chat schreibt, nimmt teil.", "Whoever types the word in chat takes part.");
        EN.put("Wort", "Word");
        EN.put("Wähle links ein Item.", "Pick an item on the left.");
        EN.put("Wähle zuerst ein Item aus deinem Inventar.", "Pick an item from your inventory first.");
        EN.put("Zuletzt", "Latest");
        EN.put("[Barak] Auktion abgebrochen.", "[Barak] Auction cancelled.");
        EN.put("[Barak] Auktion beendet – kein Gebot über dem Startgebot. Einnahmen: ", "[Barak] Auction ended – no bid above the starting bid. Income: ");
        EN.put("[Barak] Auktion beendet! Gewinner: ", "[Barak] Auction ended! Winner: ");
        EN.put("[Barak] Auktion gestartet: ", "[Barak] Auction started: ");
        EN.put("[Barak] Giveaway abgebrochen.", "[Barak] Giveaway cancelled.");
        EN.put("[Barak] Giveaway beendet – niemand hat teilgenommen.", "[Barak] Giveaway ended – nobody took part.");
        EN.put("[Barak] Giveaway beendet! Gewinner: ", "[Barak] Giveaway ended! Winner: ");
        EN.put("[Barak] Giveaway gestartet: ", "[Barak] Giveaway started: ");
        EN.put("gewinnt", "wins");
        EN.put("in den Chat", "in chat");
        EN.put("in den Chat!", "in chat!");
        EN.put("noch", "left");
        EN.put("Öffne /invsee <Name>: links neben dem", "Open /invsee <name>: left of the");
        EN.put("Öffnen", "Open");
        EN.put("Home", "Home");
        EN.put("Giveaway", "Giveaway");
        EN.put("Schließen", "Close");
        EN.put("Beenden", "End");
        EN.put("Zurücksetzen", "Reset");
        EN.put("Werbung", "Advertise");
        EN.put("TP-Command kopieren", "Copy TP command");
        EN.put("Auslosen", "Draw");
        EN.put("Gewinner verkünden", "Announce winner");
        EN.put("Auktion starten", "Start auction");
        EN.put("Giveaway starten", "Start giveaway");
        EN.put("Leuchten", "Glow");
        EN.put("Invsee-Anzeige", "Invsee display");
        EN.put("Bei jeder Kiste", "On every chest");
        EN.put("Logo oben links", "Logo top left");
        EN.put("Auktion/Giveaway-Anzeige", "Auction/Giveaway display");
        EN.put("Discord-Hinweis beim Beitreten", "Discord hint on join");
        EN.put("Animationen (alle)", "Animations (all)");
        EN.put("Pop-out beim Öffnen", "Pop-out on open");
        EN.put("Partikel", "Particles");
        EN.put("Wandernder Rand-Glanz", "Moving border glow");
        EN.put("Glow-Puls", "Glow pulse");
        EN.put("Glanz-Effekt", "Shimmer effect");
        EN.put("Titel-Effekte", "Title effects");
        EN.put("Hover-Übergang", "Hover transition");
        EN.put("Item suchen (z.B. crystal)", "Search item (e.g. crystal)");
        EN.put("Mindestgebot (z.B. 2m)", "Minimum bid (e.g. 2m)");
        EN.put("Dauer in Sekunden", "Duration in seconds");
        EN.put("Preis (z.B. 3x End Crystal)", "Prize (e.g. 3x End Crystal)");
        EN.put("Wort zum Mitmachen", "Word to join");
        EN.put("Teleport-Command", "Teleport command");
        EN.put("Drops größer oder kleiner|machen. Mit Leuchten.", "Make dropped items bigger or|smaller. With glow.");
        EN.put("Items versteigern mit|Live-Anzeige und Timer.", "Auction items with|live display and timer.");
        EN.put("Items verlosen per|Chat-Wort.", "Give items away with|a chat word.");
        EN.put("Zeigt Totems, Crystals und|mehr neben dem Inventar.", "Shows totems, crystals and|more next to the inventory.");
        EN.put("Sprache, Design und|Anzeige einstellen.", "Set language, theme and|display.");
        EN.put("Animationen ein- und|ausschalten.", "Turn animations on and|off.");
    }

    private Lang() {
    }

    public static boolean isEnglish() {
        return "en".equals(BarakConfig.language);
    }

    public static String t(String de) {
        if (!isEnglish()) {
            return de;
        }
        String en = EN.get(de);
        return en == null ? de : en;
    }
}
