package com.example.aidocumentmanager;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.example.aidocumentmanager.common.LoggerUtil;
import com.example.aidocumentmanager.ui.DialogService;
import com.example.aidocumentmanager.config.ConfigurationManager;
import com.example.aidocumentmanager.ui.ThemeManager;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Initialize logging
        LoggerUtil.getInstance().logStartup();

        // Initialize error handler with primary stage
        DialogService.getInstance().setPrimaryStage(primaryStage);

        // Initialize configuration manager
        try {
            ConfigurationManager.getInstance();
            LoggerUtil.getInstance().info("Configuration manager initialized");
        } catch (Exception e) {
            DialogService.getInstance().handleException("Configuration Initialization", e);
        }

        try {
            // Load the main application window
            FXMLLoader loader = new FXMLLoader(getClass().getResource("ui/MainView.fxml"));

            // Load configuration for window size
            ConfigurationManager config = ConfigurationManager.getInstance();
            int width = config.getWindowWidth();
            int height = config.getWindowHeight();

            Scene scene = new Scene(loader.load(), width, height);

            // Apply CSS stylesheet
            String cssPath = getClass().getResource("ui/app.css").toExternalForm();
            scene.getStylesheets().add(cssPath);

            // Register scene with ThemeManager and apply saved theme
            ThemeManager.getInstance().setScene(scene);
            String savedTheme = config.getTheme(); // Use ConfigurationManager
            // Map "light" -> "Light", "dark" -> "Dark", "system" -> "System Default" if
            // needed
            // But let's check what ConfigurationManager uses
            ThemeManager.getInstance().applyTheme(normalizeThemeName(savedTheme));

            // Configure the primary stage
            primaryStage.setTitle("AI Document Manager - Intelligent Knowledge Management System");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);

            // Restore maximized state
            if (config.isWindowMaximized()) {
                primaryStage.setMaximized(true);
            }

            // Handle window state changes
            primaryStage.widthProperty().addListener((obs, oldVal, newVal) -> {
                if (!primaryStage.isMaximized()) {
                    config.setWindowWidth(newVal.intValue());
                }
            });

            primaryStage.heightProperty().addListener((obs, oldVal, newVal) -> {
                if (!primaryStage.isMaximized()) {
                    config.setWindowHeight(newVal.intValue());
                }
            });

            primaryStage.maximizedProperty().addListener((obs, oldVal, newVal) -> {
                config.setWindowMaximized(newVal);
            });

            // Handle application close
            primaryStage.setOnCloseRequest(e -> {
                try {
                    // Save configuration on close
                    config.saveConfiguration();
                    LoggerUtil.getInstance().logShutdown();
                } catch (Exception ex) {
                    LoggerUtil.getInstance().error("Error during application shutdown", ex);
                }
            });

            // Show the window
            primaryStage.show();

            LoggerUtil.getInstance().info("Application started successfully");

            // Validate configuration after startup
            if (!config.validateConfiguration()) {
                DialogService.getInstance().showWarningDialog("Configuration Warning",
                        "Some configuration settings may need attention.",
                        "Please check the Settings tab to ensure all values are correct.");
            }

        } catch (Exception e) {
            LoggerUtil.getInstance().error("Failed to start application", e);
            DialogService.getInstance().handleException("Application Startup", e);
            throw e;
        }
    }

    private String normalizeThemeName(String theme) {
        if (theme == null)
            return ThemeManager.THEME_LIGHT;
        return switch (theme.toLowerCase()) {
            case "dark" -> ThemeManager.THEME_DARK;
            case "system", "system default" -> ThemeManager.THEME_SYSTEM;
            default -> ThemeManager.THEME_LIGHT;
        };
    }

    public static void main(String[] args) {
        launch(args);
    }
}
