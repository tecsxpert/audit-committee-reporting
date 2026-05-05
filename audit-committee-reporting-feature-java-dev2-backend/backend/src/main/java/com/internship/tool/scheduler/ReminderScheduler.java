package com.internship.tool.scheduler;

import com.internship.tool.entity.AuditReport;
import com.internship.tool.repository.AuditReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.mail.internet.MimeMessage;
import java.time.LocalDate;
import java.util.List;

/**
 * This class runs automatic jobs on a schedule.
 * No human needs to trigger these — Spring runs them automatically.
 * @EnableScheduling must be added to your main application class.
 */
@Component
public class ReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);

    private final AuditReportRepository reportRepo;
    private final JavaMailSender        mailSender;

    public ReminderScheduler(AuditReportRepository reportRepo, JavaMailSender mailSender) {
        this.reportRepo = reportRepo;
        this.mailSender = mailSender;
    }

    /**
     * Runs every day at 8:00 AM.
     * Finds all overdue PENDING reports and logs them.
     * In production: would also send emails to the assignee.
     *
     * Cron format: "second minute hour day month weekday"
     * "0 0 8 * * *" = at 8:00:00 every day
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void sendOverdueReminders() {
        log.info("Running overdue reminder job...");

        List<AuditReport> overdue = reportRepo.findOverdueReports(LocalDate.now());

        if (overdue.isEmpty()) {
            log.info("No overdue reports found.");
            return;
        }

        for (AuditReport report : overdue) {
            log.warn("OVERDUE: Report '{}' (ID:{}) was due on {}",
                    report.getTitle(), report.getId(), report.getDueDate());

            // Send email if assignedTo has an email address
            if (report.getAssignedTo() != null && report.getAssignedTo().contains("@")) {
                sendOverdueEmail(report);
            }
        }

        log.info("Overdue reminder job completed. {} reports flagged.", overdue.size());
    }

    /**
     * Runs every day at 9:00 AM.
     * Alerts for reports due within the next 7 days.
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void sendUpcomingDeadlineAlerts() {
        log.info("Running upcoming deadline alert job...");

        LocalDate today         = LocalDate.now();
        LocalDate sevenDaysOut  = today.plusDays(7);

        List<AuditReport> upcoming = reportRepo.findUpcomingDeadlines(today, sevenDaysOut);

        for (AuditReport report : upcoming) {
            log.info("UPCOMING: Report '{}' (ID:{}) is due on {}",
                    report.getTitle(), report.getId(), report.getDueDate());
        }

        log.info("Upcoming deadline job done. {} reports approaching deadline.", upcoming.size());
    }

    /**
     * Runs every Monday at 7:00 AM.
     * Logs a weekly summary of all overdue reports.
     * "0 0 7 * * MON" = every Monday at 07:00:00
     */
    @Scheduled(cron = "0 0 7 * * MON")
    public void sendWeeklySummary() {
        log.info("=== WEEKLY SUMMARY ===");
        List<AuditReport> overdue = reportRepo.findOverdueReports(LocalDate.now());
        log.info("Total overdue reports this week: {}", overdue.size());
    }

    /**
     * Sends an HTML email to the report's assignee notifying them it is overdue.
     */
    private void sendOverdueEmail(AuditReport report) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(report.getAssignedTo());
            helper.setSubject("ACTION REQUIRED: Overdue Audit Report — " + report.getTitle());
            helper.setText(buildOverdueEmailHtml(report), true);  // true = HTML

            mailSender.send(message);
            log.info("Overdue email sent to {}", report.getAssignedTo());

        } catch (Exception e) {
            // Never crash the scheduler because of an email failure
            log.error("Failed to send overdue email for report {}: {}", report.getId(), e.getMessage());
        }
    }

    /**
     * Builds the HTML content for the overdue email.
     */
    private String buildOverdueEmailHtml(AuditReport report) {
        return """
                <html>
                <body style="font-family: Arial, sans-serif; color: #333;">
                  <h2 style="color: #1B4F8A;">Audit Committee Reporting — Action Required</h2>
                  <p>The following audit report is <strong style="color: red;">OVERDUE</strong>:</p>
                  <table border="1" cellpadding="8" cellspacing="0" style="border-collapse: collapse;">
                    <tr><td><strong>Title</strong></td><td>%s</td></tr>
                    <tr><td><strong>Status</strong></td><td>%s</td></tr>
                    <tr><td><strong>Due Date</strong></td><td>%s</td></tr>
                    <tr><td><strong>Category</strong></td><td>%s</td></tr>
                  </table>
                  <p>Please log in to the system and update this report immediately.</p>
                  <p>— Audit Committee Reporting System</p>
                </body>
                </html>
                """.formatted(
                report.getTitle(),
                report.getStatus(),
                report.getDueDate(),
                report.getCategory() != null ? report.getCategory() : "N/A"
        );
    }
}