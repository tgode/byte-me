package com.bytehr.api;

import com.bytehr.api.dto.SyncResponse;
import com.bytehr.config.SecurityConfig;
import com.bytehr.service.DocumentSyncService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SyncController.class)
@Import(SecurityConfig.class)
class SyncControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentSyncService documentSyncService;

    @Test
    void shouldReturn200OnSuccessfulSync() throws Exception {
        doNothing().when(documentSyncService).synchronize();

        mockMvc.perform(post("/api/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void shouldReturn500WhenSyncThrowsException() throws Exception {
        doThrow(new RuntimeException("SharePoint unreachable"))
                .when(documentSyncService).synchronize();

        mockMvc.perform(post("/api/sync"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("error"));
    }
}
