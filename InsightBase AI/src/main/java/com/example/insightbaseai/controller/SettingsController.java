package com.example.insightbaseai.controller;

import com.example.insightbaseai.service.AIService;
import com.example.insightbaseai.util.ConfigurationManager;
import com.example.insightbaseai.util.LoggerUtil;
import com.example.insightbaseai.util.ThemeManager;
import com.example.insightbaseai.util.ValidationUtil;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.collections.FXCollections;

import java.util.Map;

public class SettingsController {

    private static final LoggerUtil logger = LoggerUtil.getInstance();
    private static final ConfigurationManager config = ConfigurationManager.getInstance();

    // FXML Controls - API Configuration
    @FXML
    private ComboBox<String> providerCombo;
    @FXML
    private VBox openaiSettingsBox;
    @FXML
    private VBox huggingfaceSettingsBox;
    @FXML
    private PasswordField openaiApiKeyField;
    @FXML
    private ComboBox<String> modelSelectionCombo;
    @FXML
    private PasswordField hfApiKeyField;
    @FXML
    private TextField hfModelField;
    @FXML
    private Button testConnectionButton;
    @FXML
    private Label connectionStatusLabel;

    // FXML Controls - Application Settings
    @FXML
    private Slider maxChatHistorySlider;
    @FXML
    private Label maxChatHistoryLabel;
    @FXML
    private CheckBox enableLoggingCheck;
    @FXML
    private CheckBox autoSaveCheck;
    @FXML
    private ComboBox<String> themeCombo;

    // FXML Controls - Advanced Settings
    @FXML
    private Slider chunkSizeSlider;
    @FXML
    private Label chunkSizeLabel;
    @FXML
    private Slider overlapSlider;
    @FXML
    private Label overlapLabel;
    @FXML
    private Slider maxRetrievalResultsSlider;
    @FXML
    private Label maxRetrievalResultsLabel;

    // FXML Controls - Statistics and Info
    @FXML
    private VBox statisticsContainer;
    @FXML
    private TextArea systemInfoArea;

    // FXML Controls - Actions
    @FXML
    private Button saveButton;
    @FXML
    private Button resetButton;
    @FXML
    private Button exportSettingsButton;
    @FXML
    private Button clearDataButton;

    // Dependencies
    private AIService aiService;

    public void setAIService(AIService aiService) {
        this.aiService = aiService;
        updateStatistics();
    }

    @FXML
    public void initialize() {
        logger.info("Initializing SettingsController");

        // Initialize API Provider
        providerCombo.setItems(FXCollections.observableArrayList("OpenAI", "Hugging Face"));
        providerCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateProviderVisibility(newVal);
        });

        // Initialize themes
        themeCombo.setItems(FXCollections.observableArrayList("Light", "Dark", "System Default"));

        setupControls();
        setupEventHandlers();
        loadSettings();
        updateSystemInfo();

        logger.logUserAction("Settings Panel", "Opened");
    }

    private void setupControls() {
        // API Model options
        modelSelectionCombo.getItems().addAll(
                "gpt-4o-mini",
                "gpt-4o");
        modelSelectionCombo.setValue("gpt-4o-mini");

        // Theme options
        themeCombo.getItems().addAll(
                "Light",
                "Dark",
                "System Default");
        themeCombo.setValue("Light");

        // Slider configurations
        maxChatHistorySlider.setMin(5);
        maxChatHistorySlider.setMax(50);
        maxChatHistorySlider.setValue(10);
        maxChatHistorySlider.setMajorTickUnit(5);
        maxChatHistorySlider.setShowTickLabels(true);

        chunkSizeSlider.setMin(200);
        chunkSizeSlider.setMax(1000);
        chunkSizeSlider.setValue(500);
        chunkSizeSlider.setMajorTickUnit(100);
        chunkSizeSlider.setShowTickLabels(true);

        overlapSlider.setMin(50);
        overlapSlider.setMax(300);
        overlapSlider.setValue(100);
        overlapSlider.setMajorTickUnit(50);
        overlapSlider.setShowTickLabels(true);

        maxRetrievalResultsSlider.setMin(1);
        maxRetrievalResultsSlider.setMax(10);
        maxRetrievalResultsSlider.setValue(3);
        maxRetrievalResultsSlider.setMajorTickUnit(1);
        maxRetrievalResultsSlider.setShowTickLabels(true);
    }

    private void setupEventHandlers() {
        // Slider value change listeners
        maxChatHistorySlider.valueProperty()
                .addListener((obs, oldVal, newVal) -> maxChatHistoryLabel.setText(String.valueOf(newVal.intValue())));

        chunkSizeSlider.valueProperty().addListener(
                (obs, oldVal, newVal) -> chunkSizeLabel.setText(String.valueOf(newVal.intValue()) + " characters"));

        overlapSlider.valueProperty().addListener(
                (obs, oldVal, newVal) -> overlapLabel.setText(String.valueOf(newVal.intValue()) + " characters"));

        maxRetrievalResultsSlider.valueProperty().addListener((obs, oldVal, newVal) -> maxRetrievalResultsLabel
                .setText(String.valueOf(newVal.intValue()) + " results"));

        // API Key field listener
        openaiApiKeyField.textProperty().addListener((obs, oldText, newText) -> {
            if (ValidationUtil.isValidOpenAIApiKey(newText)) {
                openaiApiKeyField.setStyle("-fx-border-color: green;");
            } else if (!newText.isEmpty()) {
                openaiApiKeyField.setStyle("-fx-border-color: red;");
            } else {
                openaiApiKeyField.setStyle("");
            }
        });

        // Live theme preview whenever the combo value changes
        themeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                ThemeManager.getInstance().applyTheme(newVal);
            }
        });
    }

    @FXML
    private void handleTestConnection() {
        String provider = providerCombo.getValue();
        String apiKey;

        if ("OpenAI".equals(provider)) {
            apiKey = openaiApiKeyField.getText().trim();
            if (apiKey.isEmpty() || !ValidationUtil.isValidOpenAIApiKey(apiKey)) {
                connectionStatusLabel.setText("❌ Invalid OpenAI API key");
                connectionStatusLabel.setStyle("-fx-text-fill: red;");
                return;
            }
        } else {
            apiKey = hfApiKeyField.getText().trim();
            if (apiKey.isEmpty()) {
                connectionStatusLabel.setText("❌ Please enter HF API key");
                connectionStatusLabel.setStyle("-fx-text-fill: red;");
                return;
            }
        }

        // Simulate connection test
        testConnectionButton.setDisable(true);
        connectionStatusLabel.setText("🔄 Testing connection...");
        connectionStatusLabel.setStyle("-fx-text-fill: orange;");

        // In a real app, you would test the actual connection
        javafx.concurrent.Task<Boolean> testTask = new javafx.concurrent.Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                Thread.sleep(2000); // Simulate network delay
                return true; // Simulate successful connection
            }

            @Override
            protected void succeeded() {
                boolean success = getValue();
                if (success) {
                    connectionStatusLabel.setText("✅ Connection successful");
                    connectionStatusLabel.setStyle("-fx-text-fill: green;");
                } else {
                    connectionStatusLabel.setText("❌ Connection failed");
                    connectionStatusLabel.setStyle("-fx-text-fill: red;");
                }
                testConnectionButton.setDisable(false);
            }

            @Override
            protected void failed() {
                connectionStatusLabel.setText("❌ Connection test failed");
                connectionStatusLabel.setStyle("-fx-text-fill: red;");
                testConnectionButton.setDisable(false);
            }
        };

        Thread testThread = new Thread(testTask);
        testThread.setDaemon(true);
        testThread.start();
    }

    @FXML
    private void handleSave() {
        try {
            saveSettings();

            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
            successAlert.setTitle("Settings Saved");
            successAlert.setHeaderText(null);
            successAlert.setContentText("Settings have been saved successfully!");
            successAlert.showAndWait();

            logger.logUserAction("Settings", "Saved");

        } catch (Exception e) {
            logger.error("Failed to save settings", e);

            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("Save Error");
            errorAlert.setHeaderText("Failed to save settings");
            errorAlert.setContentText("An error occurred while saving settings: " + e.getMessage());
            errorAlert.showAndWait();
        }
    }

    @FXML
    private void handleReset() {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Reset Settings");
        confirmAlert.setHeaderText("Reset to Default Settings");
        confirmAlert.setContentText("This will reset all settings to their default values. Continue?");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                resetToDefaults();
                logger.logUserAction("Settings", "Reset to defaults");
            }
        });
    }

    @FXML
    private void handleExportSettings() {
        // Placeholder for export functionality
        Alert infoAlert = new Alert(Alert.AlertType.INFORMATION);
        infoAlert.setTitle("Export Settings");
        infoAlert.setHeaderText("Feature Coming Soon");
        infoAlert.setContentText("Settings export functionality will be available in a future version.");
        infoAlert.showAndWait();
    }

    @FXML
    private void handleClearData() {
        Alert warningAlert = new Alert(Alert.AlertType.WARNING);
        warningAlert.setTitle("Clear All Data");
        warningAlert.setHeaderText("⚠️ Warning: This action cannot be undone!");
        warningAlert.setContentText("This will permanently delete all uploaded documents and chat history. Continue?");

        warningAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmAlert.setTitle("Final Confirmation");
                confirmAlert.setHeaderText("Are you absolutely sure?");
                confirmAlert.setContentText("Type 'DELETE' to confirm data deletion:");

                TextInputDialog inputDialog = new TextInputDialog();
                inputDialog.setTitle("Confirm Deletion");
                inputDialog.setHeaderText("Type 'DELETE' to confirm:");
                inputDialog.setContentText("Confirmation:");

                inputDialog.showAndWait().ifPresent(input -> {
                    if ("DELETE".equals(input)) {
                        clearAllData();
                    }
                });
            }
        });
    }

    private void clearAllData() {
        try {
            if (aiService != null) {
                aiService.getKnowledgeBase().clear();
                aiService.clearChatHistory();
            }

            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
            successAlert.setTitle("Data Cleared");
            successAlert.setHeaderText(null);
            successAlert.setContentText("All data has been cleared successfully.");
            successAlert.showAndWait();

            updateStatistics();
            logger.logUserAction("Settings", "Cleared all data");

        } catch (Exception e) {
            logger.error("Failed to clear data", e);

            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("Clear Data Error");
            errorAlert.setHeaderText("Failed to clear data");
            errorAlert.setContentText("An error occurred: " + e.getMessage());
            errorAlert.showAndWait();
        }
    }

    private void saveSettings() {
        // Save settings to ConfigurationManager
        config.setAIProvider(providerCombo.getValue().toLowerCase().replace(" ", ""));
        config.setOpenAIApiKey(openaiApiKeyField.getText());
        String selectedModel = modelSelectionCombo.getValue();
        if (selectedModel == null || selectedModel.trim().isEmpty() || "gpt-3.5-turbo".equals(selectedModel.trim())) {
            selectedModel = "gpt-4o-mini";
        }
        config.setOpenAIModel(selectedModel);
        config.setHuggingFaceApiKey(hfApiKeyField.getText());
        String hfModel = hfModelField.getText();
        if (hfModel == null || hfModel.trim().isEmpty() || "gpt2".equalsIgnoreCase(hfModel.trim())) {
            hfModel = "meta-llama/Llama-3.1-8B-Instruct";
        }
        config.setHuggingFaceModel(hfModel);

        // Save application settings
        config.setMaxChatHistory((int) maxChatHistorySlider.getValue());
        config.setLoggingEnabled(enableLoggingCheck.isSelected());
        config.setAutoSaveEnabled(autoSaveCheck.isSelected());
        config.setTheme(themeCombo.getValue());

        // Save advanced settings
        config.setChunkSize((int) chunkSizeSlider.getValue());
        config.setChunkOverlap((int) overlapSlider.getValue());
        config.setMaxRetrievalResults((int) maxRetrievalResultsSlider.getValue());

        // Update environment variable for API key
        if (!openaiApiKeyField.getText().isEmpty()) {
            System.setProperty("OPENAI_API_KEY", openaiApiKeyField.getText());
        }

        // Refresh AIService configuration to pick up new API key
        AIService.getInstance().refreshConfiguration();
    }

    private void loadSettings() {
        // Load API settings
        String provider = config.getAIProvider();
        providerCombo.setValue(provider.equalsIgnoreCase("openai") ? "OpenAI" : "Hugging Face");
        updateProviderVisibility(providerCombo.getValue());

        openaiApiKeyField.setText(config.getOpenAIApiKey());
        String configuredModel = config.getOpenAIModel();
        if (!modelSelectionCombo.getItems().contains(configuredModel)) {
            configuredModel = "gpt-4o-mini";
            config.setOpenAIModel(configuredModel);
        }
        modelSelectionCombo.setValue(configuredModel);
        hfApiKeyField.setText(config.getHuggingFaceApiKey());
        hfModelField.setText(config.getHuggingFaceModel());

        // Load application settings
        maxChatHistorySlider.setValue(config.getMaxChatHistory());
        enableLoggingCheck.setSelected(config.isLoggingEnabled());
        autoSaveCheck.setSelected(config.isAutoSaveEnabled());
        themeCombo.setValue(config.getTheme());

        // Load advanced settings
        chunkSizeSlider.setValue(config.getChunkSize());
        overlapSlider.setValue(config.getChunkOverlap());
        maxRetrievalResultsSlider.setValue(config.getMaxRetrievalResults());

        // Check current API key from environment
        String envApiKey = System.getenv("OPENAI_API_KEY");
        if (envApiKey != null && !envApiKey.isEmpty() && openaiApiKeyField.getText().isEmpty()) {
            openaiApiKeyField.setText(envApiKey);
        }
    }

    private void resetToDefaults() {
        // Reset API settings
        openaiApiKeyField.clear();
        modelSelectionCombo.setValue("gpt-4o-mini");

        // Reset application settings
        maxChatHistorySlider.setValue(10);
        enableLoggingCheck.setSelected(true);
        autoSaveCheck.setSelected(true);
        themeCombo.setValue("Light");

        // Reset advanced settings
        chunkSizeSlider.setValue(500);
        overlapSlider.setValue(100);
        maxRetrievalResultsSlider.setValue(3);

        // Clear connection status
        connectionStatusLabel.setText("");
    }

    private void updateStatistics() {
        if (aiService == null)
            return;

        Map<String, Object> stats = aiService.getStatistics();

        StringBuilder statsText = new StringBuilder();
        statsText.append("Application Statistics:\n");
        statsText.append("• Total Queries: ").append(stats.get("totalQueries")).append("\n");
        statsText.append("• Total Documents: ").append(stats.get("totalDocuments")).append("\n");
        statsText.append("• Total Chunks: ").append(stats.get("totalChunks")).append("\n");
        statsText.append("• Chat History Size: ").append(stats.get("chatHistorySize")).append("\n");

        if (stats.get("averageResponseTime") != null) {
            double avgTime = (Double) stats.get("averageResponseTime");
            statsText.append("• Avg Response Time: ").append(String.format("%.2f ms", avgTime)).append("\n");
        }

        // Add to statistics display (if we have a text area for it)
        // For now, we'll just log it
        logger.info("Statistics updated: " + statsText.toString());
    }

    private void updateSystemInfo() {
        StringBuilder sysInfo = new StringBuilder();
        sysInfo.append("System Information:\n\n");
        sysInfo.append("Java Version: ").append(System.getProperty("java.version")).append("\n");
        sysInfo.append("Java Vendor: ").append(System.getProperty("java.vendor")).append("\n");
        sysInfo.append("OS Name: ").append(System.getProperty("os.name")).append("\n");
        sysInfo.append("OS Version: ").append(System.getProperty("os.version")).append("\n");
        sysInfo.append("Architecture: ").append(System.getProperty("os.arch")).append("\n");
        sysInfo.append("User Home: ").append(System.getProperty("user.home")).append("\n");
        sysInfo.append("Working Directory: ").append(System.getProperty("user.dir")).append("\n");

        // Memory information
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        sysInfo.append("\nMemory Usage:\n");
        sysInfo.append("• Used: ").append(formatBytes(usedMemory)).append("\n");
        sysInfo.append("• Free: ").append(formatBytes(freeMemory)).append("\n");
        sysInfo.append("• Total: ").append(formatBytes(totalMemory)).append("\n");
        sysInfo.append("• Max: ").append(formatBytes(maxMemory)).append("\n");

        if (systemInfoArea != null) {
            systemInfoArea.setText(sysInfo.toString());
        }
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024)
            return bytes + " B";
        if (bytes < 1024 * 1024)
            return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024)
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    private void updateProviderVisibility(String provider) {
        if ("OpenAI".equals(provider)) {
            openaiSettingsBox.setVisible(true);
            openaiSettingsBox.setManaged(true);
            huggingfaceSettingsBox.setVisible(false);
            huggingfaceSettingsBox.setManaged(false);
        } else {
            openaiSettingsBox.setVisible(false);
            openaiSettingsBox.setManaged(false);
            huggingfaceSettingsBox.setVisible(true);
            huggingfaceSettingsBox.setManaged(true);
        }
    }

    // Called when this view becomes active
    public void onViewActivated() {
        updateStatistics();
        updateSystemInfo();
    }
}
