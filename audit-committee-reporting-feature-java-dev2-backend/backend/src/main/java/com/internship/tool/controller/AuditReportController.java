package com.internship.tool.controller;

import com.internship.tool.dto.AuditReportRequest;
import com.internship.tool.dto.AuditReportResponse;
import com.internship.tool.service.AuditReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

/**
 * REST Controller for Java Developer-2's endpoints.
 * Handles: update, soft-delete, search, stats, export.
 * Base URL: http://localhost:8080/api/reports
 */
@RestController
@RequestMapping("/api/reports")
@Tag(name = "Audit Reports", description = "CRUD and search for audit committee reports")
public class AuditReportController {

    private final AuditReportService service;

    // Spring automatically injects the service (dependency injection)
    public AuditReportController(AuditReportService service) {
        this.service = service;
    }

    /**
     * PUT /api/reports/{id}
     * Updates an existing report. Only ADMIN and MANAGER roles allowed.
     * Returns 200 OK with updated report, or 404 if not found.
     */
    @Operation(summary = "Update a report", responses = {
            @ApiResponse(responseCode = "200", description = "Updated successfully"),
            @ApiResponse(responseCode = "404", description = "Report not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<AuditReportResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody AuditReportRequest request,
            Principal principal) {              // Principal = the logged-in user

        AuditReportResponse updated = service.update(id, request, principal.getName());
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/reports/{id}
     * Soft-deletes a report (sets isDeleted=true, does NOT remove from DB).
     * Only ADMIN role allowed.
     */
    @Operation(summary = "Soft-delete a report")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            Principal principal) {

        service.softDelete(id, principal.getName());
        return ResponseEntity.noContent().build();   // 204 No Content
    }

    /**
     * GET /api/reports/search?q=keyword&page=0&size=10&sortBy=createdAt&sortDir=desc
     * Searches reports by keyword across title, description, and category.
     */
    @Operation(summary = "Search reports by keyword")
    @GetMapping("/search")
    public ResponseEntity<Page<AuditReportResponse>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "0")         int page,
            @RequestParam(defaultValue = "10")        int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc")      String sortDir) {

        return ResponseEntity.ok(service.search(q, page, size, sortBy, sortDir));
    }

    /**
     * GET /api/reports/stats
     * Returns dashboard KPI numbers: total count, count by status, average score.
     * Example response: { "total": 30, "PENDING": 10, "COMPLETED": 15, "avgScore": 72.5 }
     */
    @Operation(summary = "Get dashboard statistics")
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(service.getStats());
    }

    /**
     * GET /api/reports/export
     * Downloads all reports as a CSV file.
     * The browser will show a "Save File" dialog.
     */
    @Operation(summary = "Export all reports as CSV")
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCsv() {
        byte[] csv = service.exportToCsv();

        return ResponseEntity.ok()
                .header("Content-Type", "text/csv")
                .header("Content-Disposition", "attachment; filename=audit_reports.csv")
                .body(csv);
    }

    /**
     * GET /api/reports?page=0&size=10&sortBy=createdAt&sortDir=desc&status=PENDING
     * Lists all reports with pagination, sorting, and optional status filter.
     */
    @Operation(summary = "List all reports with pagination")
    @GetMapping
    public ResponseEntity<Page<AuditReportResponse>> listAll(
            @RequestParam(defaultValue = "0")         int page,
            @RequestParam(defaultValue = "10")        int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc")      String sortDir,
            @RequestParam(required = false)           String status) {

        return ResponseEntity.ok(service.findAll(page, size, sortBy, sortDir, status));
    }

    /**
     * GET /api/reports/{id}/audit-log
     * Returns the full history of changes made to a specific report.
     */
    @Operation(summary = "Get audit log for a report")
    @GetMapping("/{id}/audit-log")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> getAuditLog(@PathVariable Long id) {
        return ResponseEntity.ok(service.getAuditLog(id));
    }
}