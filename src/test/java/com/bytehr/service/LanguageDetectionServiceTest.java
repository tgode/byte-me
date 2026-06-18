package com.bytehr.service;

import com.bytehr.service.impl.LanguageDetectionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class LanguageDetectionServiceTest {

    private LanguageDetectionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new LanguageDetectionServiceImpl();
    }

    @Test
    void shouldReturnEnForNullInput() {
        assertThat(service.detectLanguage(null)).isEqualTo("en");
    }

    @Test
    void shouldReturnEnForBlankInput() {
        assertThat(service.detectLanguage("   ")).isEqualTo("en");
    }

    @Test
    void shouldDetectEnglishText() {
        String english = """
                Annual leave policy: Employees are entitled to twenty working days of paid annual
                leave per calendar year. Leave requests must be submitted to the HR department at
                least five working days in advance for approval.
                """;
        String lang = service.detectLanguage(english);
        assertThat(lang).isEqualTo("en");
    }

    @Test
    void shouldFallbackToEnForShortAmbiguousText() {
        // Very short text should not crash
        String result = service.detectLanguage("ok");
        assertThat(result).isNotNull().isNotBlank();
    }
}
