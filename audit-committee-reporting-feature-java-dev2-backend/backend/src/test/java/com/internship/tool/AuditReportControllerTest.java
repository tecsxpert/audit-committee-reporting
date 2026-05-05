package com.internship.tool;

import com.internship.tool.repository.AuditReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Java Developer-2's endpoints.
 * MockMvc simulates HTTP requests without starting a real server.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuditReportControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired AuditReportRepository repository;

    @BeforeEach
    void setup() {
        repository.deleteAll();   // clean slate before each test
    }

    @Test
    void getStats_returnsOk() throws Exception {
        mockMvc.perform(get("/api/reports/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").exists());
    }

    @Test
    void searchWithKeyword_returnsOk() throws Exception {
        mockMvc.perform(get("/api/reports/search?q=audit"))
                .andExpect(status().isOk());
    }

    @Test
    void listAll_returnsPagedResult() throws Exception {
        mockMvc.perform(get("/api/reports?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void exportCsv_returnsFile() throws Exception {
        mockMvc.perform(get("/api/reports/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv"))
                .andExpect(header().exists("Content-Disposition"));
    }

    @Test
    void deleteNonExistentReport_returns404() throws Exception {
        mockMvc.perform(delete("/api/reports/99999"))
                .andExpect(status().isNotFound());
        // 404 expected for non-existent report
    }
}