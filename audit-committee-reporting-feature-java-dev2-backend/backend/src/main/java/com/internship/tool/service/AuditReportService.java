package com.internship.tool.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.internship.tool.dto.AuditReportRequest;
import com.internship.tool.dto.AuditReportResponse;
import com.internship.tool.entity.AuditLog;
import com.internship.tool.entity.AuditReport;
import com.internship.tool.exception.ResourceNotFoundException;
import com.internship.tool.repository.AuditLogRepository;
import com.internship.tool.repository.AuditReportRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Contains all the business logic for audit reports.
 * The controller calls these methods — it does NOT do logic itself.
 */
@Service
public class AuditReportService {

    private final AuditReportRepository reportRepo;
    private final AuditLogRepository    logRepo;
    private final ObjectMapper          objectMapper;   // converts objects to JSON strings

    public AuditReportService(AuditReportRepository reportRepo,
                              AuditLogRepository logRepo,
                              ObjectMapper objectMapper) {
        this.reportRepo   = reportRepo;
        this.logRepo      = logRepo;
        this.objectMapper = objectMapper;
    }

    /**
     * Updates an existing report.
     * Saves the old values to audit_log before making changes.
     */
    @Transactional
    public AuditReportResponse update(Long id, AuditReportRequest request, String username) {

        // 1. Find the report (throws 404 if not found or already deleted)
        AuditReport existing = reportRepo.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with id: " + id));

        // 2. Save old values for the audit log
        String oldValue = toJson(existing);

        // 3. Apply the new values from the request
        existing.setTitle(request.getTitle());
        existing.setDescription(request.getDescription());
        if (request.getStatus() != null)   existing.setStatus(request.getStatus());
        if (request.getScore() != null)    existing.setScore(request.getScore());
        if (request.getCategory() != null) existing.setCategory(request.getCategory());
        existing.setAssignedTo(request.getAssignedTo());
        existing.setDueDate(request.getDueDate());
        existing.setUpdatedBy(username);

        // 4. Save to database
        AuditReport saved = reportRepo.save(existing);

        // 5. Write to audit log
        saveAuditLog("AuditReport", id, "UPDATE", oldValue, toJson(saved), username);

        return toResponse(saved);
    }

    /**
     * Soft-deletes a report: sets isDeleted=true instead of removing the row.
     * This means we never lose data — we can always restore it if needed.
     */
    @Transactional
    public void softDelete(Long id, String username) {

        AuditReport report = reportRepo.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with id: " + id));

        String oldValue = toJson(report);

        report.setIsDeleted(true);       // mark as deleted
        report.setUpdatedBy(username);
        reportRepo.save(report);

        saveAuditLog("AuditReport", id, "DELETE", oldValue, null, username);
    }

    /**
     * Searches reports by a keyword (title, description, or category).
     * Supports pagination and sorting.
     */
    public Page<AuditReportResponse> search(String keyword, int page, int size,
                                            String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        return reportRepo.searchByKeyword(keyword, pageable)
                .map(this::toResponse);
    }

    /**
     * Returns dashboard statistics: total count, count by each status, average score.
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();

        // Total number of non-deleted reports
        stats.put("total", reportRepo.countByIsDeletedFalse());

        // Count per status: [["PENDING", 10], ["COMPLETED", 15], ...]
        List<Object[]> statusCounts = reportRepo.countByStatus();
        for (Object[] row : statusCounts) {
            stats.put((String) row[0], row[1]);   // e.g. stats["PENDING"] = 10
        }

        // Calculate average score across all reports
        List<AuditReport> allReports = reportRepo.findAllForExport();
        OptionalDouble avg = allReports.stream()
                .filter(r -> r.getScore() != null)
                .mapToInt(AuditReport::getScore)
                .average();
        stats.put("avgScore", avg.isPresent() ? Math.round(avg.getAsDouble() * 10.0) / 10.0 : 0);

        return stats;
    }

    /**
     * Exports all reports as a CSV byte array.
     * The controller sends this as a file download.
     */
    public byte[] exportToCsv() {
        List<AuditReport> all = reportRepo.findAllForExport();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(out);

        // CSV header row
        writer.println("ID,Title,Status,Score,Category,AssignedTo,DueDate,CreatedBy,CreatedAt");

        // One CSV row per report
        for (AuditReport r : all) {
            writer.printf("%d,\"%s\",%s,%s,%s,%s,%s,%s,%s%n",
                    r.getId(),
                    r.getTitle().replace("\"", "\"\""),   // escape quotes
                    r.getStatus(),
                    r.getScore() != null ? r.getScore() : "",
                    r.getCategory() != null ? r.getCategory() : "",
                    r.getAssignedTo() != null ? r.getAssignedTo() : "",
                    r.getDueDate() != null ? r.getDueDate() : "",
                    r.getCreatedBy() != null ? r.getCreatedBy() : "",
                    r.getCreatedAt() != null ? r.getCreatedAt() : ""
            );
        }

        writer.flush();
        return out.toByteArray();
    }

    /**
     * Returns all reports with optional status filter and pagination.
     */
    public Page<AuditReportResponse> findAll(int page, int size, String sortBy,
                                             String sortDir, String status) {
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        if (status != null && !status.isBlank()) {
            return reportRepo.findByStatusAndIsDeletedFalse(status, pageable)
                    .map(this::toResponse);
        }
        return reportRepo.findByIsDeletedFalse(pageable).map(this::toResponse);
    }

    /**
     * Returns the full change history for a single report.
     */
    public List<AuditLog> getAuditLog(Long reportId) {
        return logRepo.findByEntityTypeAndEntityIdOrderByPerformedAtDesc("AuditReport", reportId);
    }

    // ─── Private Helper Methods ────────────────────────────────────────────

    /**
     * Saves a record to the audit_log table.
     * Called every time a report is created, updated, or deleted.
     */
    private void saveAuditLog(String entityType, Long entityId, String action,
                              String oldValue, String newValue, String username) {
        AuditLog log = new AuditLog();
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setAction(action);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        log.setPerformedBy(username);
        logRepo.save(log);
    }

    /**
     * Converts an AuditReport entity to a JSON string for audit logging.
     * Returns "null" string if conversion fails.
     */
    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }

    /**
     * Converts an AuditReport entity to an AuditReportResponse DTO.
     * We never expose the entity directly to the frontend.
     */
    public AuditReportResponse toResponse(AuditReport r) {
        AuditReportResponse res = new AuditReportResponse();
        res.setId(r.getId());
        res.setTitle(r.getTitle());
        res.setDescription(r.getDescription());
        res.setStatus(r.getStatus());
        res.setScore(r.getScore());
        res.setCategory(r.getCategory());
        res.setAssignedTo(r.getAssignedTo());
        res.setDueDate(r.getDueDate());
        res.setAiDescription(r.getAiDescription());
        res.setAiRecommendations(r.getAiRecommendations());
        res.setCreatedBy(r.getCreatedBy());
        res.setCreatedAt(r.getCreatedAt());
        res.setUpdatedAt(r.getUpdatedAt());
        return res;
    }
}