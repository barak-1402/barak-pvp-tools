package dev.barak.pvptools;

import net.minecraft.client.gui.GuiGraphics;

/** Kleine Zeichen-Helfer für das Design (Weinrot/Pink). Nutzt nur fill(), daher sehr stabil. */
public final class Ui {
    public static final int BG = 0xF2240D1B;
    public static final int CARD = 0xFF361428;
    public static final int CARD_LINE = 0xFF6E2B4A;
    public static final int PINK = 0xFFFF5C93;
    public static final int PINK_DARK = 0xFFB8335F;
    public static final int WHITE = 0xFFFFFFFF;
    public static final int MUTED = 0xFFC9A3B5;
    public static final int GOLD = 0xFFFFD166;
    public static final int GREEN = 0xFF5CFF9D;
    public static final int RED = 0xFFFF6B6B;
    public static final int GHOST = 0xFF4A1830;

    // Bits: a=0 b=1 c=2 d=3 e=4 f=5 g=6
    private static final int[] DIGITS = {63, 6, 91, 79, 102, 109, 125, 7, 127, 111};

    private Ui() {
    }

    public static int alpha(int color, float factor) {
        int a = (int) (((color >>> 24) & 0xFF) * Math.max(0f, Math.min(1f, factor)));
        return (a << 24) | (color & 0xFFFFFF);
    }

    /** Rechteck mit leicht abgerundeten Ecken. */
    public static void round(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x + 1, y, x + w - 1, y + h, color);
        g.fill(x, y + 1, x + w, y + h - 1, color);
    }

    public static void card(GuiGraphics g, int x, int y, int w, int h, int fill, int border) {
        round(g, x, y, w, h, border);
        round(g, x + 1, y + 1, w - 2, h - 2, fill);
    }

    /** Große Zeit im LCD-Stil, z.B. "00:09". */
    public static int bigTime(GuiGraphics g, int x, int y, String text, int color) {
        int cx = x;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= '0' && c <= '9') {
                drawDigit(g, cx, y, 127, GHOST);
                drawDigit(g, cx, y, DIGITS[c - '0'], color);
                cx += 13;
            } else {
                g.fill(cx + 1, y + 4, cx + 3, y + 6, color);
                g.fill(cx + 1, y + 12, cx + 3, y + 14, color);
                cx += 6;
            }
        }
        return cx - x;
    }

    public static int bigTimeWidth(String text) {
        int w = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            w += (c >= '0' && c <= '9') ? 13 : 6;
        }
        return w;
    }

    private static void drawDigit(GuiGraphics g, int x, int y, int mask, int color) {
        int w = 10;
        int h = 18;
        int hh = h / 2;
        if ((mask & 1) != 0) g.fill(x + 1, y, x + w - 1, y + 2, color);
        if ((mask & 2) != 0) g.fill(x + w - 2, y + 1, x + w, y + hh, color);
        if ((mask & 4) != 0) g.fill(x + w - 2, y + hh, x + w, y + h - 1, color);
        if ((mask & 8) != 0) g.fill(x + 1, y + h - 2, x + w - 1, y + h, color);
        if ((mask & 16) != 0) g.fill(x, y + hh, x + 2, y + h - 1, color);
        if ((mask & 32) != 0) g.fill(x, y + 1, x + 2, y + hh, color);
        if ((mask & 64) != 0) g.fill(x + 1, y + hh - 1, x + w - 1, y + hh + 1, color);
    }
}
