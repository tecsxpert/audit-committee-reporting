package com.internship.tool.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * This is what the backend sends BACK to the frontend.
 * Never expose the Entity directly — always convert to a Response DTO.
 */
public class AuditReportResponse {

    private Long id;
    private String title;
    private String description;
    private String status;
    private Integer score;
    private String category;
    private String assignedTo;
    private LocalDate dueDate;
    private String aiDescription;
    private String aiRecommendations;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

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
    public void setAiDescription(String d) { this.aiDescription = d; }
    public String getAiRecommendations() { return aiRecommendations; }
    public void setAiRecommendations(String r) { this.aiRecommendations = r; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}