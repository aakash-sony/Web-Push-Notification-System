package com.fcm.webpush.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.RequestParam;

import com.fcm.webpush.dto.request.UserLoginRequestDto;
import com.fcm.webpush.dto.request.UserRegistrationRequestDto;
import com.fcm.webpush.dto.response.UserResponseDto;
import com.fcm.webpush.service.NotificationService;
import com.fcm.webpush.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;
	private final NotificationService notificationService;

	@PostMapping("/register")
	public ResponseEntity<UserResponseDto> registerUser(@Valid @RequestBody final UserRegistrationRequestDto request) {
		final var response = userService.registerUser(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@PostMapping("/login")
	public ResponseEntity<UserResponseDto> loginUser(@Valid @RequestBody final UserLoginRequestDto request) {
		final var response = userService.loginUser(request);
		return ResponseEntity.ok(response);
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logoutUser(
			@RequestParam(required = false) final String guestId,
			@RequestParam(required = false) final String fcmToken) {
		notificationService.detachUserFromSubscription(guestId, fcmToken);
		return ResponseEntity.ok().build();
	}
}
