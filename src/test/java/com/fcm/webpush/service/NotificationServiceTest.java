package com.fcm.webpush.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

import com.fcm.webpush.config.FirebaseConfig;
import com.fcm.webpush.dto.request.SendNotificationRequestDto;
import com.fcm.webpush.dto.response.SendNotificationResponseDto;
import com.fcm.webpush.entity.NotificationMaster;
import com.fcm.webpush.entity.NotificationSubscription;
import com.fcm.webpush.entity.User;
import com.fcm.webpush.enums.NotificationType;
import com.fcm.webpush.repository.NotificationMasterRepository;
import com.fcm.webpush.repository.NotificationSubscriptionRepository;
import com.fcm.webpush.repository.UserRepository;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;

@SpringBootTest
@TestPropertySource(properties = {
		"spring.datasource.url=jdbc:h2:mem:testdb_send;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class NotificationServiceTest {

	@MockitoBean
	private FirebaseConfig firebaseConfig;

	@MockitoBean
	private FirebaseMessaging firebaseMessaging;

	@Autowired
	private NotificationService notificationService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private NotificationSubscriptionRepository subscriptionRepository;

	@Autowired
	private NotificationMasterRepository notificationMasterRepository;

	@Autowired
	private com.fcm.webpush.repository.NotificationLogRepository notificationLogRepository;

	private NotificationMaster activeTemplate;

	@BeforeEach
	void setUp() {
		notificationLogRepository.deleteAll();
		subscriptionRepository.deleteAll();
		userRepository.deleteAll();
		notificationMasterRepository.deleteAll();

		activeTemplate = notificationMasterRepository.save(NotificationMaster.builder()
				.code(NotificationType.WELCOME)
				.title("Welcome Test")
				.bodyTemplate("Welcome Body")
				.isActive(true)
				.build());
	}

	@Test
	void testSendNotification_SingleUser_Success() throws FirebaseMessagingException {
		User user = userRepository.save(User.builder().username("user1").password("pass").build());
		subscriptionRepository.save(NotificationSubscription.builder()
				.userId(String.valueOf(user.getId()))
				.fcmToken("token-user1")
				.isActive(true)
				.build());

		BatchResponse batchResponse = mock(BatchResponse.class);
		when(batchResponse.getSuccessCount()).thenReturn(1);
		when(batchResponse.getFailureCount()).thenReturn(0);
		SendResponse sr = mock(SendResponse.class);
		when(sr.isSuccessful()).thenReturn(true);
		when(batchResponse.getResponses()).thenReturn(List.of(sr));
		when(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class))).thenReturn(batchResponse);

		SendNotificationRequestDto request = SendNotificationRequestDto.builder()
				.userIds(List.of(user.getId()))
				.templateId(activeTemplate.getId())
				.build();

		SendNotificationResponseDto response = notificationService.sendNotification(request);

		assertEquals("SUCCESS", response.getStatus());
		assertEquals(1, response.getUsersSelected());
		assertEquals(0, response.getGuestsSelected());
		assertEquals(1, response.getTokensFound());
		assertEquals(1, response.getNotificationsSent());
		assertEquals(0, response.getNotificationsFailed());
		verify(firebaseMessaging, times(1)).sendEachForMulticast(any(MulticastMessage.class));
	}

	@Test
	void testSendNotification_MultipleUsers_Success() throws FirebaseMessagingException {
		User user1 = userRepository.save(User.builder().username("u1").password("p").build());
		User user2 = userRepository.save(User.builder().username("u2").password("p").build());
		User user3 = userRepository.save(User.builder().username("u3").password("p").build());

		subscriptionRepository.save(NotificationSubscription.builder().userId(String.valueOf(user1.getId())).fcmToken("t1").isActive(true).build());
		subscriptionRepository.save(NotificationSubscription.builder().userId(String.valueOf(user2.getId())).fcmToken("t2").isActive(true).build());
		subscriptionRepository.save(NotificationSubscription.builder().userId(String.valueOf(user3.getId())).fcmToken("t3").isActive(true).build());

		BatchResponse batchResponse = mock(BatchResponse.class);
		when(batchResponse.getSuccessCount()).thenReturn(3);
		when(batchResponse.getFailureCount()).thenReturn(0);
		SendResponse sr = mock(SendResponse.class);
		when(sr.isSuccessful()).thenReturn(true);
		when(batchResponse.getResponses()).thenReturn(List.of(sr, sr, sr));
		when(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class))).thenReturn(batchResponse);

		SendNotificationRequestDto request = SendNotificationRequestDto.builder()
				.userIds(List.of(user1.getId(), user2.getId(), user3.getId()))
				.templateId(activeTemplate.getId())
				.build();

		SendNotificationResponseDto response = notificationService.sendNotification(request);

		assertEquals("SUCCESS", response.getStatus());
		assertEquals(3, response.getUsersSelected());
		assertEquals(3, response.getTokensFound());
		assertEquals(3, response.getNotificationsSent());
	}

	@Test
	void testSendNotification_MultipleGuests_Success() throws FirebaseMessagingException {
		subscriptionRepository.save(NotificationSubscription.builder().guestId("g1").fcmToken("gt1").isActive(true).build());
		subscriptionRepository.save(NotificationSubscription.builder().guestId("g2").fcmToken("gt2").isActive(true).build());
		subscriptionRepository.save(NotificationSubscription.builder().guestId("g3").fcmToken("gt3").isActive(true).build());

		BatchResponse batchResponse = mock(BatchResponse.class);
		when(batchResponse.getSuccessCount()).thenReturn(3);
		when(batchResponse.getFailureCount()).thenReturn(0);
		SendResponse sr = mock(SendResponse.class);
		when(sr.isSuccessful()).thenReturn(true);
		when(batchResponse.getResponses()).thenReturn(List.of(sr, sr, sr));
		when(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class))).thenReturn(batchResponse);

		SendNotificationRequestDto request = SendNotificationRequestDto.builder()
				.guestIds(List.of("g1", "g2", "g3"))
				.templateId(activeTemplate.getId())
				.build();

		SendNotificationResponseDto response = notificationService.sendNotification(request);

		assertEquals("SUCCESS", response.getStatus());
		assertEquals(3, response.getGuestsSelected());
		assertEquals(3, response.getTokensFound());
		assertEquals(3, response.getNotificationsSent());
	}

	@Test
	void testSendNotification_MixedRecipients() throws FirebaseMessagingException {
		User u1 = userRepository.save(User.builder().username("mu1").password("p").build());
		User u2 = userRepository.save(User.builder().username("mu2").password("p").build());

		subscriptionRepository.save(NotificationSubscription.builder().userId(String.valueOf(u1.getId())).fcmToken("ut1").isActive(true).build());
		subscriptionRepository.save(NotificationSubscription.builder().userId(String.valueOf(u2.getId())).fcmToken("ut2").isActive(true).build());
		subscriptionRepository.save(NotificationSubscription.builder().guestId("mg1").fcmToken("mgt1").isActive(true).build());
		subscriptionRepository.save(NotificationSubscription.builder().guestId("mg2").fcmToken("mgt2").isActive(true).build());

		BatchResponse batchResponse = mock(BatchResponse.class);
		when(batchResponse.getSuccessCount()).thenReturn(4);
		when(batchResponse.getFailureCount()).thenReturn(0);
		SendResponse sr = mock(SendResponse.class);
		when(sr.isSuccessful()).thenReturn(true);
		when(batchResponse.getResponses()).thenReturn(List.of(sr, sr, sr, sr));
		when(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class))).thenReturn(batchResponse);

		SendNotificationRequestDto request = SendNotificationRequestDto.builder()
				.userIds(List.of(u1.getId(), u2.getId()))
				.guestIds(List.of("mg1", "mg2"))
				.templateId(activeTemplate.getId())
				.build();

		SendNotificationResponseDto response = notificationService.sendNotification(request);

		assertEquals("SUCCESS", response.getStatus());
		assertEquals(2, response.getUsersSelected());
		assertEquals(2, response.getGuestsSelected());
		assertEquals(4, response.getTokensFound());
		assertEquals(4, response.getNotificationsSent());
	}

	@Test
	void testSendNotification_NoRecipients_ThrowsBadRequest() {
		SendNotificationRequestDto request = SendNotificationRequestDto.builder()
				.userIds(List.of())
				.guestIds(List.of())
				.templateId(activeTemplate.getId())
				.build();

		ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> notificationService.sendNotification(request));
		assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
		assertTrue(ex.getReason().contains("At least one recipient"));
	}

	@Test
	void testSendNotification_NoTemplate_ThrowsBadRequest() {
		SendNotificationRequestDto request = SendNotificationRequestDto.builder()
				.guestIds(List.of("g1"))
				.templateId(null)
				.build();

		ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> notificationService.sendNotification(request));
		assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
	}

	@Test
	void testSendNotification_InvalidTemplate_ThrowsNotFound() {
		SendNotificationRequestDto request = SendNotificationRequestDto.builder()
				.guestIds(List.of("g1"))
				.templateId(9999L)
				.build();

		ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> notificationService.sendNotification(request));
		assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
	}

	@Test
	void testSendNotification_InvalidUserRecipient_ThrowsBadRequest() {
		SendNotificationRequestDto request = SendNotificationRequestDto.builder()
				.userIds(List.of(999L))
				.templateId(activeTemplate.getId())
				.build();

		ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> notificationService.sendNotification(request));
		assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
		assertTrue(ex.getReason().contains("Some selected users do not exist"));
	}

	@Test
	void testSendNotification_DuplicateRecipientIdsAndTokenDeduplication() throws FirebaseMessagingException {
		User u1 = userRepository.save(User.builder().username("du1").password("p").build());

		subscriptionRepository.save(NotificationSubscription.builder().userId(String.valueOf(u1.getId())).fcmToken("token-u1").isActive(true).build());
		subscriptionRepository.save(NotificationSubscription.builder().guestId("dg1").fcmToken("token-g1").isActive(true).build());

		BatchResponse batchResponse = mock(BatchResponse.class);
		when(batchResponse.getSuccessCount()).thenReturn(2);
		when(batchResponse.getFailureCount()).thenReturn(0);
		SendResponse sr = mock(SendResponse.class);
		when(sr.isSuccessful()).thenReturn(true);
		when(batchResponse.getResponses()).thenReturn(List.of(sr, sr));
		when(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class))).thenReturn(batchResponse);

		SendNotificationRequestDto request = SendNotificationRequestDto.builder()
				.userIds(List.of(u1.getId(), u1.getId()))
				.guestIds(List.of("dg1", "dg1"))
				.templateId(activeTemplate.getId())
				.build();

		SendNotificationResponseDto response = notificationService.sendNotification(request);

		assertEquals("SUCCESS", response.getStatus());
		assertEquals(1, response.getUsersSelected());
		assertEquals(1, response.getGuestsSelected());
		assertEquals(2, response.getTokensFound());
		assertEquals(2, response.getNotificationsSent());
	}


	@Test
	void testSendNotification_InvalidTokenDeactivationAndPartialFailure() throws FirebaseMessagingException {
		User u1 = userRepository.save(User.builder().username("pu1").password("p").build());
		subscriptionRepository.save(NotificationSubscription.builder().userId(String.valueOf(u1.getId())).fcmToken("valid-token").isActive(true).build());
		subscriptionRepository.save(NotificationSubscription.builder().userId(String.valueOf(u1.getId())).fcmToken("invalid-token").isActive(true).build());

		BatchResponse batchResponse = mock(BatchResponse.class);
		when(batchResponse.getSuccessCount()).thenReturn(1);
		when(batchResponse.getFailureCount()).thenReturn(1);

		SendResponse successSr = mock(SendResponse.class);
		when(successSr.isSuccessful()).thenReturn(true);

		SendResponse failedSr = mock(SendResponse.class);
		when(failedSr.isSuccessful()).thenReturn(false);
		FirebaseMessagingException fme = mock(FirebaseMessagingException.class);
		when(fme.getMessagingErrorCode()).thenReturn(MessagingErrorCode.UNREGISTERED);
		when(failedSr.getException()).thenReturn(fme);

		when(batchResponse.getResponses()).thenReturn(List.of(successSr, failedSr));
		when(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class))).thenReturn(batchResponse);

		SendNotificationRequestDto request = SendNotificationRequestDto.builder()
				.userIds(List.of(u1.getId()))
				.templateId(activeTemplate.getId())
				.build();

		SendNotificationResponseDto response = notificationService.sendNotification(request);

		assertEquals("PARTIAL_SUCCESS", response.getStatus());
		assertEquals(2, response.getTokensFound());
		assertEquals(1, response.getNotificationsSent());
		assertEquals(1, response.getNotificationsFailed());

		NotificationSubscription updatedSub2 = subscriptionRepository.findByFcmToken("invalid-token").orElseThrow();
		assertFalse(updatedSub2.isActive());
	}

	@Test
	void testNotificationLogPersistenceAndHistoryRetrieval() throws FirebaseMessagingException {
		User user1 = userRepository.save(User.builder().username("inboxUser1").password("p").build());
		User user2 = userRepository.save(User.builder().username("inboxUser2").password("p").build());

		subscriptionRepository.save(NotificationSubscription.builder().userId(String.valueOf(user1.getId())).fcmToken("t-inbox1").isActive(true).build());
		subscriptionRepository.save(NotificationSubscription.builder().guestId("inboxGuest1").fcmToken("t-inboxg1").isActive(true).build());

		BatchResponse batchResponse = mock(BatchResponse.class);
		when(batchResponse.getSuccessCount()).thenReturn(2);
		when(batchResponse.getFailureCount()).thenReturn(0);
		SendResponse sr = mock(SendResponse.class);
		when(sr.isSuccessful()).thenReturn(true);
		when(batchResponse.getResponses()).thenReturn(List.of(sr, sr));
		when(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class))).thenReturn(batchResponse);

		SendNotificationRequestDto request = SendNotificationRequestDto.builder()
				.userIds(List.of(user1.getId()))
				.guestIds(List.of("inboxGuest1"))
				.templateId(activeTemplate.getId())
				.build();

		notificationService.sendNotification(request);

		// Verify database history
		var user1Logs = notificationService.getUserNotifications(String.valueOf(user1.getId()), org.springframework.data.domain.PageRequest.of(0, 10));
		assertEquals(1, user1Logs.getTotalElements());
		assertEquals("Welcome Test", user1Logs.getContent().get(0).getTitle());
		assertEquals("Welcome Body", user1Logs.getContent().get(0).getBody());
		assertFalse(user1Logs.getContent().get(0).isRead());

		var user2Logs = notificationService.getUserNotifications(String.valueOf(user2.getId()), org.springframework.data.domain.PageRequest.of(0, 10));
		assertEquals(0, user2Logs.getTotalElements());

		var guestLogs = notificationService.getGuestNotifications("inboxGuest1", org.springframework.data.domain.PageRequest.of(0, 10));
		assertEquals(1, guestLogs.getTotalElements());
		assertEquals("Welcome Test", guestLogs.getContent().get(0).getTitle());
	}

	@Test
	void testUnreadCountAndMarkAsRead() throws FirebaseMessagingException {
		User user1 = userRepository.save(User.builder().username("readUser1").password("p").build());
		subscriptionRepository.save(NotificationSubscription.builder().userId(String.valueOf(user1.getId())).fcmToken("t-read1").isActive(true).build());

		BatchResponse batchResponse = mock(BatchResponse.class);
		when(batchResponse.getSuccessCount()).thenReturn(1);
		when(batchResponse.getFailureCount()).thenReturn(0);
		SendResponse sr = mock(SendResponse.class);
		when(sr.isSuccessful()).thenReturn(true);
		when(batchResponse.getResponses()).thenReturn(List.of(sr));
		when(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class))).thenReturn(batchResponse);

		notificationService.sendNotification(SendNotificationRequestDto.builder()
				.userIds(List.of(user1.getId()))
				.templateId(activeTemplate.getId())
				.build());

		var unreadCountBefore = notificationService.getUserUnreadCount(String.valueOf(user1.getId()));
		assertEquals(1, unreadCountBefore.getCount());

		var userLogs = notificationService.getUserNotifications(String.valueOf(user1.getId()), org.springframework.data.domain.PageRequest.of(0, 10));
		Long notifId = userLogs.getContent().get(0).getId();

		// Mark as read by rightful owner
		var updatedLog = notificationService.markAsRead(notifId, String.valueOf(user1.getId()), null);
		assertTrue(updatedLog.isRead());
		assertNotNull(updatedLog.getReadAt());

		var unreadCountAfter = notificationService.getUserUnreadCount(String.valueOf(user1.getId()));
		assertEquals(0, unreadCountAfter.getCount());
	}

	@Test
	void testMarkAsRead_RecipientIsolation_Forbidden() throws FirebaseMessagingException {
		User user1 = userRepository.save(User.builder().username("isoUser1").password("p").build());
		User user2 = userRepository.save(User.builder().username("isoUser2").password("p").build());
		subscriptionRepository.save(NotificationSubscription.builder().userId(String.valueOf(user1.getId())).fcmToken("t-iso1").isActive(true).build());

		BatchResponse batchResponse = mock(BatchResponse.class);
		when(batchResponse.getSuccessCount()).thenReturn(1);
		when(batchResponse.getFailureCount()).thenReturn(0);
		SendResponse sr = mock(SendResponse.class);
		when(sr.isSuccessful()).thenReturn(true);
		when(batchResponse.getResponses()).thenReturn(List.of(sr));
		when(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class))).thenReturn(batchResponse);

		notificationService.sendNotification(SendNotificationRequestDto.builder()
				.userIds(List.of(user1.getId()))
				.templateId(activeTemplate.getId())
				.build());

		var user1Logs = notificationService.getUserNotifications(String.valueOf(user1.getId()), org.springframework.data.domain.PageRequest.of(0, 10));
		Long notifId = user1Logs.getContent().get(0).getId();

		// Attempt mark as read by user2 (unauthorized) -> Expect 403 Forbidden
		ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
				notificationService.markAsRead(notifId, String.valueOf(user2.getId()), null)
		);
		assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
	}
}

