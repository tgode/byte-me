package com.bytehr.service.impl;

import com.bytehr.service.LanguageDetectionService;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.language.detect.LanguageDetector;
import org.apache.tika.language.detect.LanguageResult;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class LanguageDetectionServiceImpl implements LanguageDetectionService {

    private final LanguageDetector languageDetector;

    public LanguageDetectionServiceImpl() {
        LanguageDetector detector = null;
        try {
            detector = LanguageDetector.getDefaultLanguageDetector().loadModels();
            log.info("Tika language detector loaded successfully");
        } catch (Exception e) {
            log.warn("Tika language detector unavailable ({}). Language detection will default to 'en'.",
                    e.getMessage());
        }
        this.languageDetector = detector;
    }

    @Override
    public String detectLanguage(String text) {
        if (languageDetector == null || text == null || text.isBlank()) {
            return "en";
        }
        try {
            String sample = text.length() > 1000 ? text.substring(0, 1000) : text;
            LanguageResult result = languageDetector.detect(sample);
            if (result != null && !result.isUnknown() && result.getRawScore() > 0.7f) {
                String lang = result.getLanguage();
                log.debug("Detected language: {} (score: {})", lang, result.getRawScore());
                return lang;
            }
        } catch (Exception e) {
            log.warn("Language detection failed, defaulting to 'en'", e);
        }
        return "en";
    }
}
