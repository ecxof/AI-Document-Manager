package com.example.aidocumentmanager.ui.search;

import com.example.aidocumentmanager.domain.DocumentEntry;
import com.example.aidocumentmanager.ai.AIService;
import com.example.aidocumentmanager.common.LoggerUtil;
import com.example.aidocumentmanager.common.ValidationUtil;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class SearchController {

    private static final LoggerUtil logger = LoggerUtil.getInstance();

    // --- FXML Controls ---
    @FXML
    private TextField searchField;
    @FXML
    private Button searchButton;
    @FXML
    private Button clearButton;

    @FXML
    private TableView<DocumentEntry> resultsTable;
    @FXML
    private TableColumn<DocumentEntry, String> nameColumn;
    @FXML
    private TableColumn<DocumentEntry, String> typeColumn;
    @FXML
    private TableColumn<DocumentEntry, String> sizeColumn;
    @FXML
    private TableColumn<DocumentEntry, String> relevanceColumn;
    @FXML
    private TableColumn<DocumentEntry, String> statusColumn;

    @FXML
    private TextArea selectedDocumentContent;
    @FXML
    private Label resultsCountLabel;
    @FXML
    private Label searchStatusLabel;
    @FXML
    private ProgressBar searchProgressBar;

    @FXML
    private ComboBox<String> searchModeCombo;
    @FXML
    private CheckBox caseSensitiveCheck;
    @FXML
    private CheckBox wholeWordsCheck;

    // Advanced filter controls
    @FXML
    private ComboBox<String> fileTypeFilter;
    @FXML
    private ComboBox<String> statusFilter;
    @FXML
    private TextField minSizeField;
    @FXML
    private TextField maxSizeField;

    // Dependencies
    private AIService aiService;
    private ObservableList<DocumentEntry> searchResults;

    // Filters state
    private DocumentFilter activeFilter = DocumentFilter.unfiltered();

    public SearchController() {
        this.searchResults = FXCollections.observableArrayList();
    }

    public void setAIService(AIService aiService) {
        this.aiService = aiService;
    }

    @FXML
    public void initialize() {
        this.aiService = AIService.getInstance();

        setupTableColumns();
        setupEventHandlers();
        setupSearchOptions();
        setupFilterOptions();

        resultsTable.setItems(searchResults);

        logger.logUserAction("Search Panel", "Opened");
    }

    private void setupTableColumns() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("fileName"));
        typeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getType().toString()));
        sizeColumn
                .setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFormattedFileSize()));
        statusColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(cellData.getValue().isIndexed() ? "INDEXED" : "NOT INDEXED"));
        relevanceColumn
                .setCellValueFactory(cellData -> new SimpleStringProperty(DocumentSearch.relevance(cellData.getValue(), searchField.getText())));
    }

    private void setupEventHandlers() {
        searchField.setOnAction(e -> handleSearch());

        resultsTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        showDocumentContent(newSelection);
                    } else {
                        selectedDocumentContent.clear();
                    }
                });
    }

    private void setupSearchOptions() {
        searchModeCombo.getItems().addAll(
                DocumentSearch.CONTENT_SEARCH,
                DocumentSearch.FILENAME_SEARCH,
                DocumentSearch.FULL_TEXT_SEARCH);
        searchModeCombo.setValue(DocumentSearch.CONTENT_SEARCH);
    }

    private void setupFilterOptions() {
        fileTypeFilter.getItems().addAll(
                DocumentFilter.ALL_TYPES, "TXT", "PDF", "DOCX", "MD", "JAVA", "JSON", "XML", "YAML");
        fileTypeFilter.setValue(DocumentFilter.ALL_TYPES);

        statusFilter.getItems().addAll(
                DocumentFilter.ALL_STATUSES, "INDEXED", "NOT INDEXED");
        statusFilter.setValue(DocumentFilter.ALL_STATUSES);
    }

    // =========================================================================
    // Search Handlers
    // =========================================================================

    @FXML
    private void handleSearch() {
        String query = searchField.getText().trim();

        if (!ValidationUtil.isValidSearchQuery(query)) {
            searchStatusLabel.setText("Please enter a valid search query");
            return;
        }

        if (aiService == null) {
            searchStatusLabel.setText("AI Service not available");
            return;
        }

        performSearch(query);
    }

    private void performSearch(String query) {
        // Read JavaFX controls on the FX thread; the Task body runs on a background
        // thread and must not touch the scene graph.
        final DocumentSearch.Criteria criteria = new DocumentSearch.Criteria(
                query, searchModeCombo.getValue(), caseSensitiveCheck.isSelected(), wholeWordsCheck.isSelected());
        final DocumentFilter filter = activeFilter;
        final List<DocumentEntry> documents = aiService.getKnowledgeBase().getAllDocuments();

        Task<List<DocumentEntry>> searchTask = new Task<List<DocumentEntry>>() {
            @Override
            protected List<DocumentEntry> call() throws Exception {
                updateMessage("Searching...");
                updateProgress(-1, -1);

                Thread.sleep(300);

                return DocumentSearch.search(documents, criteria, filter);
            }

            @Override
            protected void succeeded() {
                searchStatusLabel.textProperty().unbind();
                searchProgressBar.progressProperty().unbind();

                List<DocumentEntry> results = getValue();
                searchResults.clear();
                if (results != null)
                    searchResults.addAll(results);

                updateResultsDisplay(results);
                searchProgressBar.setVisible(false);
                searchButton.setDisable(false);

                logger.logUserAction("Search", "Query: '" + query + "', Results: " +
                        (results != null ? results.size() : 0));
            }

            @Override
            protected void failed() {
                searchStatusLabel.textProperty().unbind();
                searchProgressBar.progressProperty().unbind();

                searchStatusLabel.setText("Search failed: " + getException().getMessage());
                searchProgressBar.setVisible(false);
                searchButton.setDisable(false);
                logger.error("Search failed for query: " + query, getException());
            }
        };

        searchProgressBar.progressProperty().bind(searchTask.progressProperty());
        searchStatusLabel.textProperty().bind(searchTask.messageProperty());
        searchProgressBar.setVisible(true);
        searchButton.setDisable(true);

        Thread searchThread = new Thread(searchTask);
        searchThread.setDaemon(true);
        searchThread.start();
    }

    // =========================================================================
    // Advanced Filter Handlers
    // =========================================================================

    @FXML
    private void handleApplyFilters() {
        activeFilter = new DocumentFilter(
                fileTypeFilter.getValue() != null ? fileTypeFilter.getValue() : DocumentFilter.ALL_TYPES,
                statusFilter.getValue() != null ? statusFilter.getValue() : DocumentFilter.ALL_STATUSES,
                parseSizeInBytes(minSizeField.getText(), 0),
                parseSizeInBytes(maxSizeField.getText(), Long.MAX_VALUE));

        // Re-run search with filters applied
        String query = searchField.getText().trim();
        if (!query.isBlank()) {
            handleSearch();
        }

        String described = activeFilter.describe();
        searchStatusLabel.setText("Filters applied: " + described);
        logger.logUserAction("Search", "Advanced filters applied: " + described);
    }

    /** The size fields are in KB; anything unparseable falls back to no bound. */
    private static long parseSizeInBytes(String text, long fallback) {
        try {
            String trimmed = text.trim();
            return trimmed.isEmpty() ? fallback : Long.parseLong(trimmed) * 1024;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @FXML
    private void handleClearFilters() {
        fileTypeFilter.setValue(DocumentFilter.ALL_TYPES);
        statusFilter.setValue(DocumentFilter.ALL_STATUSES);
        minSizeField.clear();
        maxSizeField.clear();

        activeFilter = DocumentFilter.unfiltered();

        searchStatusLabel.setText("Filters cleared");
        logger.logUserAction("Search", "Advanced filters cleared");
    }

    // =========================================================================
    // Export Handler
    // =========================================================================

    @FXML
    private void handleExportResults() {
        if (searchResults.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "No Results", "Nothing to export.",
                    "Please perform a search first to get results to export.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Search Results");
        fileChooser.setInitialFileName("search_results.csv");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"));

        File file = fileChooser.showSaveDialog(null);
        if (file == null)
            return;

        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            SearchResultsCsvWriter.write(writer, searchResults, searchField.getText().trim());

            searchStatusLabel.setText("Exported " + searchResults.size() + " results to: " + file.getName());
            logger.logUserAction("Search Export",
                    "Exported " + searchResults.size() + " results to: " + file.getAbsolutePath());

            showAlert(Alert.AlertType.INFORMATION, "Export Successful",
                    "Results exported successfully!",
                    "Saved to: " + file.getAbsolutePath());

        } catch (IOException e) {
            logger.error("Failed to export search results", e);
            showAlert(Alert.AlertType.ERROR, "Export Failed", "Could not export results.", e.getMessage());
        }
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private void updateResultsDisplay(List<DocumentEntry> results) {
        int count = results != null ? results.size() : 0;
        if (count == 0) {
            resultsCountLabel.setText("No results found");
            searchStatusLabel.setText("No documents match your search criteria");
        } else {
            resultsCountLabel.setText(count + (count == 1 ? " result" : " results") + " found");
            searchStatusLabel.setText("Search completed successfully");
        }
    }

    private void showDocumentContent(DocumentEntry document) {
        if (document.getContent() != null && !document.getContent().isEmpty()) {
            String content = document.getContent();
            String query = searchField.getText().trim();

            if (!query.isEmpty()) {
                content = highlightSearchTerms(content, query);
            }

            selectedDocumentContent.setText(content);
        } else {
            selectedDocumentContent.setText("No content available for this document.");
        }
    }

    private String highlightSearchTerms(String content, String query) {
        if (!caseSensitiveCheck.isSelected()) {
            return content.replaceAll("(?i)" + java.util.regex.Pattern.quote(query),
                    ">>> " + query.toUpperCase() + " <<<");
        }
        return content;
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    private void handleClear() {
        searchField.clear();
        searchResults.clear();
        selectedDocumentContent.clear();
        resultsCountLabel.setText("");
        searchStatusLabel.setText("Ready");

        logger.logUserAction("Search", "Cleared");
    }

    /**
     * Put the caret in the search box when the tab is opened.
     *
     * <p>
     * Deferred deliberately. This is called from the tab selection listener, and
     * the TabPane takes focus for itself once that listener returns, so
     * requesting focus directly here is silently undone. Running afterwards is
     * what makes it stick.
     */
    public void onViewActivated() {
        Platform.runLater(searchField::requestFocus);
    }
}