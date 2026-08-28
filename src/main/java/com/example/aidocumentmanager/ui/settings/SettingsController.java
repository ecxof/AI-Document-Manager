package com.example.aidocumentmanager.ui.settings;

import com.example.aidocumentmanager.ai.AIService;
import com.example.aidocumentmanager.common.LoggerUtil;
import com.example.aidocumentmanager.common.ValidationUtil;
import com.example.aidocumentmanager.config.ConfigurationManager;
import com.example.aidocumentmanager.ui.ThemeManager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.collections.FXCollections;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    @FXML
    private TextArea systemInfoArea;

    // FXML Controls - Statistics
    @FXML
    private Label totalDocsLabel;
    @FXML
    private Label totalChunksLabel;
    @FXML
    private Label totalQueriesLabel;
    @FXML
    private Label avgResponseTimeLabel;
    @FXML
    private Label lastQueryTimeLabel;
    @FXML
    private Label historySizeLabel;

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
    private Slider similarityThresholdSlider;
    @FXML
    private Label similarityThresholdLabel;
    @FXML
    private Label maxRetrievalResultsLabel;

    // FXML Controls - Statistics and Info
    @FXML
    private VBox statisticsContainer;

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
        providerCombo.setItems(FXCollections.observableArrayList(
                SettingsForm.DISPLAY_OPENAI, SettingsForm.DISPLAY_HUGGINGFACE));
        providerCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateProviderVisibility(newVal);
        });

        setupControls();
        setupEventHandlers();
        loadSettings();
        updateSystemInfo();

        logger.logUserAction("Settings Panel", "Opened");
    }

    private void setupControls() {
        // API Model options
        modelSelectionCombo.getItems().addAll(
                SettingsForm.DEFAULT_OPENAI_MODEL,
                "gpt-4o");
        modelSelectionCombo.setValue(SettingsForm.DEFAULT_OPENAI_MODEL);

        // Theme options. Populated once - filling the combo here as well as in
        // initialize() used to list every theme twice.
        themeCombo.setItems(FXCollections.observableArrayList(
                ThemeManager.THEME_LIGHT,
                ThemeManager.THEME_DARK,
                ThemeManager.THEME_SYSTEM));
        themeCombo.setValue(ThemeManager.THEME_SYSTEM);

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

        // The threshold is compared against langchain4j's rescaled relevance
        // score, (cosine + 1) / 2, so 0.5 means "cosine 0" and 1.0 is an exact
        // match. Below 0.5 nothing is ever rejected and above roughly 0.7
        // genuinely relevant chunks start dropping out, so the useful range is
        // narrow and the slider is bounded well inside it.
        similarityThresholdSlider.setMin(0.0);
        similarityThresholdSlider.setMax(0.9);
        similarityThresholdSlider.setValue(0.5);
        similarityThresholdSlider.setMajorTickUnit(0.1);
        similarityThresholdSlider.setBlockIncrement(0.05);
        similarityThresholdSlider.setShowTickLabels(true);
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

        similarityThresholdSlider.valueProperty().addListener((obs, oldVal, newVal) -> similarityThresholdLabel
                .setText(String.format("%.2f", newVal.doubleValue())));

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

        if (SettingsForm.DISPLAY_OPENAI.equals(provider)) {
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
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Settings");
        fileChooser.setInitialFileName("aidocumentmanager_settings_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".json");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON Files", "*.json"));

        File file = fileChooser.showSaveDialog(null);
        if (file == null)
            return;

        try {
            Map<String, Object> settingsMap = SettingsExporter.export(config,
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            mapper.writeValue(file, settingsMap);

            connectionStatusLabel.setText("Settings exported to: " + file.getName());
            logger.logUserAction("Settings Export", "Exported to: " + file.getAbsolutePath());

            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setTitle("Export Successful");
            ok.setHeaderText("Settings exported successfully!");
            ok.setContentText("Saved to: " + file.getAbsolutePath() +
                    "\n\nNote: API keys are redacted for security.");
            ok.showAndWait();

        } catch (Exception e) {
            logger.error("Failed to export settings", e);
            Alert err = new Alert(Alert.AlertType.ERROR);
            err.setTitle("Export Failed");
            err.setHeaderText("Could not export settings");
            err.setContentText(e.getMessage());
            err.showAndWait();
        }
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
        SettingsForm form = new SettingsForm(
                SettingsForm.providerKey(providerCombo.getValue()),
                openaiApiKeyField.getText(),
                modelSelectionCombo.getValue(),
                hfApiKeyField.getText(),
                hfModelField.getText(),
                (int) maxChatHistorySlider.getValue(),
                enableLoggingCheck.isSelected(),
                autoSaveCheck.isSelected(),
                themeCombo.getValue(),
                (int) chunkSizeSlider.getValue(),
                (int) overlapSlider.getValue(),
                (int) maxRetrievalResultsSlider.getValue(),
                similarityThresholdSlider.getValue()).normalized();

        form.applyTo(config);

        // Update environment variable for API key
        if (!openaiApiKeyField.getText().isEmpty()) {
            System.setProperty("OPENAI_API_KEY", openaiApiKeyField.getText());
        }

        // Refresh AIService configuration to pick up new API key
        AIService.getInstance().refreshConfiguration();
    }

    private void loadSettings() {
        SettingsForm form = SettingsForm.from(config);

        providerCombo.setValue(SettingsForm.providerDisplayName(form.provider()));
        updateProviderVisibility(providerCombo.getValue());

        openaiApiKeyField.setText(form.openAIApiKey());
        String configuredModel = form.openAIModel();
        if (!modelSelectionCombo.getItems().contains(configuredModel)) {
            configuredModel = SettingsForm.DEFAULT_OPENAI_MODEL;
            config.setOpenAIModel(configuredModel);
        }
        modelSelectionCombo.setValue(configuredModel);
        hfApiKeyField.setText(form.huggingFaceApiKey());
        hfModelField.setText(form.huggingFaceModel());

        maxChatHistorySlider.setValue(form.maxChatHistory());
        enableLoggingCheck.setSelected(form.loggingEnabled());
        autoSaveCheck.setSelected(form.autoSaveEnabled());
        themeCombo.setValue(form.theme());

        chunkSizeSlider.setValue(form.chunkSize());
        overlapSlider.setValue(form.chunkOverlap());
        maxRetrievalResultsSlider.setValue(form.maxRetrievalResults());
        similarityThresholdSlider.setValue(form.similarityThreshold());

        // Check current API key from environment
        String envApiKey = System.getenv("OPENAI_API_KEY");
        if (envApiKey != null && !envApiKey.isEmpty() && openaiApiKeyField.getText().isEmpty()) {
            openaiApiKeyField.setText(envApiKey);
        }
    }

    private void resetToDefaults() {
        SettingsForm defaults = SettingsForm.defaults();

        openaiApiKeyField.clear();
        modelSelectionCombo.setValue(defaults.openAIModel());

        maxChatHistorySlider.setValue(defaults.maxChatHistory());
        enableLoggingCheck.setSelected(defaults.loggingEnabled());
        autoSaveCheck.setSelected(defaults.autoSaveEnabled());
        themeCombo.setValue(defaults.theme());

        chunkSizeSlider.setValue(defaults.chunkSize());
        overlapSlider.setValue(defaults.chunkOverlap());
        maxRetrievalResultsSlider.setValue(defaults.maxRetrievalResults());
        similarityThresholdSlider.setValue(defaults.similarityThreshold());

        // Clear connection status
        connectionStatusLabel.setText("");
    }

    @FXML
    private void updateStatistics() {
        if (aiService == null)
            return;

        Map<String, Object> stats = aiService.getStatistics();
        if (stats == null)
            return;

        // Update UI Labels
        if (totalDocsLabel != null)
            totalDocsLabel.setText(String.valueOf(stats.getOrDefault("totalDocuments", 0)));
        if (totalChunksLabel != null)
            totalChunksLabel.setText(String.valueOf(stats.getOrDefault("totalChunks", 0)));
        if (totalQueriesLabel != null)
            totalQueriesLabel.setText(String.valueOf(stats.getOrDefault("totalQueries", 0)));

        if (avgResponseTimeLabel != null) {
            double avgTime = (Double) stats.getOrDefault("averageResponseTime", 0.0);
            avgResponseTimeLabel.setText(String.format("%.2f ms", avgTime));
        }

        if (lastQueryTimeLabel != null) {
            long lastTime = (Long) stats.getOrDefault("lastQueryTime", 0L);
            lastQueryTimeLabel.setText(lastTime + " ms");
        }

        if (historySizeLabel != null) {
            int histSize = (Integer) stats.getOrDefault("chatHistorySize", 0);
            historySizeLabel.setText(histSize + " messages");
        }

        logger.info("Statistics UI updated successfully from AIService data.");
    }

    private void updateSystemInfo() {
        if (systemInfoArea != null) {
            systemInfoArea.setText(SystemInfo.describe());
        }
    }

    private void updateProviderVisibility(String provider) {
        if (SettingsForm.DISPLAY_OPENAI.equals(provider)) {
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
