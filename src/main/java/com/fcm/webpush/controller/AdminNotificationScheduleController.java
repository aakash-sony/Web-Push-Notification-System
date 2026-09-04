package com.fcm.webpush.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fcm.webpush.dto.request.NotificationScheduleRequestDto;
import com.fcm.webpush.dto.response.NotificationScheduleResponseDto;
import com.fcm.webpush.service.AdminService;
import com.fcm.webpush.service.NotificationScheduleService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/notification-schedules")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@RequiredArgsConstructor
public class AdminNotificationScheduleController {

	private final AdminService adminService;
	private final NotificationScheduleService notificationScheduleService;

	@PostMapping
	public ResponseEntity<NotificationScheduleResponseDto> createSchedule(@Valid @RequestBody final NotificationScheduleRequestDto request, @RequestHeader(value = "X-User-Username", required = false) final String username) {
		adminService.verifyAdminAuthorization(username);
		final var response = notificationScheduleService.createSchedule(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping
	public ResponseEntity<List<NotificationScheduleResponseDto>> getAllSchedules(@RequestHeader(value = "X-User-Username", required = false) final String username) {
		adminService.verifyAdminAuthorization(username);
		final var schedules = notificationScheduleService.getAllSchedules();
		return ResponseEntity.ok(schedules);
	}

	@GetMapping("/{id}")
	public ResponseEntity<NotificationScheduleResponseDto> getScheduleById(@PathVariable final Long id, @RequestHeader(value = "X-User-Username", required = false) final String username) {
		adminService.verifyAdminAuthorization(username);
		final var schedule = notificationScheduleService.getScheduleById(id);
		return ResponseEntity.ok(schedule);
	}

	@PutMapping("/{id}")
	public ResponseEntity<NotificationScheduleResponseDto> updateSchedule(@PathVariable final Long id, @Valid @RequestBody final NotificationScheduleRequestDto request, @RequestHeader(value = "X-User-Username", required = false) final String username) {
		adminService.verifyAdminAuthorization(username);
		final var response = notificationScheduleService.updateSchedule(id, request);
		return ResponseEntity.ok(response);
	}

	@PatchMapping("/{id}/status")
	public ResponseEntity<NotificationScheduleResponseDto> updateScheduleStatus(@PathVariable final Long id, @RequestParam final boolean active, @RequestHeader(value = "X-User-Username", required = false) final String username) {
		adminService.verifyAdminAuthorization(username);
		final var response = notificationScheduleService.updateScheduleStatus(id, active);
		return ResponseEntity.ok(response);
	}
}
