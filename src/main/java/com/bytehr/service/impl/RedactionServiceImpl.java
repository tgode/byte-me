package com.bytehr.service.impl;

import com.bytehr.service.RedactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * Redacts personally identifiable information (PII) from document text before it is
 * chunked and embedded. This prevents sensitive data from entering the vector database
 * or being returned in RAG answers.
 *
 * Patterns covered:
 *   - Email addresses
 *   - Phone numbers (international and local formats)
 *   - IBAN / bank account numbers
 *   - Albanian National Identification Numbers (NID / NIPT)
 *   - Serbian JMBG (unique citizen number, 13 digits)
 */
@Service
@Slf4j
public class RedactionServiceImpl implements RedactionService {

    // Email: standard RFC 5322 simplified pattern
    private static final Pattern EMAIL = Pattern.compile(
            "\\b[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}\\b");

    // Phone: international (+xxx) and local formats with optional spaces/dashes
    // Matches 7–15 digits, possibly preceded by +, with separators
    private static final Pattern PHONE = Pattern.compile(
            "(?:\\+?\\d[\\d\\s\\-\\.\\(\\)]{6,14}\\d)");

    // IBAN: 2 letters + 2 digits + up to 30 alphanumerics (no spaces — normalized)
    private static final Pattern IBAN = Pattern.compile(
            "\\b[A-Z]{2}\\d{2}[A-Z0-9]{4,30}\\b");

    // Albanian NID (Numri i Identitetit): letter + 9 digits + letter, e.g. A12345678B
    private static final Pattern ALBANIAN_NID = Pattern.compile(
            "\\b[A-Z]\\d{8,9}[A-Z]\\b");

    // Serbian JMBG: exactly 13 consecutive digits
    private static final Pattern SERBIAN_JMBG = Pattern.compile("\\b\\d{13}\\b");

    // Albanian NIPT (tax number): letter + 8 digits + letter
    private static final Pattern ALBANIAN_NIPT = Pattern.compile(
            "\\b[A-Z]\\d{8}[A-Za-z]\\b");

    @Override
    public String redact(String text) {
        if (text == null || text.isBlank()) return text;

        String result = text;
        int redactedCount = 0;

        result = redact(result, IBAN,           "[IBAN REDACTED]");
        result = redact(result, SERBIAN_JMBG,   "[ID REDACTED]");
        result = redact(result, ALBANIAN_NID,   "[ID REDACTED]");
        result = redact(result, ALBANIAN_NIPT,  "[ID REDACTED]");
        result = redact(result, EMAIL,          "[EMAIL REDACTED]");
        result = redact(result, PHONE,          "[PHONE REDACTED]");

        if (!result.equals(text)) {
            log.debug("[Redaction] PII tokens replaced in document text");
        }

        return result;
    }

    private String redact(String text, Pattern pattern, String replacement) {
        return pattern.matcher(text).replaceAll(replacement);
    }
}
