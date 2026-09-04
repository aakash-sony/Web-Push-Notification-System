package com.fcm.webpush.entity;

import java.time.Instant;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.fcm.webpush.enums.ScheduleType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notification_schedules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationSchedule {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "template_id", nullable = false)
	private NotificationMaster template;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ScheduleType scheduleType;

	@Column(name = "time_of_day")
	private LocalTime timeOfDay;

	@Column(name = "offsets")
	private String offsets;

	@Column(name = "interval_minutes")
	private Integer intervalMinutes;

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
		if (createdAt == null) {
			createdAt = now;
		}
		updatedAt = now;
	}

	@PreUpdate
	public void onUpdate() {
		updatedAt = Instant.now();
	}

	public List<Integer> getParsedOffsets() {
		if (offsets == null || offsets.trim().isEmpty()) {
			return Collections.emptyList();
		}
		return Arrays.stream(offsets.split(","))
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.map(Integer::parseInt)
				.collect(Collectors.toList());
	}

	public void setParsedOffsets(final List<Integer> offsetList) {
		if (offsetList == null || offsetList.isEmpty()) {
			this.offsets = null;
		} else {
			this.offsets = offsetList.stream()
					.map(Object::toString)
					.collect(Collectors.joining(","));
		}
	}
}
