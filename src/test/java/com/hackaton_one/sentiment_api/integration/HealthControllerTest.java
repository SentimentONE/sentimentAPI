package com.hackaton_one.sentiment_api.integration;

import com.hackaton_one.sentiment_api.api.controller.HealthCheckController;
import com.hackaton_one.sentiment_api.service.SentimentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthCheckController.class)
class HealthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SentimentService sentimentService;

    @Test
    void shouldReturn200WhenSendingGetToHealth() throws Exception {
        mockMvc.perform(get("/health"))
               .andExpect(status().isOk());
    }

    @Test
    void shouldReturnHealthStatusInResponse() throws Exception {
        when(sentimentService.isModelAvailable()).thenReturn(true);

        mockMvc.perform(get("/health"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.status").value("UP"))
               .andExpect(jsonPath("$.modelStatus").exists());
    }

    @Test
    void shouldReturnModelStatusAvailableWhenModelIsLoaded() throws Exception {
        when(sentimentService.isModelAvailable()).thenReturn(true);

        mockMvc.perform(get("/health"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.modelStatus").value("AVAILABLE"));
    }

    @Test
    void shouldReturnModelStatusUnavailableWhenModelIsNotLoaded() throws Exception {
        when(sentimentService.isModelAvailable()).thenReturn(false);

        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modelStatus").value("UNAVAILABLE"));
    }
}
