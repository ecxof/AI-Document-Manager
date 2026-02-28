package com.example.insightbaseai.controller;

import com.example.insightbaseai.model.DocumentEntry;
import com.example.insightbaseai.service.AIService;
import com.example.insightbaseai.util.LoggerUtil;
import com.example.insightbaseai.util.ValidationUtil;

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
import java.util.ArrayList;
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
    private String activeFileType = "All Types";
    private String activeStatus = "All Statuses";
    private long activeMinBytes = 0;
    private long activeMaxBytes = Long.MAX_VALUE;

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
                .setCellValueFactory(cellData -> new SimpleStringProperty(computeRelevance(cellData.getValue())));
    }

    /**
     * Computes a simple relevance score based on how many keywords appear.
     */
    private String computeRelevance(DocumentEntry doc) {
        String query = searchField.getText();
        if (query == null || query.isBlank() || doc.getContent() == null)
            return "—";
        String content = doc.getContent().toLowerCase();
        String[] terms = query.toLowerCase().split("\\s+");
        int hits = 0;
        for (String term : terms) {
            int idx = 0;
            while ((idx = content.indexOf(term, idx)) != -1) {
                hits++;
                idx += term.length();
            }
        }
        if (hits == 0)
            return "Low";
        if (hits < 5)
            return "Medium";
        return "High";
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
                "Content Search",
                "Filename Search",
                "Full Text Search");
        searchModeCombo.setValue("Content Search");
    }

    private void setupFilterOptions() {
        fileTypeFilter.getItems().addAll(
                "All Types", "TXT", "PDF", "DOCX", "MD", "JAVA", "JSON", "XML", "YAML");
        fileTypeFilter.setValue("All Types");

        statusFilter.getItems().addAll(
                "All Statuses", "INDEXED", "NOT INDEXED");
        statusFilter.setValue("All Statuses");
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
        Task<List<DocumentEntry>> searchTask = new Task<List<DocumentEntry>>() {
            @Override
            protected List<DocumentEntry> call() throws Exception {
                updateMessage("Searching...");
                updateProgress(-1, -1);

                Thread.sleep(300);

                String searchMode = searchModeCombo.getValue();
                boolean caseSensitive = caseSensitiveCheck.isSelected();
                boolean wholeWords = wholeWordsCheck.isSelected();

                return performActualSearch(query, searchMode, caseSensitive, wholeWords);
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

    private List<DocumentEntry> performActualSearch(String query, String searchMode,
            boolean caseSensitive, boolean wholeWords) {

        List<DocumentEntry> allDocuments = aiService.getKnowledgeBase().getAllDocuments();
        String searchQuery = caseSensitive ? query : query.toLowerCase();

        List<DocumentEntry> results = new ArrayList<>();
        for (DocumentEntry doc : allDocuments) {
            // Text match
            if (!matchesSearchCriteria(doc, searchQuery, searchMode, caseSensitive, wholeWords))
                continue;
            // Advanced filters
            if (!matchesActiveFilters(doc))
                continue;
            results.add(doc);
        }
        return results;
    }

    // =========================================================================
    // Advanced Filter Handlers
    // =========================================================================

    @FXML
    private void handleApplyFilters() {
        activeFileType = fileTypeFilter.getValue() != null ? fileTypeFilter.getValue() : "All Types";
        activeStatus = statusFilter.getValue() != null ? statusFilter.getValue() : "All Statuses";

        try {
            String minStr = minSizeField.getText().trim();
            activeMinBytes = minStr.isEmpty() ? 0 : Long.parseLong(minStr) * 1024;
        } catch (NumberFormatException e) {
            activeMinBytes = 0;
        }

        try {
            String maxStr = maxSizeField.getText().trim();
            activeMaxBytes = maxStr.isEmpty() ? Long.MAX_VALUE : Long.parseLong(maxStr) * 1024;
        } catch (NumberFormatException e) {
            activeMaxBytes = Long.MAX_VALUE;
        }

        // Re-run search with filters applied
        String query = searchField.getText().trim();
        if (!query.isBlank()) {
            handleSearch();
        }

        searchStatusLabel.setText("Filters applied — " + describeFilters());
        logger.logUserAction("Search", "Advanced filters applied: " + describeFilters());
    }

    @FXML
    private void handleClearFilters() {
        fileTypeFilter.setValue("All Types");
        statusFilter.setValue("All Statuses");
        minSizeField.clear();
        maxSizeField.clear();

        activeFileType = "All Types";
        activeStatus = "All Statuses";
        activeMinBytes = 0;
        activeMaxBytes = Long.MAX_VALUE;

        searchStatusLabel.setText("Filters cleared");
        logger.logUserAction("Search", "Advanced filters cleared");
    }

    private boolean matchesActiveFilters(DocumentEntry doc) {
        // File type filter
        if (!"All Types".equals(activeFileType)) {
            String docType = doc.getType() != null ? doc.getType().toString().toUpperCase() : "";
            if (!docType.equalsIgnoreCase(activeFileType))
                return false;
        }

        // Status filter
        if (!"All Statuses".equals(activeStatus)) {
            boolean indexed = doc.isIndexed();
            if ("INDEXED".equals(activeStatus) && !indexed)
                return false;
            if ("NOT INDEXED".equals(activeStatus) && indexed)
                return false;
        }

        // Size filter
        long fileSizeBytes = doc.getFileSize();
        if (fileSizeBytes < activeMinBytes || fileSizeBytes > activeMaxBytes)
            return false;

        return true;
    }

    private String describeFilters() {
        StringBuilder sb = new StringBuilder();
        if (!"All Types".equals(activeFileType))
            sb.append("Type=").append(activeFileType).append(" ");
        if (!"All Statuses".equals(activeStatus))
            sb.append("Status=").append(activeStatus).append(" ");
        if (activeMinBytes > 0)
            sb.append("MinSize=").append(activeMinBytes / 1024).append("KB ");
        if (activeMaxBytes < Long.MAX_VALUE)
            sb.append("MaxSize=").append(activeMaxBytes / 1024).append("KB ");
        return sb.length() == 0 ? "none" : sb.toString().trim();
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
            // Write CSV header
            writer.println("File Name,Type,Size,Status,Relevance,Content Preview");

            // Write rows
            for (DocumentEntry doc : searchResults) {
                String name = escapeCsv(doc.getFileName());
                String type = escapeCsv(doc.getType() != null ? doc.getType().toString() : "");
                String size = escapeCsv(doc.getFormattedFileSize());
                String status = escapeCsv(doc.isIndexed() ? "INDEXED" : "NOT INDEXED");
                String relevance = escapeCsv(computeRelevance(doc));
                String preview = escapeCsv(doc.getContent() != null
                        ? doc.getContent().substring(0, Math.min(200, doc.getContent().length())).replace("\n", " ")
                        : "");

                writer.println(name + "," + type + "," + size + "," + status + "," + relevance + "," + preview);
            }

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

    private String escapeCsv(String value) {
        if (value == null)
            return "\"\"";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private boolean matchesSearchCriteria(DocumentEntry document, String query, String searchMode,
            boolean caseSensitive, boolean wholeWords) {
        switch (searchMode) {
            case "Filename Search":
                return matchesText(document.getFileName(), query, caseSensitive, wholeWords);

            case "Content Search":
                if (document.getContent() == null)
                    return false;
                return matchesText(document.getContent(), query, caseSensitive, wholeWords);

            case "Full Text Search":
            default:
                return matchesText(document.getFileName(), query, caseSensitive, wholeWords) ||
                        matchesText(document.getTitle(), query, caseSensitive, wholeWords) ||
                        (document.getContent() != null &&
                                matchesText(document.getContent(), query, caseSensitive, wholeWords));
        }
    }

    private boolean matchesText(String text, String query, boolean caseSensitive, boolean wholeWords) {
        if (text == null || text.isEmpty())
            return false;

        String searchText = caseSensitive ? text : text.toLowerCase();

        if (wholeWords) {
            String pattern = "\\b" + java.util.regex.Pattern.quote(query) + "\\b";
            int flags = caseSensitive ? 0 : java.util.regex.Pattern.CASE_INSENSITIVE;
            return java.util.regex.Pattern.compile(pattern, flags).matcher(searchText).find();
        } else {
            return searchText.contains(query);
        }
    }

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

    public void performQuickSearch(String query) {
        searchField.setText(query);
        handleSearch();
    }

    public void onViewActivated() {
        searchField.requestFocus();
    }
}