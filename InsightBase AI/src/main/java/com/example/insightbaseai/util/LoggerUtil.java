package com.example.insightbaseai.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;

public class LoggerUtil {
    private static LoggerUtil instance;
    private final Logger logger;
    
    private LoggerUtil() {
        this.logger = LogManager.getLogger(LoggerUtil.class);
    }
    
    public static LoggerUtil getInstance() {
        if (instance == null) {
            synchronized (LoggerUtil.class) {
                if (instance == null) {
                    instance = new LoggerUtil();
                }
            }
        }
        return instance;
    }
    
    public void info(String message) {
        logger.info(message);
    }
    
    public void debug(String message) {
        logger.debug(message);
    }
    
    public void warn(String message) {
        logger.warn(message);
    }
    
    public void warn(String message, Throwable throwable) {
        logger.warn(message, throwable);
    }
    
    public void error(String message) {
        logger.error(message);
    }
    
    public void error(String message, Throwable throwable) {
        logger.error(message, throwable);
    }
    
    public void trace(String message) {
        logger.trace(message);
    }
    
    public void fatal(String message) {
        logger.fatal(message);
    }
    
    public void fatal(String message, Throwable throwable) {
        logger.fatal(message, throwable);
    }
    
    /**
     * Log application startup
     */
    public void logStartup() {
        info("=".repeat(50));
        info("InsightBase AI Application Starting...");
        info("Java Version: " + System.getProperty("java.version"));
        info("Operating System: " + System.getProperty("os.name"));
        info("User Directory: " + System.getProperty("user.dir"));
        info("=".repeat(50));
    }
    
    /**
     * Log application shutdown
     */
    public void logShutdown() {
        info("=".repeat(50));
        info("InsightBase AI Application Shutting Down...");
        info("=".repeat(50));
    }
    
    /**
     * Log document processing events
     */
    public void logDocumentProcessing(String fileName, String action) {
        info(String.format("Document Processing - Action: %s, File: %s", action, fileName));
    }
    
    /**
     * Log AI service events
     */
    public void logAIService(String event, String details) {
        info(String.format("AI Service - Event: %s, Details: %s", event, details));
    }
    
    /**
     * Log performance metrics
     */
    public void logPerformance(String operation, long durationMs) {
        info(String.format("Performance - Operation: %s, Duration: %d ms", operation, durationMs));
    }
    
    /**
     * Log user actions
     */
    public void logUserAction(String action, String details) {
        info(String.format("User Action - %s: %s", action, details));
    }
    
    /**
     * Convert throwable to string for logging
     */
    public String getStackTrace(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
    
    /**
     * Log with custom format
     */
    public void log(LogLevel level, String category, String message) {
        String formattedMessage = String.format("[%s] %s", category, message);
        
        switch (level) {
            case TRACE -> logger.trace(formattedMessage);
            case DEBUG -> logger.debug(formattedMessage);
            case INFO -> logger.info(formattedMessage);
            case WARN -> logger.warn(formattedMessage);
            case ERROR -> logger.error(formattedMessage);
            case FATAL -> logger.fatal(formattedMessage);
        }
    }
    
    /**
     * Log levels for custom logging
     */
    public enum LogLevel {
        TRACE, DEBUG, INFO, WARN, ERROR, FATAL
    }
}