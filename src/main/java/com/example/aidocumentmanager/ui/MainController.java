package com.example.aidocumentmanager.ui;

import com.example.aidocumentmanager.common.LoggerUtil;
import com.example.aidocumentmanager.ui.documents.DocumentsController;
import com.example.aidocumentmanager.ui.search.SearchController;
import com.example.aidocumentmanager.ui.settings.SettingsController;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Main Application Controller - Manages navigation between different views
 */
public class MainController {

    @FXML
    private BorderPane mainBorderPane;
    @FXML
    private TabPane mainTabPane;
    @FXML
    private MenuBar menuBar;
    @FXML
    private MenuItem exitMenuItem;
    @FXML
    private MenuItem aboutMenuItem;
    @FXML
    private MenuItem helpMenuItem;

    // Tab references
    @FXML
    private Tab chatTab;
    @FXML
    private Tab documentsTab;
    @FXML
    private Tab searchTab;
    @FXML
    private Tab settingsTab;

    // Controllers for each view. Held only for the views that need telling when
    // their tab is activated, or that need a dependency handed to them.
    private DocumentsController documentsController;
    private SearchController searchController;
    private SettingsController settingsController;

    // Cache loaded views to prevent reloading
    private final Map<String, Node> viewCache = new HashMap<>();

    @FXML
    private void initialize() {
        LoggerUtil.getInstance().log(LoggerUtil.LogLevel.INFO, "MainController",
                "Initializing main application window");

        setupTabs();
        setupMenus();
        loadInitialViews();

        // Set chat as the default active tab
        mainTabPane.getSelectionModel().select(chatTab);

        LoggerUtil.getInstance().log(LoggerUtil.LogLevel.INFO, "MainController",
                "Main application window initialized successfully");
    }

    /**
     * Setup tab change listeners and load views on demand
     */
    private void setupTabs() {
        mainTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null) {
                loadTabContent(newTab);
                notifyViewActivated(newTab);
                LoggerUtil.getInstance().log(LoggerUtil.LogLevel.INFO, "MainController",
                        "Switched to tab: " + newTab.getText());
            }
        });
    }

    private void notifyViewActivated(Tab tab) {
        if (tab == null)
            return;
        String tabId = tab.getId();
        switch (tabId) {
            case "documentsTab" -> {
                if (documentsController != null)
                    documentsController.onViewActivated();
            }
            case "searchTab" -> {
                if (searchController != null)
                    searchController.onViewActivated();
            }
            case "settingsTab" -> {
                if (settingsController != null)
                    settingsController.onViewActivated();
            }
            default -> {
                /* no-op for chat, which has nothing to do on activation */ }
        }
    }

    /**
     * Setup menu item actions
     */
    private void setupMenus() {
        exitMenuItem.setOnAction(e -> handleExit());
        aboutMenuItem.setOnAction(e -> handleAbout());
        helpMenuItem.setOnAction(e -> handleHelp());
    }

    /**
     * Load initial views (just chat for faster startup)
     */
    private void loadInitialViews() {
        loadTabContent(chatTab);
    }

    /**
     * Load content for the specified tab
     */
    private void loadTabContent(Tab tab) {
        try {
            String tabId = tab.getId();

            // Check if view is already cached
            if (viewCache.containsKey(tabId)) {
                tab.setContent(viewCache.get(tabId));
                return;
            }

            // Load the appropriate FXML based on tab
            Node content = null;
            switch (tabId) {
                case "chatTab":
                    content = loadChatView();
                    break;
                case "documentsTab":
                    content = loadDocumentsView();
                    break;
                case "searchTab":
                    content = loadSearchView();
                    break;
                case "settingsTab":
                    content = loadSettingsView();
                    break;
                default:
                    LoggerUtil.getInstance().log(LoggerUtil.LogLevel.ERROR, "MainController",
                            "Unknown tab ID: " + tabId);
                    return;
            }

            if (content != null) {
                viewCache.put(tabId, content);
                tab.setContent(content);
            }

        } catch (Exception e) {
            LoggerUtil.getInstance().log(LoggerUtil.LogLevel.ERROR, "MainController",
                    "Error loading tab content: " + e.toString());
            e.printStackTrace(); // Optional but helpful for console debugging
            showErrorDialog("Navigation Error", "Failed to load view: " + e.getMessage());
        }
    }

    /**
     * Load Chat View
     */
    private Node loadChatView() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("chat/ChatView.fxml"));
        return loader.load();
    }

    /**
     * Load Documents View
     */
    private Node loadDocumentsView() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("documents/DocumentsView.fxml"));
        Node content = loader.load();
        documentsController = loader.getController();
        return content;
    }

    /**
     * Load Search View
     */
    private Node loadSearchView() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("search/SearchView.fxml"));
        Node content = loader.load();
        searchController = loader.getController();
        return content;
    }

    /**
     * Load Settings View
     */
    private Node loadSettingsView() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("settings/SettingsView.fxml"));
        Node content = loader.load();
        settingsController = loader.getController();
        settingsController.setAIService(com.example.aidocumentmanager.ai.AIService.getInstance());
        return content;
    }

    /**
     * Handle application exit
     */
    @FXML
    private void handleExit() {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Exit Application");
        confirmDialog.setHeaderText("Are you sure you want to exit AI Document Manager?");
        confirmDialog.setContentText("Any unsaved changes will be lost.");

        if (confirmDialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            LoggerUtil.getInstance().log(LoggerUtil.LogLevel.INFO, "MainController",
                    "Application exit requested by user");
            Platform.exit();
        }
    }

    /**
     * Handle About menu item
     */
    @FXML
    private void handleAbout() {
        Alert aboutDialog = new Alert(Alert.AlertType.INFORMATION);
        aboutDialog.setTitle("About AI Document Manager");
        aboutDialog.setHeaderText("AI Document Manager v1.0");
        aboutDialog.setContentText(
                "An intelligent knowledge management system powered by AI.\n\n" +
                        "Features:\n" +
                        "• Document management and processing\n" +
                        "• AI-powered chat interface\n" +
                        "• Advanced search capabilities\n" +
                        "• Knowledge base organization\n\n" +
                        "Built with JavaFX and LangChain4j\n" +
                        "© 2024 AI Document Manager Project");
        aboutDialog.showAndWait();
    }

    /**
     * Handle Help menu item
     */
    @FXML
    private void handleHelp() {
        Alert helpDialog = new Alert(Alert.AlertType.INFORMATION);
        helpDialog.setTitle("AI Document Manager - Help");
        helpDialog.setHeaderText("How to use AI Document Manager");
        helpDialog.setContentText(
                "Getting Started:\n\n" +
                        "1. SETTINGS TAB:\n" +
                        "   • Configure your OpenAI API key\n" +
                        "   • Adjust AI model settings\n" +
                        "   • Set application preferences\n\n" +
                        "2. ADMIN TAB:\n" +
                        "   • Upload and manage documents\n" +
                        "   • View knowledge base statistics\n" +
                        "   • Organize document collections\n\n" +
                        "3. CHAT TAB:\n" +
                        "   • Ask questions about your documents\n" +
                        "   • Get AI-powered responses\n" +
                        "   • View conversation history\n\n" +
                        "4. SEARCH TAB:\n" +
                        "   • Search across all documents\n" +
                        "   • Filter by document type\n" +
                        "   • Export search results\n\n" +
                        "For more help, check the README.md file in the project directory.");
        helpDialog.showAndWait();
    }

    /**
     * Navigate to specific tab programmatically
     */
    public void navigateToTab(String tabId) {
        Tab targetTab = switch (tabId.toLowerCase()) {
            case "chat" -> chatTab;
            case "documents" -> documentsTab;
            case "search" -> searchTab;
            case "settings" -> settingsTab;
            default -> null;
        };

        if (targetTab != null) {
            mainTabPane.getSelectionModel().select(targetTab);
            LoggerUtil.getInstance().log(LoggerUtil.LogLevel.INFO, "MainController", "Navigated to tab: " + tabId);
        }
    }

    /**
     * Refresh all views (useful after settings changes)
     */
    public void refreshAllViews() {
        viewCache.clear();
        Tab currentTab = mainTabPane.getSelectionModel().getSelectedItem();
        if (currentTab != null) {
            loadTabContent(currentTab);
        }
        LoggerUtil.getInstance().log(LoggerUtil.LogLevel.INFO, "MainController", "All views refreshed");
    }

    /**
     * Show error dialog
     */
    private void showErrorDialog(String title, String message) {
        Alert errorAlert = new Alert(Alert.AlertType.ERROR);
        errorAlert.setTitle(title);
        errorAlert.setHeaderText("An error occurred");
        errorAlert.setContentText(message);
        errorAlert.showAndWait();
    }

    // Menu action handlers
    @FXML
    private void handleNavigateToChat() {
        navigateToTab("chat");
    }

    @FXML
    private void handleNavigateToDocuments() {
        navigateToTab("documents");
    }

    @FXML
    private void handleNavigateToSearch() {
        navigateToTab("search");
    }

    @FXML
    private void handleNavigateToSettings() {
        navigateToTab("settings");
    }

    @FXML
    private void handleRefreshViews() {
        refreshAllViews();
    }
}