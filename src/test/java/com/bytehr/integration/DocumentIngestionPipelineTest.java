package com.bytehr.integration;

import com.bytehr.service.impl.ChunkingServiceImpl;
import com.bytehr.service.impl.LanguageDetectionServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for the document ingestion pipeline (without external dependencies).
 */
class DocumentIngestionPipelineTest {

    @Test
    void shouldChunkAndDetectLanguageForEnglishHrDocument() {
        ChunkingServiceImpl chunker = new ChunkingServiceImpl();
        LanguageDetectionServiceImpl detector = new LanguageDetectionServiceImpl();

        String hrText = """
                ANNUAL LEAVE POLICY
                
                1. Entitlement
                All full-time employees are entitled to 20 working days of paid annual leave per calendar year.
                Part-time employees receive annual leave on a pro-rata basis.
                
                2. Accrual
                Annual leave accrues from the date of employment commencement.
                New employees complete a 3-month probation period before taking leave.
                
                3. Approval Process
                Leave requests must be submitted via the HR portal at least 5 working days in advance.
                The direct manager approves or rejects leave requests within 2 working days.
                
                4. Carry-Over
                Up to 10 unused days may be carried over to the following year.
                Carried-over leave must be used by March 31 of the new year.
                
                5. Public Holidays
                National public holidays are in addition to the annual leave entitlement.
                Albania observes 12 public holidays per year.
                Serbia observes 11 public holidays per year.
                """;

        List<String> chunks = chunker.chunk(hrText, 500, 100);
        assertThat(chunks).isNotEmpty();
        assertThat(chunks.size()).isGreaterThan(1);

        String detectedLang = detector.detectLanguage(hrText);
        assertThat(detectedLang).isEqualTo("en");

        // Verify all chunks are non-blank and within size limits
        for (String chunk : chunks) {
            assertThat(chunk).isNotBlank();
            assertThat(chunk.length()).isLessThanOrEqualTo(500);
        }
    }

    @Test
    void shouldProcessDocumentWithUnicodeCharacters() {
        ChunkingServiceImpl chunker = new ChunkingServiceImpl();

        String unicodeText = "Politika e pushimeve vjetore. Të gjithë punonjësit kanë të drejtë "
                + "në 20 ditë pushim vjetor me pagesë. " + "X".repeat(500);

        List<String> chunks = chunker.chunk(unicodeText, 200, 50);
        assertThat(chunks).isNotEmpty();
        // First chunk must start with the Albanian text
        assertThat(chunks.get(0)).contains("Politika e pushimeve");
    }
}
