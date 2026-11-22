package dev.barak.pvptools;

import net.minecraft.client.gui.GuiGraphics;

/** Zeichen-Helfer und Farbdesigns. Nutzt nur fill(), daher sehr stabil. */
public final class Ui {
    /** cycle: 0 = fest, 1 = Aurora (Polarlicht), 2 = Holografisch (Regenbogen). */
    public record Theme(String name, int bg, int card, int line, int accent, int accentDark, int muted, int secondary, int ghost, int cycle) {
    }

    public static final Theme[] THEMES = {
            new Theme("Kirschblüte", 0xF2240D1B, 0xFF361428, 0xFF6E2B4A, 0xFFFF5C93, 0xFFB8335F, 0xFFC9A3B5, 0xFFFFD166, 0xFF4A1830, 0),
            new Theme("Dark Mode", 0xF2121316, 0xFF1E2025, 0xFF3A3D46, 0xFF4DA3FF, 0xFF2A6FBF, 0xFF9AA0AD, 0xFFFFD166, 0xFF2A2D35, 0),
            new Theme("Cyberpunk", 0xF20B0B14, 0xFF15152A, 0xFF2D2D55, 0xFFFFE600, 0xFF8A7F00, 0xFF8EA0C8, 0xFF00F0FF, 0xFF22223F, 0),
            new Theme("Neon Glow", 0xF2050808, 0xFF0D1614, 0xFF1F4A3F, 0xFF39FF9C, 0xFF14A664, 0xFF8FBFAE, 0xFFFF3DCB, 0xFF12302A, 0),
            new Theme("Luxury", 0xF20D0B08, 0xFF1A1610, 0xFF5A4A22, 0xFFE6B84A, 0xFF9A7A22, 0xFFBFB08A, 0xFFFFE9A8, 0xFF3A3018, 0),
            new Theme("Eis", 0xF20C1A26, 0xFF132A3C, 0xFF2F5C80, 0xFF7FD6FF, 0xFF2E86B8, 0xFFA9CCE0, 0xFFFFFFFF, 0xFF1D3C54, 0),
            new Theme("Feuer", 0xF21E0A06, 0xFF331308, 0xFF7A2E12, 0xFFFF7A1A, 0xFFBF3F0A, 0xFFD9A58C, 0xFFFFD166, 0xFF4A1E0E, 0),
            new Theme("Ozean", 0xF2061A2A, 0xFF0C2E47, 0xFF1F6A91, 0xFF2DD4BF, 0xFF148577, 0xFF9CC8D6, 0xFF7FD6FF, 0xFF123F5C, 0),
            new Theme("Terminal", 0xF2030A04, 0xFF08140A, 0xFF1C5A24, 0xFF33FF55, 0xFF16A02E, 0xFF6FBF7A, 0xFFB8FFB8, 0xFF0F2A12, 0),
            new Theme("AMOLED", 0xFF000000, 0xFF0A0A0A, 0xFF2A2A2A, 0xFFFFFFFF, 0xFF555555, 0xFF9A9A9A, 0xFFFF5C93, 0xFF1A1A1A, 0),
            new Theme("Aurora", 0xF2081420, 0xFF0F2433, 0xFF2A5470, 0xFF55FFC0, 0xFF1E9C78, 0xFFA3C4D6, 0xFF9DFFD6, 0xFF163246, 1),
            new Theme("Holografisch", 0xF2101018, 0xFF1A1A2A, 0xFF3A3A5A, 0xFFFFFFFF, 0xFF808099, 0xFFB5B5D0, 0xFFFFFFFF, 0xFF262638, 2),
    };

    public static final int WHITE = 0xFFFFFFFF;
    public static final int GREEN = 0xFF5CFF9D;
    public static final int RED = 0xFFFF6B6B;

    // Aktuelle Farben (werden pro Frame aus dem gewählten Design gesetzt)
    public static int BG = THEMES[0].bg();
    public static int CARD = THEMES[0].card();
    public static int CARD_LINE = THEMES[0].line();
    public static int PINK = THEMES[0].accent();
    public static int PINK_DARK = THEMES[0].accentDark();
    public static int MUTED = THEMES[0].muted();
    public static int GOLD = THEMES[0].secondary();
    public static int GHOST = THEMES[0].ghost();

    // Bits: a=0 b=1 c=2 d=3 e=4 f=5 g=6
    private static final int[] DIGITS = {63, 6, 91, 79, 102, 109, 125, 7, 127, 111};

    private Ui() {
    }

    public static int themeIndex() {
        int i = BarakConfig.themeIndex;
        return (i < 0 || i >= THEMES.length) ? 0 : i;
    }

    /** Setzt die Farben für den aktuellen Frame (inkl. animierter Designs). */
    public static void apply() {
        Theme t = THEMES[themeIndex()];
        double time = System.currentTimeMillis() / 1000.0;
        BG = t.bg();
        CARD = t.card();
        CARD_LINE = t.line();
        PINK = t.accent();
        PINK_DARK = t.accentDark();
        MUTED = t.muted();
        GOLD = t.secondary();
        GHOST = t.ghost();
        if (t.cycle() == 1) {
            float h = 0.33f + 0.32f * (float) (0.5 + 0.5 * Math.sin(time * 0.5));
            PINK = hsv(h, 0.55f, 1.0f);
            PINK_DARK = hsv(h, 0.70f, 0.55f);
        } else if (t.cycle() == 2) {
            float h = (float) ((time * 0.12) % 1.0);
            PINK = hsv(h, 0.45f, 1.0f);
            PINK_DARK = hsv(h, 0.60f, 0.60f);
            GOLD = hsv((h + 0.5f) % 1.0f, 0.35f, 1.0f);
        }
    }

    public static int hsv(float h, float s, float v) {
        float hh = (h - (float) Math.floor(h)) * 6.0f;
        int i = (int) hh;
        float f = hh - i;
        float p = v * (1 - s);
        float q = v * (1 - f * s);
        float t = v * (1 - (1 - f) * s);
        float r;
        float g;
        float b;
        switch (i % 6) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return 0xFF000000 | ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
    }

    public static int mix(int a, int b, float t) {
        float k = Math.max(0f, Math.min(1f, t));
        int aa = (int) (((a >>> 24) & 0xFF) * (1 - k) + ((b >>> 24) & 0xFF) * k);
        int ar = (int) (((a >> 16) & 0xFF) * (1 - k) + ((b >> 16) & 0xFF) * k);
        int ag = (int) (((a >> 8) & 0xFF) * (1 - k) + ((b >> 8) & 0xFF) * k);
        int ab = (int) ((a & 0xFF) * (1 - k) + (b & 0xFF) * k);
        return (aa << 24) | (ar << 16) | (ag << 8) | ab;
    }

    public static int luminance(int c) {
        return (int) (0.299 * ((c >> 16) & 0xFF) + 0.587 * ((c >> 8) & 0xFF) + 0.114 * (c & 0xFF));
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

    /** Wandernder Lichtpunkt am Rand eines Rechtecks (Border Glow). */
    public static void borderGlow(GuiGraphics g, int x, int y, int w, int h, int color, double time) {
        int perimeter = 2 * (w + h);
        int head = (int) ((time * 110.0) % perimeter);
        for (int i = 0; i < 46; i++) {
            int p = ((head - i) % perimeter + perimeter) % perimeter;
            int px;
            int py;
            if (p < w) {
                px = x + p;
                py = y;
            } else if (p < w + h) {
                px = x + w - 1;
                py = y + (p - w);
            } else if (p < 2 * w + h) {
                px = x + w - 1 - (p - w - h);
                py = y + h - 1;
            } else {
                px = x;
                py = y + h - 1 - (p - 2 * w - h);
            }
            g.fill(px, py, px + 2, py + 2, alpha(color, 1.0f - i / 46f));
        }
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
