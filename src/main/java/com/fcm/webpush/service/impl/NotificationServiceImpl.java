package com.fcm.webpush.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fcm.webpush.dto.request.SubscriptionRequestDto;
import com.fcm.webpush.dto.response.SubscriptionResponseDto;
import com.fcm.webpush.entity.NotificationSubscription;
import com.fcm.webpush.repository.NotificationSubscriptionRepository;
import com.fcm.webpush.service.NotificationService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

	private final NotificationSubscriptionRepository subscriptionRepository;

	@Override
	@Transactional
	public SubscriptionResponseDto registerOrRefreshSubscription(final SubscriptionRequestDto request) {
		final var subscription = subscriptionRepository.findByFcmToken(request.getFcmToken())
				.map(existing -> {
					existing.setGuestId(request.getGuestId());
					existing.setDeviceType(request.getDeviceType());
					existing.setActive(true);
					return existing;
				})
				.orElseGet(() -> NotificationSubscription.builder()
						.guestId(request.getGuestId())
						.fcmToken(request.getFcmToken())
						.deviceType(request.getDeviceType())
						.build());

		final var savedSubscription = subscriptionRepository.save(subscription);

		return mapToResponseDto(savedSubscription);
	}

	@Override
	@Transactional
	public void associateGuestWithUser(final String guestId, final String userId) {
		final var subscriptions = subscriptionRepository.findAllByGuestId(guestId);
		subscriptions.forEach(subscription -> subscription.setUserId(userId));
		subscriptionRepository.saveAll(subscriptions);
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
}
