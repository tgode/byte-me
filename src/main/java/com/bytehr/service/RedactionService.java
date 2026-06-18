package com.bytehr.service;

public interface RedactionService {

    /**
     * Replaces PII patterns in the input text with redaction tokens before indexing.
     * Patterns covered: email addresses, phone numbers, IBAN/account numbers, national IDs.
     *
     * @param text raw extracted document text
     * @return text with PII replaced by redaction tokens
     */
    String redact(String text);
}
