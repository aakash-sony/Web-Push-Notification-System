package com.fcm.webpush.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.fcm.webpush.dto.request.AssociateGuestRequestDto;
import com.fcm.webpush.dto.request.SubscriptionRequestDto;
import com.fcm.webpush.dto.response.SubscriptionResponseDto;
import com.fcm.webpush.service.NotificationService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

	private final NotificationService notificationService;

	@PostMapping("/subscriptions")
	public ResponseEntity<SubscriptionResponseDto> registerOrRefreshSubscription(@Valid @RequestBody final SubscriptionRequestDto request) {
		final var response = notificationService.registerOrRefreshSubscription(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@PatchMapping("/subscriptions/associate")
	public ResponseEntity<Void> associateGuestWithUser(@Valid @RequestBody final AssociateGuestRequestDto request, final HttpSession session) {
		final var userIdObj = session.getAttribute("USER_ID");
		if (userIdObj == null)
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated");
		notificationService.associateGuestWithUser(request.getGuestId(), userIdObj.toString());
		return ResponseEntity.ok().build();
	}
}
