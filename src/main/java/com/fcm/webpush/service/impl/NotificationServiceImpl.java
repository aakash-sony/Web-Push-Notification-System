package com.fcm.webpush.service.impl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fcm.webpush.dto.request.SendNotificationRequestDto;
import com.fcm.webpush.dto.request.SubscriptionRequestDto;
import com.fcm.webpush.dto.response.NotificationLogResponseDto;
import com.fcm.webpush.dto.response.SendNotificationResponseDto;
import com.fcm.webpush.dto.response.SubscriptionResponseDto;
import com.fcm.webpush.dto.response.UnreadCountResponseDto;
import com.fcm.webpush.entity.NotificationLog;
import com.fcm.webpush.entity.NotificationSubscription;
import com.fcm.webpush.entity.User;
import com.fcm.webpush.repository.NotificationLogRepository;
import com.fcm.webpush.repository.NotificationMasterRepository;
import com.fcm.webpush.repository.NotificationSubscriptionRepository;
import com.fcm.webpush.repository.UserRepository;
import com.fcm.webpush.service.NotificationService;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

	private static final int FCM_BATCH_SIZE = 500;

	private final NotificationSubscriptionRepository 		subscriptionRepository;
	private final UserRepository 							userRepository;
	private final NotificationMasterRepository 				notificationMasterRepository;
	private final NotificationLogRepository 				notificationLogRepository;
	private final FirebaseMessaging 						firebaseMessaging;

	@Override
	@Transactional
	public SubscriptionResponseDto registerOrRefreshSubscription(final SubscriptionRequestDto request) {
		return registerOrRefreshSubscription(request, null);
	}

	@Override
	@Transactional
	public SubscriptionResponseDto registerOrRefreshSubscription(final SubscriptionRequestDto request, final String userId) {
		String targetGuestId = request.getGuestId();

		if (targetGuestId == null || targetGuestId.isBlank()) {
			targetGuestId = java.util.UUID.randomUUID().toString() + System.currentTimeMillis();
			log.info("EVENT=GUEST_CREATED No guestId provided in subscription request. Generated server guestId '{}'", targetGuestId);
		} else {
			final boolean guestExistsInDb = subscriptionRepository.existsByGuestId(targetGuestId);
			if (guestExistsInDb)
				log.debug("Reusing existing guestId '{}' found in database", targetGuestId);
			else
				log.info("EVENT=GUEST_CREATED Client provided new guestId '{}'. Registering new guest subscription", targetGuestId);
		}

		final String finalGuestId = targetGuestId;
		final String targetUserId = (userId != null && !userId.isBlank()) ? userId : null;

		final var subscription = subscriptionRepository.findByFcmToken(request.getFcmToken())
				.map(existing -> {
					final String oldToken = existing.getFcmToken();
					existing.setGuestId(finalGuestId);
					existing.setUserId(targetUserId); // Explicitly set targetUserId (null if guest)
					existing.setDeviceType(request.getDeviceType());
					existing.setActive(true);
					log.info("EVENT=SUBSCRIPTION_UPDATED Updated subscription (ID {}) for guestId '{}', userId '{}', safeToken '{}'",
							existing.getId(), finalGuestId, targetUserId, safeToken(oldToken));
					return existing;
				})
				.orElseGet(() -> {
					final List<NotificationSubscription> guestSubs = subscriptionRepository.findAllByGuestId(finalGuestId);
					if (!guestSubs.isEmpty()) {
						final NotificationSubscription existingGuestSub = guestSubs.get(0);
						final String oldToken = existingGuestSub.getFcmToken();
						existingGuestSub.setFcmToken(request.getFcmToken());
						existingGuestSub.setUserId(targetUserId);
						existingGuestSub.setDeviceType(request.getDeviceType());
						existingGuestSub.setActive(true);
						log.info("EVENT=TOKEN_CHANGED Refreshed FCM token on subscription (ID {}) for guestId '{}', userId '{}', oldToken '{}', newToken '{}'",
								existingGuestSub.getId(), finalGuestId, targetUserId, safeToken(oldToken), safeToken(request.getFcmToken()));
						return existingGuestSub;
					}

					log.info("EVENT=SUBSCRIPTION_REGISTERED Created new subscription for guestId '{}', userId '{}', safeToken '{}'",
							finalGuestId, targetUserId, safeToken(request.getFcmToken()));
					return NotificationSubscription.builder()
							.guestId(finalGuestId)
							.userId(targetUserId)
							.fcmToken(request.getFcmToken())
							.deviceType(request.getDeviceType())
							.build();
				});

		final var savedSubscription = subscriptionRepository.save(subscription);

		return mapToResponseDto(savedSubscription);
	}

	@Override
	@Transactional
	public void associateGuestWithUser(final String guestId, final String userId) {
		final var subscriptions = subscriptionRepository.findAllByGuestId(guestId);
		subscriptions.forEach(subscription -> subscription.setUserId(userId));
		subscriptionRepository.saveAll(subscriptions);
		log.info("EVENT=USER_ASSOCIATED Associated guestId '{}' with userId '{}' across {} subscriptions", guestId, userId, subscriptions.size());
	}

	@Override
	@Transactional(readOnly = true)
	public boolean checkSubscriptionExists(final String guestId, final String fcmToken) {
		if (guestId != null && !guestId.isBlank() && fcmToken != null && !fcmToken.isBlank()) {
			return subscriptionRepository.existsByGuestIdAndFcmToken(guestId, fcmToken);
		}
		if (guestId != null && !guestId.isBlank()) {
			return subscriptionRepository.existsByGuestId(guestId);
		}
		if (fcmToken != null && !fcmToken.isBlank()) {
			return subscriptionRepository.findByFcmToken(fcmToken).isPresent();
		}
		return false;
	}

	@Override
	@Transactional
	public void detachUserFromSubscription(final String guestId, final String fcmToken) {
		log.info("EVENT=LOGOUT Detach request received for guestId='{}', safeToken='{}'", guestId, safeToken(fcmToken));
		if (fcmToken != null && !fcmToken.isBlank()) {
			subscriptionRepository.findByFcmToken(fcmToken).ifPresent(sub -> {
				final String previousUserId = sub.getUserId();
				sub.setUserId(null);
				subscriptionRepository.save(sub);
				log.info("EVENT=USER_DISASSOCIATED Detached previous userId '{}' from subscription ID {} for safeToken '{}'",
						previousUserId, sub.getId(), safeToken(fcmToken));
			});
		} else if (guestId != null && !guestId.isBlank()) {
			final List<NotificationSubscription> subs = subscriptionRepository.findAllByGuestId(guestId);
			subs.forEach(sub -> sub.setUserId(null));
			subscriptionRepository.saveAll(subs);
			log.info("EVENT=USER_DISASSOCIATED Detached userId from {} subscriptions for guestId '{}'", subs.size(), guestId);
		}
	}

	private String safeToken(final String token) {
		if (token == null) return "null";
		if (token.length() <= 10) return "***";
		return token.substring(0, 6) + "..." + token.substring(token.length() - 4);
	}

	@Override
	public SendNotificationResponseDto sendNotification(final SendNotificationRequestDto request) {
		if (request == null || request.getTemplateId() == null)
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Template ID is required");

		final boolean hasUsers 		= request.getUserIds() != null && !request.getUserIds().isEmpty();
		final boolean hasGuests 	= request.getGuestIds() != null && !request.getGuestIds().isEmpty();

		if (!hasUsers && !hasGuests)
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one recipient (user or guest) must be selected");

		final var template 			= notificationMasterRepository.findById(request.getTemplateId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification template not found"));

		if (!template.isActive())
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Notification template is inactive");

		final List<Long> distinctUserIds = hasUsers
				? request.getUserIds().stream().distinct().toList()
						: List.of();

		final List<String> distinctGuestIds = hasGuests
				? request.getGuestIds().stream().distinct().toList()
						: List.of();

		if (!distinctUserIds.isEmpty()) {
			final List<User> foundUsers = userRepository.findAllById(distinctUserIds);
			if (foundUsers.size() < distinctUserIds.size()) {
				final Set<Long> foundIds 		= foundUsers.stream().map(User::getId).collect(Collectors.toSet());
				final List<Long> missingIds	 	= distinctUserIds.stream().filter(id -> !foundIds.contains(id)).toList();
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Some selected users do not exist: " + missingIds);
			}
		}

		if (!distinctGuestIds.isEmpty()) {
			final List<NotificationSubscription> guestSubs = subscriptionRepository.findByGuestIdIn(distinctGuestIds);
			final Set<String> foundGuestIds 	= guestSubs.stream().map(NotificationSubscription::getGuestId).collect(Collectors.toSet());
			final List<String> missingGuestIds 	= distinctGuestIds.stream().filter(id -> !foundGuestIds.contains(id)).toList();
			if (!missingGuestIds.isEmpty())
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Some selected guests do not exist: " + missingGuestIds);
		}

		final List<NotificationLog> logsToSave = new ArrayList<>();

		final String templateCodeStr 			= template.getCode() != null ? template.getCode().name() : null;

		for (final Long uId : distinctUserIds)
			logsToSave.add(NotificationLog.builder()
					.userId(String.valueOf(uId))
					.templateId(template.getId())
					.title(template.getTitle())
					.body(template.getBodyTemplate())
					.code(templateCodeStr)
					.isRead(false)
					.createdAt(Instant.now())
					.build());

		for (final String gId : distinctGuestIds)
			logsToSave.add(NotificationLog.builder()
					.guestId(gId)
					.templateId(template.getId())
					.title(template.getTitle())
					.body(template.getBodyTemplate())
					.code(templateCodeStr)
					.isRead(false)
					.createdAt(Instant.now())
					.build());

		if (!logsToSave.isEmpty()) {
			notificationLogRepository.saveAll(logsToSave);
			log.info("Persisted {} notification log records for history", logsToSave.size());
		}

		final Set<String> tokensToNotify = new HashSet<>();

		if (!distinctUserIds.isEmpty()) {
			final List<String> userIdsStr 					= distinctUserIds.stream().map(String::valueOf).toList();
			final List<NotificationSubscription> userSubs 	= subscriptionRepository.findByUserIdInAndIsActiveTrue(userIdsStr);
			userSubs.forEach(sub -> tokensToNotify.add(sub.getFcmToken()));
		}

		if (!distinctGuestIds.isEmpty()) {
			final List<NotificationSubscription> guestSubs = subscriptionRepository.findByGuestIdInAndIsActiveTrue(distinctGuestIds);
			guestSubs.forEach(sub -> tokensToNotify.add(sub.getFcmToken()));
		}

		log.info("Notification send requested. Template ID: {}, Users selected: {}, Guests selected: {}, Tokens found: {}",
				template.getId(), distinctUserIds.size(), distinctGuestIds.size(), tokensToNotify.size());

		if (tokensToNotify.isEmpty())
			return SendNotificationResponseDto.builder()
					.message("No active FCM tokens found for selected recipients")
					.status("NO_TOKENS_FOUND")
					.templateId(template.getId())
					.usersSelected(distinctUserIds.size())
					.guestsSelected(distinctGuestIds.size())
					.tokensFound(0)
					.notificationsSent(0)
					.notificationsFailed(0)
					.build();

		final List<String> tokenList = new ArrayList<>(tokensToNotify);
		int sentCount = 0;
		int failedCount = 0;
		final List<String> invalidTokens = new ArrayList<>();

		for (int i = 0; i < tokenList.size(); i += FCM_BATCH_SIZE) {
			final List<String> batch = tokenList.subList(i, Math.min(i + FCM_BATCH_SIZE, tokenList.size()));
			final MulticastMessage message = MulticastMessage.builder()
					.setNotification(Notification.builder()
							.setTitle(template.getTitle())
							.setBody(template.getBodyTemplate())
							.build())
					.addAllTokens(batch)
					.build();

			try {
				final BatchResponse response = firebaseMessaging.sendEachForMulticast(message);
				sentCount 		+= response.getSuccessCount();
				failedCount 	+= response.getFailureCount();

				final List<SendResponse> responses = response.getResponses();
				for (int j = 0; j < responses.size(); j++) {
					final SendResponse sendResponse = responses.get(j);
					if (!sendResponse.isSuccessful()) {
						final FirebaseMessagingException exception = sendResponse.getException();
						if (exception != null && isInvalidTokenError(exception))
							invalidTokens.add(batch.get(j));
					}
				}
			} catch (final FirebaseMessagingException e) {
				log.error("Failed to send FCM batch notification", e);
				failedCount += batch.size();
			}
		}

		if (!invalidTokens.isEmpty())
			deactivateInvalidTokens(invalidTokens);

		log.info("Notification send completed. Sent: {}, Failed: {}", sentCount, failedCount);

		final String status;
		final String responseMessage;

		if (failedCount == 0) {
			status = "SUCCESS";
			responseMessage = "Notification sent successfully";
		} else if (sentCount > 0) {
			status = "PARTIAL_SUCCESS";
			responseMessage = "Notification sent with partial failures";
		} else {
			status = "FAILED";
			responseMessage = "Failed to send notifications";
		}

		return SendNotificationResponseDto.builder()
				.message(responseMessage)
				.status(status)
				.templateId(template.getId())
				.usersSelected(distinctUserIds.size())
				.guestsSelected(distinctGuestIds.size())
				.tokensFound(tokenList.size())
				.notificationsSent(sentCount)
				.notificationsFailed(failedCount)
				.build();
	}

	private boolean isInvalidTokenError(final FirebaseMessagingException exception) {
		final MessagingErrorCode code = exception.getMessagingErrorCode();
		return MessagingErrorCode.UNREGISTERED.equals(code) || MessagingErrorCode.INVALID_ARGUMENT.equals(code);
	}

	private void deactivateInvalidTokens(final List<String> invalidTokens) {
		for (final String token : invalidTokens)
			subscriptionRepository.findByFcmToken(token).ifPresent(sub -> {
				sub.setActive(false);
				subscriptionRepository.save(sub);
			});
	}

	private SubscriptionResponseDto mapToResponseDto(final NotificationSubscription subscription) {
		return SubscriptionResponseDto.builder()
				.id(subscription.getId())
				.guestId(subscription.getGuestId())
				.userId(subscription.getUserId())
				.deviceType(subscription.getDeviceType())
				.isActive(subscription.isActive())
				.build();
	}

	@Override
	public Page<NotificationLogResponseDto> getUserNotifications(final String userId, final Pageable pageable) {
		if (userId == null || userId.trim().isEmpty())
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User ID is required");
		final Page<NotificationLog> logs = notificationLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
		return logs.map(this::mapToLogResponseDto);
	}

	@Override
	public Page<NotificationLogResponseDto> getGuestNotifications(final String guestId, final Pageable pageable) {
		if (guestId == null || guestId.trim().isEmpty())
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Guest ID is required");

		final Page<NotificationLog> logs = notificationLogRepository.findByGuestIdOrderByCreatedAtDesc(guestId, pageable);
		return logs.map(this::mapToLogResponseDto);
	}

	@Override
	public UnreadCountResponseDto getUserUnreadCount(final String userId) {
		if (userId == null || userId.trim().isEmpty())
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User ID is required");
		final long count = notificationLogRepository.countByUserIdAndIsReadFalse(userId);
		return new UnreadCountResponseDto(count);
	}

	@Override
	public UnreadCountResponseDto getGuestUnreadCount(final String guestId) {
		if (guestId == null || guestId.trim().isEmpty())
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Guest ID is required");

		final long count = notificationLogRepository.countByGuestIdAndIsReadFalse(guestId);
		return new UnreadCountResponseDto(count);
	}

	@Override
	@Transactional
	public NotificationLogResponseDto markAsRead(final Long notificationId, final String requesterUserId, final String requesterGuestId) {
		if (notificationId == null)
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Notification ID is required");
		final var logEntry = notificationLogRepository.findById(notificationId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));

		boolean isOwner = false;
		if (requesterUserId != null && requesterUserId.equals(logEntry.getUserId()))
			isOwner = true;
		if (requesterGuestId != null && requesterGuestId.equals(logEntry.getGuestId()))
			isOwner = true;

		if (!isOwner)
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Notification does not belong to the requester");

		if (!logEntry.isRead()) {
			logEntry.setRead(true);
			logEntry.setReadAt(java.time.Instant.now());
			notificationLogRepository.save(logEntry);
		}

		return mapToLogResponseDto(logEntry);
	}

	private NotificationLogResponseDto mapToLogResponseDto(final NotificationLog log) {
		return NotificationLogResponseDto.builder()
				.id(log.getId())
				.userId(log.getUserId())
				.guestId(log.getGuestId())
				.templateId(log.getTemplateId())
				.title(log.getTitle())
				.body(log.getBody())
				.code(log.getCode())
				.isRead(log.isRead())
				.readAt(log.getReadAt())
				.createdAt(log.getCreatedAt())
				.build();
	}
}
