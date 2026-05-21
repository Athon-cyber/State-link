package com.statelink.app;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Manages the app's theme selection across sessions.
 *
 * Three built-in themes:
 *  0 — Light   (white background, dark text)
 *  1 — Dark    (dark background, light text)
 *  2 — Tech Blue (deep navy background, cyan accents)
 */
public class ThemeManager {

    private static final String PREFS_FILE = "statelink_prefs";
    private static final String KEY_THEME = "app_theme";

    public static final int THEME_LIGHT = 0;
    public static final int THEME_DARK  = 1;
    public static final int THEME_BLUE  = 2;

    private final SharedPreferences prefs;

    /** Maps each theme index to its style resource ID in styles.xml */
    private static final int[] THEME_STYLE_IDS = {
        R.style.Theme_StateLink_Light,   // 0
        R.style.Theme_StateLink_Dark,    // 1
        R.style.Theme_StateLink_Blue,    // 2
    };

    public ThemeManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
    }

    /** Returns the previously selected theme index (default: Dark). */
    public int getCurrentThemeIndex() {
        return prefs.getInt(KEY_THEME, THEME_DARK);
    }

    /** Returns the Android style resource ID for the current theme. */
    public int getCurrentThemeResId() {
        return THEME_STYLE_IDS[getCurrentThemeIndex()];
    }

    /** Persists a new theme choice. Caller should recreate() the activity after this. */
    public void setTheme(int themeIndex) {
        int idx = Math.max(THEME_LIGHT, Math.min(THEME_BLUE, themeIndex));
        prefs.edit().putInt(KEY_THEME, idx).apply();
    }
}
