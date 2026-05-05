package com.internship.tool.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

/**
 * This is the data the frontend sends in the request body.
 * The @Valid annotations automatically reject bad data before it reaches the service.
 */
public class AuditReportRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title cannot exceed 255 characters")
    private String title;

    private String description;

    @Pattern(regexp = "PENDING|IN_REVIEW|COMPLETED|REJECTED",
            message = "Status must be PENDING, IN_REVIEW, COMPLETED, or REJECTED")
    private String status;

    @Min(value = 0, message = "Score must be at least 0")
    @Max(value = 100, message = "Score cannot exceed 100")
    private Integer score;

    private String category;
    private String assignedTo;
    private LocalDate dueDate;

    // ─── Getters and Setters ───────────────────────────────────────────────

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
}