package com.bytehr.service.impl;

import com.bytehr.service.LanguageDetectionService;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.language.detect.LanguageDetector;
import org.apache.tika.language.detect.LanguageResult;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@Slf4j
public class LanguageDetectionServiceImpl implements LanguageDetectionService {

    private final LanguageDetector languageDetector;

    public LanguageDetectionServiceImpl() throws IOException {
        this.languageDetector = LanguageDetector.getDefaultLanguageDetector().loadModels();
    }

    @Override
    public String detectLanguage(String text) {
        if (text == null || text.isBlank()) {
            return "en";
        }
        try {
            // Use a reasonable sample for detection
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
