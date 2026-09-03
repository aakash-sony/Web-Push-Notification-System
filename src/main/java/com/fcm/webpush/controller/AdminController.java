package com.fcm.webpush.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fcm.webpush.dto.response.GuestResponseDto;
import com.fcm.webpush.dto.response.NotificationTemplateResponseDto;
import com.fcm.webpush.dto.response.UserResponseDto;
import com.fcm.webpush.service.AdminService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

	private final AdminService adminService;

	@GetMapping("/guests")
	public ResponseEntity<List<GuestResponseDto>> getAllGuests(final HttpSession session) {
		adminService.verifyAdminAuthorization(session);
		final var guests = adminService.getAllGuests();
		return ResponseEntity.ok(guests);
	}

	@GetMapping("/users")
	public ResponseEntity<List<UserResponseDto>> getAllUsers(final HttpSession session) {
		adminService.verifyAdminAuthorization(session);
		final var users = adminService.getAllUsers();
		return ResponseEntity.ok(users);
	}

	@GetMapping("/notifications/templates")
	public ResponseEntity<List<NotificationTemplateResponseDto>> getAllNotificationTemplates(final HttpSession session) {
		adminService.verifyAdminAuthorization(session);
		final var templates = adminService.getAllNotificationTemplates();
		return ResponseEntity.ok(templates);
	}
}
