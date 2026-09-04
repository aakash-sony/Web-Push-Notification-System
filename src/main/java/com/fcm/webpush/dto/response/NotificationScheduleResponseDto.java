package com.fcm.webpush.dto.response;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fcm.webpush.enums.ScheduleType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationScheduleResponseDto {

	private Long id;
	private Long templateId;
	private String templateTitle;
	private ScheduleType scheduleType;

	@JsonFormat(pattern = "HH:mm")
	private LocalTime timeOfDay;

	private List<Integer> offsets;
	private Integer intervalMinutes;
	private boolean isActive;
	private Instant createdAt;
	private Instant updatedAt;
}
