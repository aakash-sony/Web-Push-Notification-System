package com.fcm.webpush.dto.request;

import java.time.LocalTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fcm.webpush.enums.ScheduleType;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationScheduleRequestDto {

	@NotNull(message = "Template ID is required")
	private Long templateId;

	@NotNull(message = "Schedule type is required")
	private ScheduleType scheduleType;

	@JsonFormat(pattern = "HH:mm")
	private LocalTime timeOfDay;

	private List<Integer> offsets;

	private Integer intervalMinutes;

	@Builder.Default
	private Boolean isActive = true;
}
