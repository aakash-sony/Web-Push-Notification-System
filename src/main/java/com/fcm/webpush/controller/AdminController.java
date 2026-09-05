package com.fcm.webpush.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fcm.webpush.dto.request.SendNotificationRequestDto;
import com.fcm.webpush.dto.response.GuestResponseDto;
import com.fcm.webpush.dto.response.NotificationTemplateResponseDto;
import com.fcm.webpush.dto.response.SendNotificationResponseDto;
import com.fcm.webpush.dto.response.UserResponseDto;
import com.fcm.webpush.service.AdminService;
import com.fcm.webpush.service.NotificationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

	private final AdminService 				adminService;
	private final NotificationService 		notificationService;

	@GetMapping("/guests")
	public ResponseEntity<List<GuestResponseDto>> getAllGuests() {
		adminService.verifyAdminAuthorization();
		final var guests = adminService.getAllGuests();
		return ResponseEntity.ok(guests);
	}

	@GetMapping("/users")
	public ResponseEntity<List<UserResponseDto>> getAllUsers() {
		adminService.verifyAdminAuthorization();
		final var users = adminService.getAllUsers();
		return ResponseEntity.ok(users);
	}

	@GetMapping("/notifications/templates")
	public ResponseEntity<List<NotificationTemplateResponseDto>> getAllNotificationTemplates() {
		adminService.verifyAdminAuthorization();
		final var templates = adminService.getAllNotificationTemplates();
		return ResponseEntity.ok(templates);
	}

	@PostMapping("/notifications/send")
	public ResponseEntity<SendNotificationResponseDto> sendNotification(@Valid @RequestBody final SendNotificationRequestDto request) {
		adminService.verifyAdminAuthorization();
		final var response = notificationService.sendNotification(request);
		return ResponseEntity.ok(response);
	}
}
