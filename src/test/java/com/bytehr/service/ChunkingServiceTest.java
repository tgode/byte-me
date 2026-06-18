package com.bytehr.service;

import com.bytehr.service.impl.ChunkingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class ChunkingServiceTest {

    private ChunkingServiceImpl chunkingService;

    @BeforeEach
    void setUp() {
        chunkingService = new ChunkingServiceImpl();
    }

    @Test
    void shouldReturnEmptyListForNullText() {
        List<String> chunks = chunkingService.chunk(null, 1000, 200);
        assertThat(chunks).isEmpty();
    }

    @Test
    void shouldReturnEmptyListForBlankText() {
        List<String> chunks = chunkingService.chunk("   ", 1000, 200);
        assertThat(chunks).isEmpty();
    }

    @Test
    void shouldReturnSingleChunkWhenTextFitsInOneChunk() {
        String text = "This is a short HR policy document about vacation.";
        List<String> chunks = chunkingService.chunk(text, 1000, 200);
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).isEqualTo(text.trim());
    }

    @Test
    void shouldSplitLargeTextIntoMultipleChunks() {
        String text = "A".repeat(2500);
        List<String> chunks = chunkingService.chunk(text, 1000, 200);
        assertThat(chunks).hasSize(4); // 0-999, 800-1799, 1600-2499, 2400-2499
    }

    @Test
    void shouldRespectOverlapBetweenChunks() {
        String text = "A".repeat(1500);
        List<String> chunks = chunkingService.chunk(text, 1000, 200);
        assertThat(chunks).hasSize(3);
        assertThat(chunks.get(0)).hasSize(1000);
        assertThat(chunks.get(1)).hasSize(1000); // 800..1799
    }

    @Test
    void shouldThrowExceptionWhenOverlapEqualToChunkSize() {
        assertThatThrownBy(() -> chunkingService.chunk("text", 1000, 1000))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldHandleExactChunkSizeText() {
        String text = "X".repeat(1000);
        List<String> chunks = chunkingService.chunk(text, 1000, 200);
        assertThat(chunks).hasSize(1);
    }

    @Test
    void chunksFromHrTextShouldPreserveContent() {
        String hrPolicy = """
                Annual Leave Policy
                Employees are entitled to 20 days of annual leave per calendar year.
                Leave must be approved by the direct manager at least 5 working days in advance.
                Unused leave can be carried over to the next year, up to a maximum of 10 days.
                """;
        List<String> chunks = chunkingService.chunk(hrPolicy, 200, 50);
        assertThat(chunks).isNotEmpty();
        String combined = String.join(" ", chunks);
        assertThat(combined).contains("Annual Leave Policy");
    }
}
