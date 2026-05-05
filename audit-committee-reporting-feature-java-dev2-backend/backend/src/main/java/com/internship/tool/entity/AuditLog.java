package com.internship.tool.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Represents one row in the audit_log table.
 * Every create/update/delete action saves a record here.
 */
@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String entityType;    // which table was affected, e.g. "AuditReport"
    private Long entityId;        // which row was affected, e.g. 42
    private String action;        // what happened: CREATE, UPDATE, DELETE

    @Column(columnDefinition = "TEXT")
    private String oldValue;      // JSON string of old data

    @Column(columnDefinition = "TEXT")
    private String newValue;      // JSON string of new data

    private String performedBy;   // who did this action
    private LocalDateTime performedAt = LocalDateTime.now();

    // ─── Getters and Setters ───────────────────────────────────────────────

    public Long getId() { return id; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getOldValue() { return oldValue; }
    public void setOldValue(String oldValue) { this.oldValue = oldValue; }
    public String getNewValue() { return newValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }
    public String getPerformedBy() { return performedBy; }
    public void setPerformedBy(String performedBy) { this.performedBy = performedBy; }
    public LocalDateTime getPerformedAt() { return performedAt; }
    public void setPerformedAt(LocalDateTime performedAt) { this.performedAt = performedAt; }
}