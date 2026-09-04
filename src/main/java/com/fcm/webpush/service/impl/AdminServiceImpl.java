package com.fcm.webpush.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fcm.webpush.dto.response.GuestResponseDto;
import com.fcm.webpush.dto.response.NotificationTemplateResponseDto;
import com.fcm.webpush.dto.response.UserResponseDto;
import com.fcm.webpush.entity.NotificationMaster;
import com.fcm.webpush.entity.NotificationSubscription;
import com.fcm.webpush.entity.User;
import com.fcm.webpush.repository.NotificationMasterRepository;
import com.fcm.webpush.repository.NotificationSubscriptionRepository;
import com.fcm.webpush.repository.UserRepository;
import com.fcm.webpush.service.AdminService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

	private final UserRepository 						userRepository;
	private final NotificationSubscriptionRepository 	subscriptionRepository;
	private final NotificationMasterRepository 			notificationMasterRepository;

	@Value("${admin.username}")
	private String adminUsername;

	@Override
	public void verifyAdminAuthorization(final String username) {
		if (username == null || username.isBlank())
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated");

		if (!adminUsername.equals(username))
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Admin privileges required");
	}

	@Override
	public List<GuestResponseDto> getAllGuests() {
		return subscriptionRepository.findByUserIdIsNull().stream().map(this::mapToGuestResponseDto).toList();
	}

	@Override
	public List<UserResponseDto> getAllUsers() {
		return userRepository.findAll().stream().map(this::mapToUserResponseDto).toList();
	}

	@Override
	public List<NotificationTemplateResponseDto> getAllNotificationTemplates() {
		return notificationMasterRepository.findByIsActiveTrue().stream().map(this::mapToNotificationTemplateResponseDto).toList();
	}

	private GuestResponseDto mapToGuestResponseDto(final NotificationSubscription subscription) {
		return GuestResponseDto.builder()
				.id(subscription.getId())
				.guestId(subscription.getGuestId())
				.deviceType(subscription.getDeviceType())
				.isActive(subscription.isActive())
				.build();
	}

	private UserResponseDto mapToUserResponseDto(final User user) {
		return UserResponseDto.builder()
				.id(user.getId())
				.username(user.getUsername())
				.build();
	}

	private NotificationTemplateResponseDto mapToNotificationTemplateResponseDto(final NotificationMaster template) {
		return NotificationTemplateResponseDto.builder()
				.id(template.getId())
				.code(template.getCode())
				.title(template.getTitle())
				.bodyTemplate(template.getBodyTemplate())
				.isActive(template.isActive())
				.build();
	}
}
