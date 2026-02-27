package com.example.insightbaseai.util;

import javafx.scene.Scene;

/**
 * ThemeManager - Applies light/dark/system themes at runtime by toggling
 * the "dark-mode" style class on the scene's root node.
 */
public class ThemeManager {

    private static ThemeManager instance;
    private Scene currentScene;

    public static final String THEME_LIGHT = "Light";
    public static final String THEME_DARK = "Dark";
    public static final String THEME_SYSTEM = "System Default";

    private ThemeManager() {
    }

    public static ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }

    /** Call this once after the main Scene is created. */
    public void setScene(Scene scene) {
        this.currentScene = scene;
    }

    /**
     * Apply the theme identified by the display name (e.g. "Dark", "Light",
     * "System Default"). Safe to call from the FX thread.
     */
    public void applyTheme(String themeName) {
        if (currentScene == null)
            return;

        var rootStyles = currentScene.getRoot().getStyleClass();

        switch (themeName) {
            case THEME_DARK -> {
                if (!rootStyles.contains("dark-mode")) {
                    rootStyles.add("dark-mode");
                }
            }
            case THEME_SYSTEM -> {
                // Honour the OS preference via the JavaFX platform preference API (JavaFX 20+).
                // Fall back to checking a system property as a heuristic for older runtimes.
                boolean preferDark = isOSDarkMode();
                if (preferDark) {
                    if (!rootStyles.contains("dark-mode"))
                        rootStyles.add("dark-mode");
                } else {
                    rootStyles.remove("dark-mode");
                }
            }
            default -> { // Light or anything else
                rootStyles.remove("dark-mode");
            }
        }
    }

    /** Best-effort detection of OS dark-mode preference. */
    private boolean isOSDarkMode() {
        // JavaFX 22+ can use Platform.Preferences. For older versions on Windows,
        // we can check the Registry for the "AppsUseLightTheme" value.
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                // Heuristic for Windows 10/11
                ProcessBuilder pb = new ProcessBuilder(
                        "reg", "query",
                        "HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize", "/v",
                        "AppsUseLightTheme");
                Process process = pb.start();
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains("AppsUseLightTheme") && line.contains("REG_DWORD") && line.contains("0x0")) {
                            return true; // 0x0 means dark mode
                        }
                    }
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
