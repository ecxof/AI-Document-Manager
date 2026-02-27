package com.example.insightbaseai.util;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Comprehensive error handling utility for the InsightBase AI application
 * Provides user-friendly error dialogs, logging integration, and recovery mechanisms
 */
public class ErrorHandler {
    
    private static final LoggerUtil logger = LoggerUtil.getInstance();
    private static volatile ErrorHandler instance;
    private Stage primaryStage;
    
    private ErrorHandler() {}
    
    public static ErrorHandler getInstance() {
        if (instance == null) {
            synchronized (ErrorHandler.class) {
                if (instance == null) {
                    instance = new ErrorHandler();
                }
            }
        }
        return instance;
    }
    
    /**
     * Set the primary stage for modal dialogs
     */
    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }
    
    /**
     * Handle general exceptions with user-friendly messages
     */
    public void handleException(String context, Throwable exception) {
        handleException(context, exception, null, true);
    }
    
    /**
     * Handle exceptions with custom recovery action
     */
    public void handleException(String context, Throwable exception, Runnable recoveryAction) {
        handleException(context, exception, recoveryAction, true);
    }
    
    /**
     * Handle exceptions with optional user notification
     */
    public void handleException(String context, Throwable exception, Runnable recoveryAction, boolean showToUser) {
        // Always log the exception
        logger.error(String.format("Exception in %s: %s", context, exception.getMessage()), exception);
        
        if (showToUser) {
            Platform.runLater(() -> {
                showExceptionDialog(context, exception, recoveryAction);
            });
        }
        
        // Execute recovery action if provided
        if (recoveryAction != null) {
            try {
                recoveryAction.run();
            } catch (Exception recoveryException) {
                logger.error("Error during recovery action", recoveryException);
            }
        }
    }
    
    /**
     * Handle file operation errors
     */
    public void handleFileError(String fileName, String operation, Throwable exception) {
        String userMessage = generateFileErrorMessage(operation, exception);
        String context = String.format("File Operation: %s on %s", operation, fileName);
        
        logger.error(context + ": " + exception.getMessage(), exception);
        
        Platform.runLater(() -> {
            showErrorDialog("File Operation Error", userMessage, getFileErrorAdvice(operation));
        });
    }
    
    /**
     * Handle AI service errors
     */
    public void handleAIServiceError(String operation, Throwable exception) {
        String userMessage = generateAIServiceErrorMessage(operation, exception);
        String context = String.format("AI Service: %s", operation);
        
        logger.error(context + ": " + exception.getMessage(), exception);
        
        Platform.runLater(() -> {
            String advice = "Please check your API key configuration in Settings and ensure you have an active internet connection.";
            showErrorDialog("AI Service Error", userMessage, advice);
        });
    }
    
    /**
     * Handle validation errors
     */
    public void handleValidationError(String field, String errorMessage) {
        String context = String.format("Validation Error: %s", field);
        logger.warn(context + ": " + errorMessage);
        
        Platform.runLater(() -> {
            showWarningDialog("Input Validation", 
                String.format("Invalid input for %s: %s", field, errorMessage),
                "Please correct the input and try again.");
        });
    }
    
    /**
     * Handle network connectivity errors
     */
    public void handleNetworkError(String operation, Throwable exception) {
        String userMessage = "Network connection failed. Please check your internet connection and try again.";
        String context = String.format("Network Error: %s", operation);
        
        logger.error(context + ": " + exception.getMessage(), exception);
        
        Platform.runLater(() -> {
            showErrorDialog("Network Error", userMessage, 
                "Verify your internet connection and firewall settings. If the problem persists, try again later.");
        });
    }
    
    /**
     * Handle configuration errors
     */
    public void handleConfigurationError(String configItem, String errorMessage) {
        String userMessage = String.format("Configuration error: %s", errorMessage);
        String context = String.format("Configuration Error: %s", configItem);
        
        logger.error(context + ": " + errorMessage);
        
        Platform.runLater(() -> {
            showErrorDialog("Configuration Error", userMessage, 
                "Please check your settings in the Settings tab and ensure all required fields are properly configured.");
        });
    }
    
    /**
     * Show confirmation dialog for potentially destructive operations
     */
    public CompletableFuture<Boolean> showConfirmationDialog(String title, String message, String details) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        Platform.runLater(() -> {
            Alert alert = createAlert(Alert.AlertType.CONFIRMATION, title, message, details);
            Optional<ButtonType> result = alert.showAndWait();
            future.complete(result.isPresent() && result.get() == ButtonType.OK);
        });
        
        return future;
    }
    
    /**
     * Show information dialog
     */
    public void showInfoDialog(String title, String message) {
        Platform.runLater(() -> {
            showInfoDialog(title, message, null);
        });
    }
    
    /**
     * Show information dialog with details
     */
    public void showInfoDialog(String title, String message, String details) {
        Platform.runLater(() -> {
            Alert alert = createAlert(Alert.AlertType.INFORMATION, title, message, details);
            alert.showAndWait();
        });
    }
    
    /**
     * Show warning dialog
     */
    public void showWarningDialog(String title, String message, String advice) {
        Platform.runLater(() -> {
            Alert alert = createAlert(Alert.AlertType.WARNING, title, message, advice);
            alert.showAndWait();
        });
    }
    
    /**
     * Show error dialog
     */
    public void showErrorDialog(String title, String message, String advice) {
        Platform.runLater(() -> {
            Alert alert = createAlert(Alert.AlertType.ERROR, title, message, advice);
            alert.showAndWait();
        });
    }
    
    /**
     * Show detailed exception dialog with stack trace
     */
    private void showExceptionDialog(String context, Throwable exception, Runnable recoveryAction) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Application Error");
        alert.setHeaderText(String.format("An error occurred in %s", context));
        alert.setContentText(generateUserFriendlyMessage(exception));
        
        // Create expandable stack trace
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        String exceptionText = sw.toString();
        
        TextArea textArea = new TextArea(exceptionText);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);
        
        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(textArea, 0, 0);
        
        alert.getDialogPane().setExpandableContent(expContent);
        
        // Add recovery button if recovery action is provided
        if (recoveryAction != null) {
            ButtonType retryButton = new ButtonType("Retry");
            alert.getButtonTypes().add(retryButton);
            
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == retryButton) {
                try {
                    recoveryAction.run();
                } catch (Exception recoveryException) {
                    handleException("Recovery Action", recoveryException, null, false);
                }
            }
        } else {
            alert.showAndWait();
        }
        
        configureAlert(alert);
    }
    
    /**
     * Create a standard alert dialog
     */
    private Alert createAlert(Alert.AlertType type, String title, String message, String details) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        
        if (details != null && !details.trim().isEmpty()) {
            TextArea textArea = new TextArea(details);
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setMaxWidth(Double.MAX_VALUE);
            textArea.setMaxHeight(Double.MAX_VALUE);
            
            GridPane.setVgrow(textArea, Priority.ALWAYS);
            GridPane.setHgrow(textArea, Priority.ALWAYS);
            
            GridPane expContent = new GridPane();
            expContent.setMaxWidth(Double.MAX_VALUE);
            expContent.add(textArea, 0, 0);
            
            alert.getDialogPane().setExpandableContent(expContent);
        }
        
        configureAlert(alert);
        return alert;
    }
    
    /**
     * Configure alert dialog appearance and behavior
     */
    private void configureAlert(Alert alert) {
        if (primaryStage != null) {
            alert.initOwner(primaryStage);
        }
        
        alert.setResizable(true);
        alert.getDialogPane().setPrefWidth(500);
        alert.getDialogPane().setPrefHeight(200);
    }
    
    /**
     * Generate user-friendly error message from exception
     */
    private String generateUserFriendlyMessage(Throwable exception) {
        if (exception == null) {
            return "An unknown error occurred.";
        }
        
        String message = exception.getMessage();
        if (message == null || message.trim().isEmpty()) {
            message = exception.getClass().getSimpleName();
        }
        
        // Convert technical messages to user-friendly ones
        if (message.contains("ConnectException") || message.contains("UnknownHostException")) {
            return "Unable to connect to the service. Please check your internet connection.";
        } else if (message.contains("FileNotFoundException")) {
            return "The requested file could not be found.";
        } else if (message.contains("AccessDeniedException")) {
            return "Access denied. Please check file permissions.";
        } else if (message.contains("OutOfMemoryError")) {
            return "The application has run out of memory. Try closing other applications or processing smaller files.";
        } else if (message.contains("API key")) {
            return "There's an issue with your API key configuration. Please check your settings.";
        } else if (message.contains("Timeout")) {
            return "The operation timed out. Please try again.";
        }
        
        return message;
    }
    
    /**
     * Generate file error message
     */
    private String generateFileErrorMessage(String operation, Throwable exception) {
        String baseMessage = String.format("Failed to %s file", operation.toLowerCase());
        
        if (exception.getMessage() != null) {
            if (exception.getMessage().contains("AccessDenied")) {
                return baseMessage + ": Permission denied. Please check file permissions.";
            } else if (exception.getMessage().contains("FileNotFound")) {
                return baseMessage + ": File not found. The file may have been moved or deleted.";
            } else if (exception.getMessage().contains("No space left")) {
                return baseMessage + ": Insufficient disk space.";
            }
        }
        
        return baseMessage + ". " + generateUserFriendlyMessage(exception);
    }
    
    /**
     * Generate AI service error message
     */
    private String generateAIServiceErrorMessage(String operation, Throwable exception) {
        String baseMessage = String.format("AI service error during %s", operation.toLowerCase());
        
        if (exception.getMessage() != null) {
            String msg = exception.getMessage().toLowerCase();
            if (msg.contains("unauthorized") || msg.contains("invalid api key")) {
                return "Invalid API key. Please check your OpenAI API key in Settings.";
            } else if (msg.contains("quota") || msg.contains("limit")) {
                return "API quota exceeded. Please check your OpenAI usage limits.";
            } else if (msg.contains("model not found")) {
                return "The specified AI model is not available. Please select a different model in Settings.";
            } else if (msg.contains("timeout")) {
                return "AI service request timed out. Please try again.";
            }
        }
        
        return baseMessage + ". Please check your network connection and API configuration.";
    }
    
    /**
     * Get file error advice
     */
    private String getFileErrorAdvice(String operation) {
        return switch (operation.toLowerCase()) {
            case "read", "open" -> "Ensure the file exists, is not corrupted, and you have read permissions.";
            case "write", "save" -> "Check available disk space and write permissions for the target directory.";
            case "delete" -> "Ensure the file is not in use by another application and you have delete permissions.";
            case "upload" -> "Verify the file is a supported format and not larger than 50MB.";
            default -> "Please check the file status and try again.";
        };
    }
    
    /**
     * Execute operation with error handling
     */
    public <T> Optional<T> executeWithErrorHandling(String context, ThrowingSupplier<T> operation) {
        try {
            T result = operation.get();
            return Optional.ofNullable(result);
        } catch (Exception e) {
            handleException(context, e);
            return Optional.empty();
        }
    }
    
    /**
     * Execute operation with error handling and custom error handler
     */
    public <T> Optional<T> executeWithErrorHandling(String context, ThrowingSupplier<T> operation, 
                                                   Consumer<Exception> errorHandler) {
        try {
            T result = operation.get();
            return Optional.ofNullable(result);
        } catch (Exception e) {
            if (errorHandler != null) {
                errorHandler.accept(e);
            } else {
                handleException(context, e);
            }
            return Optional.empty();
        }
    }
    
    /**
     * Functional interface for operations that can throw exceptions
     */
    @FunctionalInterface
    public interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
    
    /**
     * Error severity levels
     */
    public enum ErrorSeverity {
        INFO, WARNING, ERROR, CRITICAL
    }
    
    /**
     * Log error with severity level
     */
    public void logError(String context, String message, ErrorSeverity severity) {
        String formattedMessage = String.format("[%s] %s: %s", severity, context, message);
        
        switch (severity) {
            case INFO -> logger.info(formattedMessage);
            case WARNING -> logger.warn(formattedMessage);
            case ERROR -> logger.error(formattedMessage);
            case CRITICAL -> logger.fatal(formattedMessage);
        }
    }
}