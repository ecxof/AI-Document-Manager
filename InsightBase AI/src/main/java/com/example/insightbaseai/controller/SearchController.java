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

import java.util.List;

public class SearchController {
    
    private static final LoggerUtil logger = LoggerUtil.getInstance();
    
    // FXML Controls
    @FXML private TextField searchField;
    @FXML private Button searchButton;
    @FXML private Button clearButton;
    
    @FXML private TableView<DocumentEntry> resultsTable;
    @FXML private TableColumn<DocumentEntry, String> nameColumn;
    @FXML private TableColumn<DocumentEntry, String> typeColumn;
    @FXML private TableColumn<DocumentEntry, String> sizeColumn;
    @FXML private TableColumn<DocumentEntry, String> relevanceColumn;
    
    @FXML private TextArea selectedDocumentContent;
    @FXML private Label resultsCountLabel;
    @FXML private Label searchStatusLabel;
    @FXML private ProgressBar searchProgressBar;
    
    @FXML private ComboBox<String> searchModeCombo;
    @FXML private CheckBox caseSensitiveCheck;
    @FXML private CheckBox wholeWordsCheck;
    
    // Dependencies
    private AIService aiService;
    private ObservableList<DocumentEntry> searchResults;
    
    public SearchController() {
        this.searchResults = FXCollections.observableArrayList();
    }
    
    public void setAIService(AIService aiService) {
        this.aiService = aiService;
    }
    
    @FXML
    public void initialize() {
        // Initialize AIService with singleton
        this.aiService = AIService.getInstance();
        
        setupTableColumns();
        setupEventHandlers();
        setupSearchOptions();
        
        resultsTable.setItems(searchResults);
        
        logger.logUserAction("Search Panel", "Opened");
    }
    
    private void setupTableColumns() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("fileName"));
        typeColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getType().toString()));
        sizeColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getFormattedFileSize()));
        relevanceColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty("High")); // Placeholder for relevance scoring
    }
    
    private void setupEventHandlers() {
        // Search on Enter key
        searchField.setOnAction(e -> handleSearch());
        
        // Table selection handler
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
            "Full Text Search"
        );
        searchModeCombo.setValue("Content Search");
    }
    
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
                updateProgress(-1, -1); // Indeterminate progress
                
                Thread.sleep(500); // Simulate search delay
                
                String searchMode = searchModeCombo.getValue();
                boolean caseSensitive = caseSensitiveCheck.isSelected();
                boolean wholeWords = wholeWordsCheck.isSelected();
                
                return performActualSearch(query, searchMode, caseSensitive, wholeWords);
            }
            
            @Override
            protected void succeeded() {
                List<DocumentEntry> results = getValue();
                searchResults.clear();
                if (results != null) {
                    searchResults.addAll(results);
                }
                
                updateResultsDisplay(results);
                searchProgressBar.setVisible(false);
                
                logger.logUserAction("Search", "Query: '" + query + "', Results: " + 
                    (results != null ? results.size() : 0));
            }
            
            @Override
            protected void failed() {
                searchStatusLabel.setText("Search failed: " + getException().getMessage());
                searchProgressBar.setVisible(false);
                logger.error("Search failed for query: " + query, getException());
            }
        };
        
        // Bind progress and status
        searchProgressBar.progressProperty().bind(searchTask.progressProperty());
        searchStatusLabel.textProperty().bind(searchTask.messageProperty());
        searchProgressBar.setVisible(true);
        
        // Disable search button during search
        searchButton.setDisable(true);
        searchTask.setOnSucceeded(e -> searchButton.setDisable(false));
        searchTask.setOnFailed(e -> searchButton.setDisable(false));
        
        // Run search
        Thread searchThread = new Thread(searchTask);
        searchThread.setDaemon(true);
        searchThread.start();
    }
    
    private List<DocumentEntry> performActualSearch(String query, String searchMode, 
                                                   boolean caseSensitive, boolean wholeWords) {
        
        List<DocumentEntry> allDocuments = aiService.getKnowledgeBase().getAllDocuments();
        
        String searchQuery = caseSensitive ? query : query.toLowerCase();
        
        return allDocuments.stream()
            .filter(doc -> matchesSearchCriteria(doc, searchQuery, searchMode, caseSensitive, wholeWords))
            .toList();
    }
    
    private boolean matchesSearchCriteria(DocumentEntry document, String query, String searchMode,
                                        boolean caseSensitive, boolean wholeWords) {
        
        switch (searchMode) {
            case "Filename Search":
                return matchesText(document.getFileName(), query, caseSensitive, wholeWords);
                
            case "Content Search":
                if (document.getContent() == null) return false;
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
        if (text == null || text.isEmpty()) return false;
        
        String searchText = caseSensitive ? text : text.toLowerCase();
        
        if (wholeWords) {
            // Use word boundaries for whole word matching
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
            
            // Highlight search terms in the content
            String content = document.getContent();
            String query = searchField.getText().trim();
            
            if (!query.isEmpty() && !caseSensitiveCheck.isSelected()) {
                // Simple highlighting (in a real app, you'd use a more sophisticated approach)
                content = highlightSearchTerms(content, query);
            }
            
            selectedDocumentContent.setText(content);
        } else {
            selectedDocumentContent.setText("No content available for this document.");
        }
    }
    
    private String highlightSearchTerms(String content, String query) {
        // Simple highlighting - replace with the query in uppercase
        // In a real application, you'd use proper text highlighting
        if (!caseSensitiveCheck.isSelected()) {
            return content.replaceAll("(?i)" + java.util.regex.Pattern.quote(query), 
                                    "**" + query.toUpperCase() + "**");
        }
        return content;
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
    
    @FXML
    private void handleAdvancedSearch() {
        // Placeholder for advanced search dialog
        Alert advancedDialog = new Alert(Alert.AlertType.INFORMATION);
        advancedDialog.setTitle("Advanced Search");
        advancedDialog.setHeaderText("Advanced Search Options");
        advancedDialog.setContentText("Advanced search features coming soon!\n\n" +
            "Future features will include:\n" +
            "• Date range filtering\n" +
            "• File type filtering\n" +
            "• Size-based filtering\n" +
            "• Semantic similarity search\n" +
            "• Boolean operators");
        
        advancedDialog.showAndWait();
    }
    
    public void performQuickSearch(String query) {
        searchField.setText(query);
        handleSearch();
    }
    
    // Called when this view becomes active
    public void onViewActivated() {
        searchField.requestFocus();
    }
}