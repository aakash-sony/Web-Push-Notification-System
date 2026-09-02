package com.fcm.webpush.service;

import com.fcm.webpush.dto.request.SubscriptionRequestDto;
import com.fcm.webpush.dto.response.SubscriptionResponseDto;

public interface NotificationService {

	SubscriptionResponseDto registerOrRefreshSubscription(SubscriptionRequestDto request);

	void associateGuestWithUser(String guestId, String userId);
}
