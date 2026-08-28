package com.example.aidocumentmanager.ui;

import com.example.aidocumentmanager.config.ConfigurationManager;

import javafx.stage.Stage;

/**
 * Keeps the window's size and maximized state in the configuration, so the
 * application reopens the way the user left it.
 */
public class WindowStateBinder {

    /**
     * Restore the saved maximized state and start recording changes.
     *
     * <p>
     * Size is only recorded while the window is not maximized. A maximized
     * window reports the screen's dimensions, and saving those would make
     * un-maximizing restore to full screen size for good.
     */
    public static void bind(Stage stage, ConfigurationManager config) {
        if (config.isWindowMaximized()) {
            stage.setMaximized(true);
        }

        stage.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (!stage.isMaximized()) {
                config.setWindowWidth(newVal.intValue());
            }
        });

        stage.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (!stage.isMaximized()) {
                config.setWindowHeight(newVal.intValue());
            }
        });

        stage.maximizedProperty().addListener((obs, oldVal, newVal) -> {
            config.setWindowMaximized(newVal);
        });
    }
}
