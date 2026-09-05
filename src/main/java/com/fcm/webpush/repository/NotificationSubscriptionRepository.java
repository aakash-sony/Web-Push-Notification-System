package com.fcm.webpush.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

	@Modifying
	@Query("UPDATE NotificationSubscription s SET s.isActive = false, s.updatedAt = :now WHERE s.fcmToken IN :tokens")
	int deactivateTokensInBulk(@Param("tokens") Collection<String> tokens, @Param("now") Instant now);

	@Query("SELECT s.id, s.guestId, s.createdAt FROM NotificationSubscription s WHERE s.userId IS NULL AND s.isActive = true AND s.id > :lastId ORDER BY s.id ASC")
	List<Object[]> findActiveGuestIdAndCreatedAtChunk(@Param("lastId") Long lastId, Pageable pageable);
}


