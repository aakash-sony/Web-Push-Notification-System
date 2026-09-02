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
@Table(name = "notification_subscriptions", indexes = {
		@Index(name = "idx_sub_user_id", columnList = "userId"),
		@Index(name = "idx_sub_guest_id", columnList = "guestId")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationSubscription {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String guestId;

	private String userId;

	@Column(nullable = false, unique = true)
	private String fcmToken;

	private String deviceType;

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
