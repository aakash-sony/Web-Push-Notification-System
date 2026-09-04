package com.fcm.webpush.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fcm.webpush.entity.NotificationSubscription;

@Repository
public interface NotificationSubscriptionRepository extends JpaRepository<NotificationSubscription, Long> {

	Optional<NotificationSubscription> findByFcmToken(String fcmToken);

	List<NotificationSubscription> findAllByGuestId(String guestId);

	boolean existsByGuestId(String guestId);

	Optional<NotificationSubscription> findByGuestIdAndFcmToken(String guestId, String fcmToken);

	boolean existsByGuestIdAndFcmToken(String guestId, String fcmToken);

	List<NotificationSubscription> findByUserIdIsNull();

	List<NotificationSubscription> findByUserIdInAndIsActiveTrue(Collection<String> userIds);

	List<NotificationSubscription> findByGuestIdInAndIsActiveTrue(Collection<String> guestIds);

	List<NotificationSubscription> findByGuestIdIn(Collection<String> guestIds);

	List<NotificationSubscription> findByUserIdIsNullAndIsActiveTrue();
}


