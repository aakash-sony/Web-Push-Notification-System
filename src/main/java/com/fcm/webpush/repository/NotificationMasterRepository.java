package com.fcm.webpush.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fcm.webpush.entity.NotificationMaster;
import com.fcm.webpush.enums.NotificationType;

@Repository
public interface NotificationMasterRepository extends JpaRepository<NotificationMaster, Long> {

	List<NotificationMaster> findByIsActiveTrue();

	Optional<NotificationMaster> findByCode(NotificationType code);

	boolean existsByCode(NotificationType code);
}
