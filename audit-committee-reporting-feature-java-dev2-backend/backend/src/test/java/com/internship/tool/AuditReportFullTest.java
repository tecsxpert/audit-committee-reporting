package com.internship.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.internship.tool.dto.AuditReportRequest;
import com.internship.tool.entity.AuditReport;
import com.internship.tool.repository.AuditReportRepository;
import com.internship.tool.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Java Developer-2's endpoints.
 *
 * @WithMockUser simulates a logged-in user so we don't need a real JWT.
 * @ActiveProfiles("test") uses test configuration if you create application-test.yml.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditReportFullTest {

    @Autowired MockMvc              mockMvc;
    @Autowired ObjectMapper         objectMapper;
    @Autowired AuditReportRepository reportRepo;
    @Autowired AuditLogRepository    logRepo;

    // ── Setup ──────────────────────────────────────────────────────────────

    @BeforeEach
    void cleanDatabase() {
        logRepo.deleteAll();     // delete audit logs first (foreign key)
        reportRepo.deleteAll();  // then delete reports
    }

    private AuditReport createSampleReport() {
        AuditReport r = new AuditReport();
        r.setTitle("Q1 Financial Audit");
        r.setDescription("Quarterly audit of financial controls");
        r.setStatus("PENDING");
        r.setScore(75);
        r.setCategory("Finance");
        r.setAssignedTo("john@company.com");
        r.setDueDate(LocalDate.now().plusDays(10));
        r.setCreatedBy("admin");
        return reportRepo.save(r);
    }

    // ── Test 1: GET /stats returns total count ─────────────────────────────

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void getStats_returnsCorrectTotal() throws Exception {
        createSampleReport();
        createSampleReport();

        mockMvc.perform(get("/api/reports/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2));
    }

    // ── Test 2: GET /stats returns avgScore ───────────────────────────────

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void getStats_returnsAvgScore() throws Exception {
        createSampleReport();

        mockMvc.perform(get("/api/reports/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avgScore").exists());
    }

    // ── Test 3: GET /search finds by keyword ──────────────────────────────

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void search_findsReportByKeyword() throws Exception {
        createSampleReport();   // title = "Q1 Financial Audit"

        mockMvc.perform(get("/api/reports/search?q=Financial"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.content[0].title").value("Q1 Financial Audit"));
    }

    // ── Test 4: GET /search returns empty for unknown keyword ──────────────

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void search_returnsEmptyForUnknownKeyword() throws Exception {
        mockMvc.perform(get("/api/reports/search?q=XYZNOTEXIST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    // ── Test 5: GET / returns paged list ──────────────────────────────────

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void listAll_returnsPaginatedResults() throws Exception {
        createSampleReport();

        mockMvc.perform(get("/api/reports?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(1)));
    }

    // ── Test 6: GET / filters by status ───────────────────────────────────

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void listAll_filtersByStatus() throws Exception {
        createSampleReport();   // status = PENDING

        mockMvc.perform(get("/api/reports?status=PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("PENDING"));
    }

    // ── Test 7: PUT /{id} updates a report ────────────────────────────────

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void update_changesReportTitle() throws Exception {
        AuditReport saved = createSampleReport();

        AuditReportRequest updateReq = new AuditReportRequest();
        updateReq.setTitle("Updated Title");
        updateReq.setDescription("Updated description");
        updateReq.setStatus("IN_REVIEW");

        mockMvc.perform(put("/api/reports/" + saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.status").value("IN_REVIEW"));
    }

    // ── Test 8: PUT /{id} returns 404 for non-existent report ─────────────

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void update_returns404ForNonExistentReport() throws Exception {
        AuditReportRequest req = new AuditReportRequest();
        req.setTitle("Any Title");

        mockMvc.perform(put("/api/reports/99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    // ── Test 9: DELETE /{id} soft-deletes a report ────────────────────────

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void delete_softDeletesReport() throws Exception {
        AuditReport saved = createSampleReport();

        // Delete it
        mockMvc.perform(delete("/api/reports/" + saved.getId()))
                .andExpect(status().isNoContent());   // 204

        // Verify it no longer appears in the list
        mockMvc.perform(get("/api/reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // ── Test 10: GET /export returns CSV ──────────────────────────────────

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void export_returnsCsvFile() throws Exception {
        createSampleReport();

        mockMvc.perform(get("/api/reports/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv"))
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(content().string(containsString("Q1 Financial Audit")));
    }

    // ── Test 11: VIEWER cannot delete (403) ───────────────────────────────

    @Test
    @WithMockUser(username = "viewer", roles = "VIEWER")
    void delete_forbiddenForViewer() throws Exception {
        AuditReport saved = createSampleReport();

        mockMvc.perform(delete("/api/reports/" + saved.getId()))
                .andExpect(status().isForbidden());   // 403
    }

    // ── Test 12: Audit log saved after update ─────────────────────────────

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void update_savesAuditLog() throws Exception {
        AuditReport saved = createSampleReport();

        AuditReportRequest req = new AuditReportRequest();
        req.setTitle("New Title After Update");

        mockMvc.perform(put("/api/reports/" + saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        // Check audit log was created
        mockMvc.perform(get("/api/reports/" + saved.getId() + "/audit-log"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))));
    }
}