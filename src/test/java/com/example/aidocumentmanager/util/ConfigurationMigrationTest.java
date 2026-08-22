package com.example.aidocumentmanager.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The similarity threshold shipped with a value now known to starve retrieval.
 * Existing installs carry it in their config file, so the migration is what
 * actually fixes them - changing the default alone only helps a fresh install.
 *
 * <p>
 * Runs against a sandboxed user.home (see the surefire configuration in the
 * pom), so it never touches a real installation's settings.
 */
class ConfigurationMigrationTest {

    private static final double CURRENT_DEFAULT = 0.5;

    private static Path configFile() {
        return Paths.get(System.getProperty("user.home"), ".aidocumentmanager", "config.properties");
    }

    /** Drop the singleton so the next getInstance() re-reads the file. */
    private static void resetSingleton() throws Exception {
        Field instance = ConfigurationManager.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);
    }

    private static void writeConfig(String... lines) throws IOException {
        Path file = configFile();
        Files.createDirectories(file.getParent());
        Files.writeString(file, String.join(System.lineSeparator(), lines) + System.lineSeparator());
    }

    @BeforeEach
    void isolate() throws Exception {
        resetSingleton();
    }

    @AfterEach
    void leaveCleanDefaults() throws Exception {
        // Later tests read this config, so do not leave a hand-written one behind.
        Files.deleteIfExists(configFile());
        resetSingleton();
    }

    @Test
    void migratesTheOriginalThresholdThatBrokeRetrieval() throws Exception {
        writeConfig("rag.similarity.threshold=0.7");

        assertEquals(CURRENT_DEFAULT, ConfigurationManager.getInstance().getSimilarityThreshold(), 1e-9,
                "a config never migrated before should land on the current default in one launch");
    }

    @Test
    void leavesADeliberateThresholdAlone() throws Exception {
        writeConfig("rag.similarity.threshold=0.62");

        assertEquals(0.62, ConfigurationManager.getInstance().getSimilarityThreshold(), 1e-9,
                "a value the user chose is not a superseded default and must survive");
    }

    @Test
    void doesNotReapplyAMigrationTheConfigHasAlreadyPassed() throws Exception {
        // 0.7 on a config that already passed v1 means someone chose it, rather
        // than it being left over from the version that shipped it as default.
        writeConfig("config.schema.version=1", "rag.similarity.threshold=0.7");

        assertEquals(0.7, ConfigurationManager.getInstance().getSimilarityThreshold(), 1e-9);
    }

    @Test
    void stampsTheSchemaVersionSoMigrationsRunOnce() throws Exception {
        writeConfig("rag.similarity.threshold=0.7");
        ConfigurationManager.getInstance();

        assertTrue(Files.readString(configFile()).contains("config.schema.version=1"),
                "the version has to be persisted, or every launch would migrate again");
    }

    @Test
    void survivesAnUnreadableSchemaVersion() throws Exception {
        writeConfig("config.schema.version=not-a-number", "rag.similarity.threshold=0.7");

        assertEquals(CURRENT_DEFAULT, ConfigurationManager.getInstance().getSimilarityThreshold(), 1e-9,
                "a corrupt version should be treated as pre-migration, not throw");
    }

    @Test
    void freshInstallGetsTheCurrentDefault() throws Exception {
        Files.deleteIfExists(configFile());

        assertEquals(CURRENT_DEFAULT, ConfigurationManager.getInstance().getSimilarityThreshold(), 1e-9);
    }
}
