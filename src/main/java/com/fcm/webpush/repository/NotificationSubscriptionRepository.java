package com.fcm.webpush.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fcm.webpush.entity.NotificationSubscription;

@Repository
public interface NotificationSubscriptionRepository extends JpaRepository<NotificationSubscription, Long> {

	Optional<NotificationSubscription> findByFcmToken(String fcmToken);

	List<NotificationSubscription> findAllByGuestId(String guestId);

	List<NotificationSubscription> findByUserIdIsNull();
}

