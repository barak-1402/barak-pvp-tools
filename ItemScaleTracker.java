package dev.barak.pvptools;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/** Merkt sich pro Render-State die Größe des fallengelassenen Items. */
public final class ItemScaleTracker {
    private static final Map<Object, Float> SCALES = Collections.synchronizedMap(new WeakHashMap<>());

    private ItemScaleTracker() {
    }

    public static void set(Object state, float scale) {
        if (state != null) {
            SCALES.put(state, scale);
        }
    }

    public static float get(Object state) {
        if (state == null) {
            return 1.0f;
        }
        Float f = SCALES.get(state);
        return f == null ? 1.0f : f;
    }
}
