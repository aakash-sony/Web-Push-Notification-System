package com.fcm.webpush.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.fcm.webpush.dto.request.SendNotificationRequestDto;
import com.fcm.webpush.dto.request.SubscriptionRequestDto;
import com.fcm.webpush.dto.response.NotificationLogResponseDto;
import com.fcm.webpush.dto.response.SendNotificationResponseDto;
import com.fcm.webpush.dto.response.SubscriptionResponseDto;
import com.fcm.webpush.dto.response.UnreadCountResponseDto;

public interface NotificationService {

	SubscriptionResponseDto registerOrRefreshSubscription(SubscriptionRequestDto request);

	SubscriptionResponseDto registerOrRefreshSubscription(SubscriptionRequestDto request, String userId);

	void associateGuestWithUser(String guestId, String userId);

	SendNotificationResponseDto sendNotification(SendNotificationRequestDto request);

	Page<NotificationLogResponseDto> getUserNotifications(String userId, Pageable pageable);

	Page<NotificationLogResponseDto> getGuestNotifications(String guestId, Pageable pageable);

	UnreadCountResponseDto getUserUnreadCount(String userId);

	UnreadCountResponseDto getGuestUnreadCount(String guestId);

	NotificationLogResponseDto markAsRead(Long notificationId, String requesterUserId, String requesterGuestId);

	boolean checkSubscriptionExists(String guestId, String fcmToken);

	void detachUserFromSubscription(String guestId, String fcmToken);
}


