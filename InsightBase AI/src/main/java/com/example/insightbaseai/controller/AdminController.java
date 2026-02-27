package com.example.insightbaseai.controller;

import com.example.insightbaseai.model.DocumentEntry;
import com.example.insightbaseai.model.KnowledgeBase;
import com.example.insightbaseai.service.AIService;
import com.example.insightbaseai.util.FileUtils;
import com.example.insightbaseai.util.LoggerUtil;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;
import java.util.Optional;

public class AdminController {

    private static final LoggerUtil logger = LoggerUtil.getInstance();

    // FXML Controls
    @FXML
    private TableView<DocumentEntry> documentsTable;
    @FXML
    private TableColumn<DocumentEntry, String> nameColumn;
    @FXML
    private TableColumn<DocumentEntry, String> typeColumn;
    @FXML
    private TableColumn<DocumentEntry, String> sizeColumn;
    @FXML
    private TableColumn<DocumentEntry, String> statusColumn;
    @FXML
    private TableColumn<DocumentEntry, String> dateColumn;

    @FXML
    private Button uploadButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Button refreshButton;
    @FXML
    private Button viewButton;

    @FXML
    private Label totalDocumentsLabel;
    @FXML
    private Label indexedDocumentsLabel;
    @FXML
    private Label totalSizeLabel;
    @FXML
    private Label statusLabel;

    @FXML
    private TextArea selectedDocumentPreview;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> categoryFilter;
    @FXML
    private ProgressBar progressBar;

    // Dependencies
    private AIService aiService;
    private ObservableList<DocumentEntry> documentList;

    public AdminController() {
        this.documentList = FXCollections.observableArrayList();
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
        setupFilters();

        // Initialize with empty data
        documentsTable.setItems(documentList);

        logger.logUserAction("Admin Panel", "Opened");
    }

    private void setupTableColumns() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("fileName"));
        typeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getType().toString()));
        sizeColumn
                .setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFormattedFileSize()));
        statusColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(cellData.getValue().isIndexed() ? "Indexed" : "Pending"));
        dateColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(cellData.getValue().getUploadedAt().toString()));
    }

    private void setupEventHandlers() {
        // Table selection handler
        documentsTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        showDocumentPreview(newSelection);
                        deleteButton.setDisable(false);
                        viewButton.setDisable(false);
                    } else {
                        selectedDocumentPreview.clear();
                        deleteButton.setDisable(true);
                        viewButton.setDisable(true);
                    }
                });

        // Search field handler
        searchField.textProperty().addListener((obs, oldText, newText) -> {
            filterDocuments();
        });
    }

    private void setupFilters() {
        categoryFilter.getItems().addAll("All Categories", "PDF", "DOCX", "DOC", "TXT");
        categoryFilter.setValue("All Categories");

        categoryFilter.setOnAction(e -> filterDocuments());
    }

    @FXML
    private void handleUpload() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Documents to Upload");

        // Set file extension filters
        FileChooser.ExtensionFilter txtFilter = new FileChooser.ExtensionFilter("Text Files (*.txt)", "*.txt");
        FileChooser.ExtensionFilter allFilter = new FileChooser.ExtensionFilter("All Supported Files", "*.txt");

        fileChooser.getExtensionFilters().addAll(allFilter, txtFilter);
        fileChooser.setSelectedExtensionFilter(allFilter);

        // Show file dialog
        Stage stage = (Stage) uploadButton.getScene().getWindow();
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(stage);

        if (selectedFiles != null && !selectedFiles.isEmpty()) {
            uploadDocuments(selectedFiles);
        }
    }

    private void uploadDocuments(List<File> files) {
        Task<Void> uploadTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                int totalFiles = files.size();
                int processed = 0;

                for (File file : files) {
                    updateProgress(processed, totalFiles);
                    updateMessage("Processing: " + file.getName());

                    try {
                        // Validate file
                        FileUtils.validateFile(file);

                        // Create document entry
                        DocumentEntry document = FileUtils.createDocumentEntry(file);

                        // Check for duplicates
                        if (isDuplicateDocument(document)) {
                            updateMessage("Skipped duplicate: " + file.getName());
                            continue;
                        }

                        // Add to AI service
                        aiService.addDocument(document);

                        // Update UI on JavaFX thread
                        javafx.application.Platform.runLater(() -> {
                            documentList.add(document);
                            updateStatistics();
                        });

                        logger.logDocumentProcessing(file.getName(), "Uploaded");

                    } catch (Exception e) {
                        logger.error("Failed to upload file: " + file.getName(), e);
                        javafx.application.Platform.runLater(
                                () -> showError("Failed to upload " + file.getName() + ": " + e.getMessage()));
                    }

                    processed++;
                }

                return null;
            }

            @Override
            protected void succeeded() {
                javafx.application.Platform.runLater(() -> {
                    progressBar.setVisible(false);
                    statusLabel.setText("Upload completed successfully");
                    refreshDocuments();
                });
            }

            @Override
            protected void failed() {
                javafx.application.Platform.runLater(() -> {
                    progressBar.setVisible(false);
                    statusLabel.setText("Upload failed");
                    showError("Upload operation failed: " + getException().getMessage());
                });
            }
        };

        // Bind progress
        progressBar.progressProperty().bind(uploadTask.progressProperty());
        statusLabel.textProperty().bind(uploadTask.messageProperty());
        progressBar.setVisible(true);

        // Run task
        Thread uploadThread = new Thread(uploadTask);
        uploadThread.setDaemon(true);
        uploadThread.start();
    }

    private boolean isDuplicateDocument(DocumentEntry newDocument) {
        if (aiService == null)
            return false;

        KnowledgeBase kb = aiService.getKnowledgeBase();
        return kb.getAllDocuments().stream()
                .anyMatch(existing -> existing.getHash().equals(newDocument.getHash()));
    }

    @FXML
    private void handleDelete() {
        DocumentEntry selected = documentsTable.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;

        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirm Delete");
        confirmDialog.setHeaderText("Delete Document");
        confirmDialog.setContentText("Are you sure you want to delete '" + selected.getFileName() + "'?");

        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                aiService.removeDocument(selected.getId());
                documentList.remove(selected);
                updateStatistics();

                statusLabel.setText("Document deleted: " + selected.getFileName());
                logger.logDocumentProcessing(selected.getFileName(), "Deleted");

            } catch (Exception e) {
                logger.error("Failed to delete document", e);
                showError("Failed to delete document: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleView() {
        DocumentEntry selected = documentsTable.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;

        // Create document viewer dialog
        Dialog<Void> viewDialog = new Dialog<>();
        viewDialog.setTitle("Document Viewer - " + selected.getFileName());
        viewDialog.setHeaderText("Document Content");

        TextArea contentArea = new TextArea(selected.getContent());
        contentArea.setEditable(false);
        contentArea.setPrefSize(600, 400);
        contentArea.setWrapText(true);

        viewDialog.getDialogPane().setContent(contentArea);
        viewDialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        viewDialog.showAndWait();
    }

    @FXML
    private void handleRefresh() {
        refreshDocuments();
        statusLabel.setText("Documents refreshed");
    }

    private void refreshDocuments() {
        if (aiService == null)
            return;

        documentList.clear();
        KnowledgeBase kb = aiService.getKnowledgeBase();
        documentList.addAll(kb.getAllDocuments());
        updateStatistics();

        logger.logUserAction("Refresh", "Document list refreshed");
    }

    private void filterDocuments() {
        if (aiService == null)
            return;

        String searchText = searchField.getText().toLowerCase().trim();
        String selectedCategory = categoryFilter.getValue();

        List<DocumentEntry> allDocuments = aiService.getKnowledgeBase().getAllDocuments();
        List<DocumentEntry> filtered = allDocuments.stream()
                .filter(doc -> {
                    // Category filter
                    if (!"All Categories".equals(selectedCategory)) {
                        if (!doc.getType().toString().equalsIgnoreCase(selectedCategory)) {
                            return false;
                        }
                    }

                    // Text search
                    if (!searchText.isEmpty()) {
                        return doc.getFileName().toLowerCase().contains(searchText) ||
                                doc.getTitle().toLowerCase().contains(searchText) ||
                                (doc.getContent() != null && doc.getContent().toLowerCase().contains(searchText));
                    }

                    return true;
                })
                .toList();

        documentList.clear();
        documentList.addAll(filtered);
    }

    private void showDocumentPreview(DocumentEntry document) {
        if (document.getContent() != null) {
            String preview = document.getContent();
            if (preview.length() > 1000) {
                preview = preview.substring(0, 1000) + "...";
            }
            selectedDocumentPreview.setText(preview);
        } else {
            selectedDocumentPreview.setText("No content available");
        }
    }

    private void updateStatistics() {
        if (aiService == null)
            return;

        KnowledgeBase kb = aiService.getKnowledgeBase();
        var stats = kb.getDetailedStatistics();

        totalDocumentsLabel.setText(String.valueOf(stats.get("totalDocuments")));
        indexedDocumentsLabel.setText(String.valueOf(stats.get("indexedDocuments")));

        int totalSizeKB = (Integer) stats.get("totalSizeKB");
        totalSizeLabel.setText(FileUtils.formatFileSize(totalSizeKB * 1024L));
    }

    private void showError(String message) {
        Alert errorAlert = new Alert(Alert.AlertType.ERROR);
        errorAlert.setTitle("Error");
        errorAlert.setHeaderText("An error occurred");
        errorAlert.setContentText(message);
        errorAlert.showAndWait();
    }

    // Called when this view becomes active
    public void onViewActivated() {
        refreshDocuments();
    }
}