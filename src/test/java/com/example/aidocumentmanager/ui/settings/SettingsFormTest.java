package com.example.aidocumentmanager.ui.settings;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SettingsFormTest {

    private static SettingsForm withModels(String openAIModel, String huggingFaceModel) {
        SettingsForm defaults = SettingsForm.defaults();
        return new SettingsForm(
                defaults.provider(), defaults.openAIApiKey(), openAIModel,
                defaults.huggingFaceApiKey(), huggingFaceModel,
                defaults.maxChatHistory(), defaults.loggingEnabled(), defaults.autoSaveEnabled(), defaults.theme(),
                defaults.chunkSize(), defaults.chunkOverlap(), defaults.maxRetrievalResults(),
                defaults.similarityThreshold());
    }

    private static SettingsForm withThreshold(double threshold) {
        SettingsForm defaults = SettingsForm.defaults();
        return new SettingsForm(
                defaults.provider(), defaults.openAIApiKey(), defaults.openAIModel(),
                defaults.huggingFaceApiKey(), defaults.huggingFaceModel(),
                defaults.maxChatHistory(), defaults.loggingEnabled(), defaults.autoSaveEnabled(), defaults.theme(),
                defaults.chunkSize(), defaults.chunkOverlap(), defaults.maxRetrievalResults(),
                threshold);
    }

    @Test
    void supersededModelNamesAreReplacedWithTheCurrentDefaults() {
        SettingsForm normalized = withModels("gpt-3.5-turbo", "gpt2").normalized();

        assertEquals(SettingsForm.DEFAULT_OPENAI_MODEL, normalized.openAIModel());
        assertEquals(SettingsForm.DEFAULT_HUGGINGFACE_MODEL, normalized.huggingFaceModel());
    }

    @Test
    void blankModelNamesFallBackToTheDefaults() {
        SettingsForm normalized = withModels("   ", null).normalized();

        assertEquals(SettingsForm.DEFAULT_OPENAI_MODEL, normalized.openAIModel());
        assertEquals(SettingsForm.DEFAULT_HUGGINGFACE_MODEL, normalized.huggingFaceModel());
    }

    @Test
    void aDeliberatelyChosenModelIsLeftAlone() {
        SettingsForm normalized = withModels("gpt-4o", "meta-llama/Llama-3.3-70B-Instruct").normalized();

        assertEquals("gpt-4o", normalized.openAIModel());
        assertEquals("meta-llama/Llama-3.3-70B-Instruct", normalized.huggingFaceModel());
    }

    @Test
    void similarityThresholdIsRoundedToTheTwoDecimalsTheSliderShows() {
        assertEquals(0.55, withThreshold(0.5499999).normalized().similarityThreshold(), 1e-9);
        assertEquals(0.7, withThreshold(0.7000001).normalized().similarityThreshold(), 1e-9);
    }

    @Test
    void providerDisplayNameAndKeyRoundTrip() {
        assertEquals(SettingsForm.PROVIDER_HUGGINGFACE, SettingsForm.providerKey(SettingsForm.DISPLAY_HUGGINGFACE));
        assertEquals(SettingsForm.PROVIDER_OPENAI, SettingsForm.providerKey(SettingsForm.DISPLAY_OPENAI));

        assertEquals(SettingsForm.DISPLAY_HUGGINGFACE,
                SettingsForm.providerDisplayName(SettingsForm.PROVIDER_HUGGINGFACE));
        assertEquals(SettingsForm.DISPLAY_OPENAI, SettingsForm.providerDisplayName(SettingsForm.PROVIDER_OPENAI));
    }

    @Test
    void anUnsetProviderIsTreatedAsOpenAI() {
        assertEquals(SettingsForm.PROVIDER_OPENAI, SettingsForm.providerKey(null));
    }
}
