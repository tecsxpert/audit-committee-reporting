package com.internship.tool.repository;

import com.internship.tool.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Database operations for the audit_log table.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Returns all audit log entries for a specific record.
     * Example: "show me all changes ever made to AuditReport #42"
     */
    List<AuditLog> findByEntityTypeAndEntityIdOrderByPerformedAtDesc(
            String entityType, Long entityId);

    /**
     * Returns all actions performed by a specific user.
     */
    List<AuditLog> findByPerformedByOrderByPerformedAtDesc(String performedBy);
}