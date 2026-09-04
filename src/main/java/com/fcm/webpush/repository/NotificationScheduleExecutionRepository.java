package com.fcm.webpush.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fcm.webpush.entity.NotificationScheduleExecution;

@Repository
public interface NotificationScheduleExecutionRepository extends JpaRepository<NotificationScheduleExecution, Long> {

	boolean existsByScheduleIdAndRecipientTypeAndRecipientIdAndOccurrenceKey(
			Long scheduleId,
			String recipientType,
			String recipientId,
			String occurrenceKey
	);

	Optional<NotificationScheduleExecution> findByScheduleIdAndRecipientTypeAndRecipientIdAndOccurrenceKey(
			Long scheduleId,
			String recipientType,
			String recipientId,
			String occurrenceKey
	);
}
