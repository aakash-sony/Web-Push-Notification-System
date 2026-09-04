package com.fcm.webpush.service;

import java.util.List;

import com.fcm.webpush.dto.request.NotificationScheduleRequestDto;
import com.fcm.webpush.dto.response.NotificationScheduleResponseDto;

public interface NotificationScheduleService {

	NotificationScheduleResponseDto createSchedule(NotificationScheduleRequestDto request);

	List<NotificationScheduleResponseDto> getAllSchedules();

	NotificationScheduleResponseDto getScheduleById(Long id);

	NotificationScheduleResponseDto updateSchedule(Long id, NotificationScheduleRequestDto request);

	NotificationScheduleResponseDto updateScheduleStatus(Long id, boolean active);

	void processDueSchedules();
}
