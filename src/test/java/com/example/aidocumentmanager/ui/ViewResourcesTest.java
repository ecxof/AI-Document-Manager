package com.example.aidocumentmanager.ui;

import com.example.aidocumentmanager.MainApp;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The views and stylesheet are looked up by name relative to the loading
 * class's package, and each view names its controller as a string. Neither is
 * visible to the compiler, and only the chat tab is loaded at startup, so a
 * wrong path or a stale fx:controller in one of the other three views survives
 * until a user clicks that tab. This loads every view the way the application
 * does, which also proves each fx:controller class resolves and its @FXML
 * fields still match the markup.
 */
class ViewResourcesTest {

    @BeforeAll
    static void startToolkit() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        try {
            Platform.startup(started::countDown);
        } catch (IllegalStateException alreadyRunning) {
            started.countDown();
        }
        assertTrue(started.await(30, TimeUnit.SECONDS), "JavaFX toolkit did not start");
    }

    @Test
    void mainAppResolvesItsViewAndStylesheet() {
        assertNotNull(MainApp.class.getResource("ui/MainView.fxml"), "ui/MainView.fxml");
        assertNotNull(MainApp.class.getResource("ui/app.css"), "ui/app.css");
    }

    @Test
    void everyViewLoadsWithItsController() throws Exception {
        loadOnFxThread(MainApp.class.getResource("ui/MainView.fxml"));
        loadOnFxThread(MainController.class.getResource("chat/ChatView.fxml"));
        loadOnFxThread(MainController.class.getResource("documents/DocumentsView.fxml"));
        loadOnFxThread(MainController.class.getResource("search/SearchView.fxml"));
        loadOnFxThread(MainController.class.getResource("settings/SettingsView.fxml"));
    }

    private void loadOnFxThread(URL view) throws Exception {
        assertNotNull(view, "view resource not found");

        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch loaded = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                assertNotNull(new FXMLLoader(view).load(), view.toString());
            } catch (Throwable t) {
                failure.set(t);
            } finally {
                loaded.countDown();
            }
        });

        assertTrue(loaded.await(60, TimeUnit.SECONDS), "timed out loading " + view);
        if (failure.get() != null) {
            throw new AssertionError("failed to load " + view, failure.get());
        }
    }
}
