package com.fcm.webpush.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
	name = "notification_schedule_executions",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_sched_exec_idemp",
			columnNames = {"schedule_id", "recipient_type", "recipient_id", "occurrence_key"}
		)
	},
	indexes = {
		@Index(
			name = "idx_sched_exec_lookup",
			columnList = "schedule_id, recipient_type, recipient_id, occurrence_key"
		)
	}
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationScheduleExecution {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "schedule_id", nullable = false)
	private Long scheduleId;

	@Column(name = "recipient_type", nullable = false)
	private String recipientType;

	@Column(name = "recipient_id", nullable = false)
	private String recipientId;

	@Column(name = "occurrence_key", nullable = false)
	private String occurrenceKey;

	@Column(name = "notification_log_id")
	private Long notificationLogId;

	@Column(name = "executed_at", nullable = false)
	private Instant executedAt;

	@Column(name = "status", nullable = false)
	private String status;

	@PrePersist
	public void onCreate() {
		if (executedAt == null) {
			executedAt = Instant.now();
		}
	}
}
