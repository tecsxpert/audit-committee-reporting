package com.internship.tool.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * This class represents one row in the audit_report table.
 * Each field here matches a column in the database.
 */
@Entity
@Table(name = "audit_report")
@EntityListeners(AuditingEntityListener.class)   // auto-fills createdAt and updatedAt
public class AuditReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String status = "PENDING";   // default value

    private Integer score;

    private String category;

    private String assignedTo;

    private LocalDate dueDate;

    @Column(columnDefinition = "TEXT")
    private String aiDescription;        // filled by AI service

    @Column(columnDefinition = "TEXT")
    private String aiRecommendations;    // filled by AI service

    @Column(nullable = false)
    private Boolean isDeleted = false;   // soft delete flag

    private String createdBy;
    private String updatedBy;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;     // set automatically when record is created

    @LastModifiedDate
    private LocalDateTime updatedAt;     // updated automatically on every save

    // ─── Getters and Setters ───────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public String getAiDescription() { return aiDescription; }
    public void setAiDescription(String aiDescription) { this.aiDescription = aiDescription; }

    public String getAiRecommendations() { return aiRecommendations; }
    public void setAiRecommendations(String aiRecommendations) { this.aiRecommendations = aiRecommendations; }

    public Boolean getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}



