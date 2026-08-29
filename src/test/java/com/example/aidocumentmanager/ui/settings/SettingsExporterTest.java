package com.example.aidocumentmanager.ui.settings;

import com.example.aidocumentmanager.config.ConfigurationManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Runs against a sandboxed user.home (see the surefire configuration in the
 * pom), so it never touches a real installation's settings.
 */
class SettingsExporterTest {

    private static final String OPENAI_SECRET = "sk-do-not-leak-this-openai-key";
    private static final String HUGGINGFACE_SECRET = "hf_do_not_leak_this_key";

    private final ConfigurationManager config = ConfigurationManager.getInstance();

    @AfterEach
    void clearKeys() {
        config.setOpenAIApiKey("");
        config.setHuggingFaceApiKey("");
    }

    @Test
    @SuppressWarnings("unchecked")
    void apiKeysAreRedacted() {
        config.setOpenAIApiKey(OPENAI_SECRET);
        config.setHuggingFaceApiKey(HUGGINGFACE_SECRET);

        Map<String, Object> exported = SettingsExporter.export(config, "2026-01-01T00:00:00");
        Map<String, Object> api = (Map<String, Object>) exported.get("api");

        assertEquals(SettingsExporter.REDACTED, api.get("openAIApiKey"));
        assertEquals(SettingsExporter.REDACTED, api.get("huggingFaceApiKey"));
    }

    @Test
    void noSecretSurvivesAnywhereInTheExport() {
        config.setOpenAIApiKey(OPENAI_SECRET);
        config.setHuggingFaceApiKey(HUGGINGFACE_SECRET);

        // The export is the file a user attaches when asking for help, so check
        // the whole rendered document, not just the fields we remembered to redact.
        String rendered = SettingsExporter.export(config, "2026-01-01T00:00:00").toString();

        assertFalse(rendered.contains(OPENAI_SECRET), rendered);
        assertFalse(rendered.contains(HUGGINGFACE_SECRET), rendered);
    }

    @Test
    void carriesTheTimestampAndTheConfiguredSections() {
        Map<String, Object> exported = SettingsExporter.export(config, "2026-01-01T00:00:00");

        assertEquals("2026-01-01T00:00:00", exported.get("exportedAt"));
        assertNotNull(exported.get("appVersion"));
        assertNotNull(exported.get("api"));
        assertNotNull(exported.get("application"));
        assertNotNull(exported.get("rag"));
    }
}
