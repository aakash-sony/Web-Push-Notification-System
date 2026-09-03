package com.fcm.webpush.entity;

import java.time.Instant;

import com.fcm.webpush.enums.NotificationType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notification_masters")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationMaster {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, unique = true)
	private NotificationType code;

	@Column(nullable = false)
	private String title;

	@Column(nullable = false)
	private String bodyTemplate;

	@Builder.Default
	@Column(nullable = false)
	private boolean isActive = true;

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
