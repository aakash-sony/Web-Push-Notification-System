package com.fcm.webpush.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notification_logs", indexes = {
		@Index(name = "idx_notif_log_user_id", columnList = "userId"),
		@Index(name = "idx_notif_log_guest_id", columnList = "guestId")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String userId;

	private String guestId;

	private Long templateId;

	@Column(nullable = false)
	private String title;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String body;

	private String code;

	@Builder.Default
	@Column(nullable = false)
	private boolean isRead = false;

	private Instant readAt;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	@PrePersist
	public void onCreate() {
		final Instant now = Instant.now();
		if (createdAt == null)
			createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	public void onUpdate() {
		updatedAt = Instant.now();
	}
}
