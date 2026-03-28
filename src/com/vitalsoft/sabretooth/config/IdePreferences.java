package com.vitalsoft.sabretooth.config;

import java.io.*;
import java.util.*;
import java.nio.file.*;

public class IdePreferences {
    private static final String PREFS_DIR  = System.getProperty("user.home") + File.separator + ".sabretooth";
    private static final String PREFS_FILE = PREFS_DIR + File.separator + "ide.conf";

    private static IdePreferences instance;
    private final Properties props = new Properties();

    // Keys
    public static final String KEY_RECENT_PROJECTS  = "ide.recentProjects";
    public static final String KEY_LAST_OPEN_DIR    = "ide.lastOpenDir";
    public static final String KEY_FONT_SIZE         = "editor.fontSize";
    public static final String KEY_TAB_SIZE          = "editor.tabSize";
    public static final String KEY_SHOW_LINE_NUMS    = "editor.showLineNumbers";
    public static final String KEY_WORD_WRAP         = "editor.wordWrap";
    public static final String KEY_DEFAULT_GRADLE    = "tools.defaultGradle";
    public static final String KEY_DEFAULT_JDK       = "tools.defaultJdk";
    public static final String KEY_DEFAULT_ANDROID_SDK = "tools.androidSdk";
    public static final String KEY_GRADLE_USER_HOME  = "tools.gradleUserHome";
    public static final String KEY_WINDOW_WIDTH      = "window.width";
    public static final String KEY_WINDOW_HEIGHT     = "window.height";
    public static final String KEY_WINDOW_X          = "window.x";
    public static final String KEY_WINDOW_Y          = "window.y";
    public static final String KEY_SPLIT_POS_H       = "window.splitPosH";
    public static final String KEY_SPLIT_POS_V       = "window.splitPosV";

    private IdePreferences() {
        try {
            File dir = new File(PREFS_DIR);
            if (!dir.exists()) dir.mkdirs();
            File f = new File(PREFS_FILE);
            if (f.exists()) {
                props.load(new FileReader(f));
            } else {
                setDefaults();
            }
        } catch (IOException e) {
            setDefaults();
        }
    }

    private void setDefaults() {
        props.setProperty(KEY_FONT_SIZE, "13");
        props.setProperty(KEY_TAB_SIZE, "4");
        props.setProperty(KEY_SHOW_LINE_NUMS, "true");
        props.setProperty(KEY_WORD_WRAP, "false");
        props.setProperty(KEY_WINDOW_WIDTH, "1280");
        props.setProperty(KEY_WINDOW_HEIGHT, "800");
        props.setProperty(KEY_SPLIT_POS_H, "250");
        props.setProperty(KEY_SPLIT_POS_V, "600");
        props.setProperty(KEY_RECENT_PROJECTS, "");
        props.setProperty(KEY_DEFAULT_ANDROID_SDK, detectAndroidSdk());
        props.setProperty(KEY_DEFAULT_GRADLE, "");
        props.setProperty(KEY_DEFAULT_JDK, System.getProperty("java.home", ""));
    }

    private String detectAndroidSdk() {
        String[] envVars = {"ANDROID_HOME", "ANDROID_SDK_ROOT"};
        for (String v : envVars) {
            String val = System.getenv(v);
            if (val != null && !val.isEmpty() && new File(val).exists()) return val;
        }
        // Try common paths
        String home = System.getProperty("user.home");
        String[] candidates = {
            home + "/Android/Sdk",
            home + "/android/sdk",
            home + "/.android/sdk",
            "/opt/android-sdk",
            "C:\\Users\\" + System.getProperty("user.name") + "\\AppData\\Local\\Android\\Sdk",
        };
        for (String c : candidates) {
            if (new File(c).exists()) return c;
        }
        return "";
    }

    public static IdePreferences get() {
        if (instance == null) instance = new IdePreferences();
        return instance;
    }

    public String getString(String key, String def) {
        return props.getProperty(key, def);
    }

    public int getInt(String key, int def) {
        try { return Integer.parseInt(props.getProperty(key, String.valueOf(def))); }
        catch (NumberFormatException e) { return def; }
    }

    public boolean getBoolean(String key, boolean def) {
        String v = props.getProperty(key);
        if (v == null) return def;
        return "true".equalsIgnoreCase(v);
    }

    public void set(String key, String value) {
        props.setProperty(key, value);
    }

    public void set(String key, int value) {
        props.setProperty(key, String.valueOf(value));
    }

    public void set(String key, boolean value) {
        props.setProperty(key, String.valueOf(value));
    }

    public void save() {
        try {
            File dir = new File(PREFS_DIR);
            if (!dir.exists()) dir.mkdirs();
            props.store(new FileWriter(PREFS_FILE), "SabreTooth IDE Preferences");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Recent projects as list
    public List<String> getRecentProjects() {
        String raw = getString(KEY_RECENT_PROJECTS, "");
        if (raw.isEmpty()) return new ArrayList<>();
        String[] parts = raw.split("\\|");
        List<String> list = new ArrayList<>();
        for (String p : parts) {
            p = p.trim();
            if (!p.isEmpty() && new File(p).exists()) list.add(p);
        }
        return list;
    }

    public void addRecentProject(String path) {
        List<String> recent = getRecentProjects();
        recent.remove(path);
        recent.add(0, path);
        if (recent.size() > 10) recent = recent.subList(0, 10);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < recent.size(); i++) {
            if (i > 0) sb.append("|");
            sb.append(recent.get(i));
        }
        set(KEY_RECENT_PROJECTS, sb.toString());
    }

    public String getPrefsDir() { return PREFS_DIR; }
}
