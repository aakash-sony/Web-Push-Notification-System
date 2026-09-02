package com.fcm.webpush.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fcm.webpush.entity.NotificationSubscription;
import com.fcm.webpush.repository.NotificationSubscriptionRepository;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationSubscriptionRepository subscriptionRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Test
    @DisplayName("Should update all subscriptions matching guestId with the provided userId")
    void associateGuestWithUser_Success() {
        String guestId = "GUEST-123";
        String userId = "45";

        NotificationSubscription sub1 = NotificationSubscription.builder()
                .id(1L)
                .guestId(guestId)
                .fcmToken("token1")
                .build();

        NotificationSubscription sub2 = NotificationSubscription.builder()
                .id(2L)
                .guestId(guestId)
                .fcmToken("token2")
                .build();

        List<NotificationSubscription> subscriptions = List.of(sub1, sub2);

        when(subscriptionRepository.findAllByGuestId(guestId)).thenReturn(subscriptions);

        notificationService.associateGuestWithUser(guestId, userId);

        assertEquals(userId, sub1.getUserId());
        assertEquals(userId, sub2.getUserId());
        verify(subscriptionRepository).saveAll(subscriptions);
    }
}
