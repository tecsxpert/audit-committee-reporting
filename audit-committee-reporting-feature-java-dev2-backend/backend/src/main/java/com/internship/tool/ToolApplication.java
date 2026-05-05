package com.internship.tool;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * This is the entry point of the entire backend application.
 * Run this class to start the server on port 8080.
 *
 * @SpringBootApplication  — enables auto-configuration, component scanning
 * @EnableJpaAuditing      — enables @CreatedDate and @LastModifiedDate in entities
 * @EnableCaching          — enables @Cacheable and @CacheEvict annotations
 * @EnableScheduling       — enables @Scheduled cron jobs in ReminderScheduler
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableCaching
@EnableScheduling
public class ToolApplication {

	public static void main(String[] args) {
		SpringApplication.run(ToolApplication.class, args);
	}
}