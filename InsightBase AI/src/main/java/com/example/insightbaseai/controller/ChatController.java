package com.example.insightbaseai.controller;

import com.example.insightbaseai.service.AIService;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import javafx.util.Duration;

public class ChatController {
    @FXML
    private ScrollPane chatScrollPane;
    @FXML
    private VBox chatContainer;
    @FXML
    private TextField inputField;
    @FXML
    private Button sendButton;
    @FXML
    private Button clearChatButton;

    private AIService aiService;
    private Timeline thinkingAnimation;
    private int thinkingDots = 0;
    private String thinkingPrefix = "AI is thinking";
    private Label thinkingLabel;

    public void initialize() {
        aiService = AIService.getInstance();
        sendButton.setOnAction(e -> handleSend());

        // Add Enter key support for sending messages
        inputField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                handleSend();
            }
        });
    }

    @FXML
    private void handleClearChat() {
        chatContainer.getChildren().clear();
        aiService.clearChatHistory();
        // Show a welcome message after clearing
        addAIMessage("Chat cleared. How can I help you?");
    }

    private void handleSend() {
        String userInput = inputField.getText().trim();
        if (userInput.isEmpty())
            return;

        // INSTANTLY show user message and clear input - happens immediately
        addUserMessage(userInput);
        inputField.clear();

        // Start thinking animation
        startThinkingAnimation();

        // Create background task for AI response so UI doesn't freeze
        Task<String> aiTask = new Task<String>() {
            @Override
            protected String call() throws Exception {
                return aiService.getResponse(userInput);
            }

            @Override
            protected void succeeded() {
                // This runs on JavaFX thread when AI response is ready
                Platform.runLater(() -> {
                    stopThinkingAnimation();
                    addAIMessage(getValue());
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    stopThinkingAnimation();
                    addAIMessage("Sorry, I encountered an error. Please try again.");
                });
            }
        };

        // Run AI task in background thread
        new Thread(aiTask).start();
    }

    private void addUserMessage(String message) {
        // Create user message bubble (right side)
        HBox messageContainer = new HBox();
        messageContainer.setAlignment(Pos.CENTER_RIGHT);
        messageContainer.setPadding(new Insets(5, 10, 5, 80));

        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(450); // Increased from 300 to 450
        messageLabel.setStyle(
                "-fx-background-color: #007bff; " +
                        "-fx-text-fill: white; " +
                        "-fx-padding: 12; " +
                        "-fx-background-radius: 15; " +
                        "-fx-font-size: 14;");

        messageContainer.getChildren().add(messageLabel);
        chatContainer.getChildren().add(messageContainer);
        scrollToBottom();
    }

    private void addAIMessage(String message) {
        // Create AI message bubble (left side)
        HBox messageContainer = new HBox();
        messageContainer.setAlignment(Pos.CENTER_LEFT);
        messageContainer.setPadding(new Insets(5, 80, 5, 10));

        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(450); // Increased from 300 to 450
        messageLabel.setStyle(
                "-fx-background-color: #e9ecef; " +
                        "-fx-text-fill: #333; " +
                        "-fx-padding: 12; " +
                        "-fx-background-radius: 15; " +
                        "-fx-font-size: 14;");

        messageContainer.getChildren().add(messageLabel);
        chatContainer.getChildren().add(messageContainer);
        scrollToBottom();
    }

    private void scrollToBottom() {
        Platform.runLater(() -> {
            chatScrollPane.setVvalue(1.0);
        });
    }

    private void startThinkingAnimation() {
        // Create thinking message bubble (left side)
        HBox messageContainer = new HBox();
        messageContainer.setAlignment(Pos.CENTER_LEFT);
        messageContainer.setPadding(new Insets(5, 80, 5, 10));

        thinkingLabel = new Label("AI is thinking.");
        thinkingLabel.setWrapText(true);
        thinkingLabel.setMaxWidth(450); // Increased from 300 to 450
        thinkingLabel.setStyle(
                "-fx-background-color: #f8f9fa; " +
                        "-fx-text-fill: #666; " +
                        "-fx-padding: 12; " +
                        "-fx-background-radius: 15; " +
                        "-fx-font-size: 14; " +
                        "-fx-font-style: italic;");

        messageContainer.getChildren().add(thinkingLabel);
        chatContainer.getChildren().add(messageContainer);
        scrollToBottom();

        // Create animation timeline
        thinkingAnimation = new Timeline(new KeyFrame(Duration.seconds(0.5), e -> {
            // Update dots animation (1-3 dots cycling)
            thinkingDots = (thinkingDots % 3) + 1;
            String dots = ".".repeat(thinkingDots);
            thinkingLabel.setText(thinkingPrefix + dots);
        }));

        thinkingAnimation.setCycleCount(Animation.INDEFINITE);
        thinkingAnimation.play();
    }

    private void stopThinkingAnimation() {
        if (thinkingAnimation != null) {
            thinkingAnimation.stop();
        }

        // Remove the thinking message
        if (thinkingLabel != null && thinkingLabel.getParent() != null) {
            chatContainer.getChildren().remove(thinkingLabel.getParent());
            thinkingLabel = null;
        }

        // Reset dots counter
        thinkingDots = 0;
    }
}
