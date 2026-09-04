package com.fcm.webpush.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@RequiredArgsConstructor
public class AdminController {

	private final AdminService 				adminService;
	private final NotificationService 		notificationService;

	@GetMapping("/guests")
	public ResponseEntity<List<GuestResponseDto>> getAllGuests(@RequestHeader(value = "X-User-Username", required = false) final String username) {
		adminService.verifyAdminAuthorization(username);
		final var guests = adminService.getAllGuests();
		return ResponseEntity.ok(guests);
	}

	@GetMapping("/users")
	public ResponseEntity<List<UserResponseDto>> getAllUsers(@RequestHeader(value = "X-User-Username", required = false) final String username) {
		adminService.verifyAdminAuthorization(username);
		final var users = adminService.getAllUsers();
		return ResponseEntity.ok(users);
	}

	@GetMapping("/notifications/templates")
	public ResponseEntity<List<NotificationTemplateResponseDto>> getAllNotificationTemplates(@RequestHeader(value = "X-User-Username", required = false) final String username) {
		adminService.verifyAdminAuthorization(username);
		final var templates = adminService.getAllNotificationTemplates();
		return ResponseEntity.ok(templates);
	}

	@PostMapping("/notifications/send")
	public ResponseEntity<SendNotificationResponseDto> sendNotification(@Valid @RequestBody final SendNotificationRequestDto request, @RequestHeader(value = "X-User-Username", required = false) final String username) {
		adminService.verifyAdminAuthorization(username);
		final var response = notificationService.sendNotification(request);
		return ResponseEntity.ok(response);
	}
}
