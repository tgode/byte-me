package com.bytehr.service;

public interface LanguageDetectionService {

    /**
     * Detects the BCP-47 language code for the given text (e.g. "en", "sq", "sr").
     * Returns "en" as default if detection is inconclusive.
     */
    String detectLanguage(String text);
}
