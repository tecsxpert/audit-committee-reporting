package com.internship.tool.repository;

import com.internship.tool.entity.AuditReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * This interface handles all database operations for AuditReport.
 * Spring automatically generates the SQL — you just define what you want.
 */
@Repository
public interface AuditReportRepository extends JpaRepository<AuditReport, Long> {

    /**
     * Finds a report by ID but only if it has NOT been soft-deleted.
     * Use this instead of findById so deleted records are invisible.
     */
    Optional<AuditReport> findByIdAndIsDeletedFalse(Long id);

    /**
     * Returns all non-deleted reports, with pagination support.
     * Pageable lets the caller request page 1, 2, 3... with 10 items each.
     */
    Page<AuditReport> findByIsDeletedFalse(Pageable pageable);

    /**
     * Filters reports by status (e.g. "PENDING") with pagination.
     */
    Page<AuditReport> findByStatusAndIsDeletedFalse(String status, Pageable pageable);

    /**
     * Finds reports whose due date is between two dates.
     * Used for the overdue scheduler and date-range filters.
     */
    @Query("SELECT r FROM AuditReport r WHERE r.isDeleted = false " +
            "AND r.dueDate BETWEEN :startDate AND :endDate")
    List<AuditReport> findByDueDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate")   LocalDate endDate);

    /**
     * Full-text search: searches title, description, and category for a keyword.
     * LOWER() makes the search case-insensitive.
     */
    @Query("SELECT r FROM AuditReport r WHERE r.isDeleted = false AND (" +
            "LOWER(r.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(r.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(r.category) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<AuditReport> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Finds all overdue reports — due date has passed and status is still PENDING.
     * Used by the scheduler to send reminder emails.
     */
    @Query("SELECT r FROM AuditReport r WHERE r.isDeleted = false " +
            "AND r.dueDate < :today AND r.status = 'PENDING'")
    List<AuditReport> findOverdueReports(@Param("today") LocalDate today);

    /**
     * Finds reports due within the next 7 days (upcoming deadline alerts).
     */
    @Query("SELECT r FROM AuditReport r WHERE r.isDeleted = false " +
            "AND r.dueDate BETWEEN :today AND :sevenDaysLater AND r.status = 'PENDING'")
    List<AuditReport> findUpcomingDeadlines(
            @Param("today") LocalDate today,
            @Param("sevenDaysLater") LocalDate sevenDaysLater);

    /**
     * Counts how many non-deleted reports exist for each status.
     * Returns pairs like: ["PENDING", 12], ["COMPLETED", 8]
     * Used for the dashboard KPI stats.
     */
    @Query("SELECT r.status, COUNT(r) FROM AuditReport r WHERE r.isDeleted = false GROUP BY r.status")
    List<Object[]> countByStatus();

    /**
     * Returns all non-deleted reports as a simple list (no pagination).
     * Used for CSV export — we need everything, not just one page.
     */
    @Query("SELECT r FROM AuditReport r WHERE r.isDeleted = false ORDER BY r.createdAt DESC")
    List<AuditReport> findAllForExport();

    /**
     * Counts all non-deleted reports.
     * Used for dashboard statistics.
     */
    long countByIsDeletedFalse();
}