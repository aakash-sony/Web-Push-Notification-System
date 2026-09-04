package com.fcm.webpush.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.fcm.webpush.dto.request.AssociateGuestRequestDto;
import com.fcm.webpush.dto.request.SubscriptionRequestDto;
import com.fcm.webpush.dto.response.NotificationLogResponseDto;
import com.fcm.webpush.dto.response.SubscriptionResponseDto;
import com.fcm.webpush.dto.response.UnreadCountResponseDto;
import com.fcm.webpush.service.NotificationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/notifications")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@RequiredArgsConstructor
public class NotificationController {

	private final NotificationService notificationService;

	@PostMapping("/subscriptions")
	public ResponseEntity<SubscriptionResponseDto> registerOrRefreshSubscription(
			@Valid @RequestBody final SubscriptionRequestDto request,
			@org.springframework.web.bind.annotation.RequestHeader(value = "X-User-Username", required = false) final String authenticatedUsername) {
		final String targetUserId = (authenticatedUsername != null && !authenticatedUsername.isBlank())
				? authenticatedUsername
				: (request.getUserId() != null && !request.getUserId().isBlank() ? request.getUserId() : null);
		final var response = notificationService.registerOrRefreshSubscription(request, targetUserId);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/subscriptions/check")
	public ResponseEntity<Boolean> checkSubscriptionExists(
			@RequestParam(required = false) final String guestId,
			@RequestParam(required = false) final String fcmToken) {
		final boolean exists = notificationService.checkSubscriptionExists(guestId, fcmToken);
		return ResponseEntity.ok(exists);
	}

	@PostMapping("/subscriptions/disassociate")
	public ResponseEntity<Void> disassociateSubscription(
			@RequestParam(required = false) final String guestId,
			@RequestParam(required = false) final String fcmToken) {
		notificationService.detachUserFromSubscription(guestId, fcmToken);
		return ResponseEntity.ok().build();
	}

	@PatchMapping("/subscriptions/associate")
	public ResponseEntity<Void> associateGuestWithUser(@Valid @RequestBody final AssociateGuestRequestDto request, @RequestParam(required = false) final String userId) {
		final var targetUserId = request.getUserId() != null && !request.getUserId().isBlank() ? request.getUserId() : userId;
		if (targetUserId == null || targetUserId.isBlank())
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User ID is required for association");
		notificationService.associateGuestWithUser(request.getGuestId(), targetUserId);
		return ResponseEntity.ok().build();
	}

	@GetMapping("/user/{userId}")
	public ResponseEntity<Page<NotificationLogResponseDto>> getUserNotifications(@PathVariable final String userId, @RequestParam(defaultValue = "0") final int page, @RequestParam(defaultValue = "20") final int size) {
		final var pageable = PageRequest.of(page, size);
		final var response = notificationService.getUserNotifications(userId, pageable);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/guest/{guestId}")
	public ResponseEntity<Page<NotificationLogResponseDto>> getGuestNotifications(@PathVariable final String guestId, @RequestParam(defaultValue = "0") final int page, @RequestParam(defaultValue = "20") final int size) {
		final var pageable = PageRequest.of(page, size);
		final var response = notificationService.getGuestNotifications(guestId, pageable);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/user/{userId}/unread-count")
	public ResponseEntity<UnreadCountResponseDto> getUserUnreadCount(@PathVariable final String userId) {
		final var response = notificationService.getUserUnreadCount(userId);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/guest/{guestId}/unread-count")
	public ResponseEntity<UnreadCountResponseDto> getGuestUnreadCount(@PathVariable final String guestId) {
		final var response = notificationService.getGuestUnreadCount(guestId);
		return ResponseEntity.ok(response);
	}

	@PatchMapping("/{notificationId}/read")
	public ResponseEntity<NotificationLogResponseDto> markAsRead(@PathVariable final Long notificationId, @RequestParam(required = false) final String userId, @RequestParam(required = false) final String guestId) {
		if (userId == null && guestId == null)
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Requester userId or guestId is required");

		final var response = notificationService.markAsRead(notificationId, userId, guestId);
		return ResponseEntity.ok(response);
	}
}
