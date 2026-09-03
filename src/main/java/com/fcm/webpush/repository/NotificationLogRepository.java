package com.fcm.webpush.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fcm.webpush.entity.NotificationLog;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

	Page<NotificationLog> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

	Page<NotificationLog> findByGuestIdOrderByCreatedAtDesc(String guestId, Pageable pageable);

	long countByUserIdAndIsReadFalse(String userId);

	long countByGuestIdAndIsReadFalse(String guestId);
}
