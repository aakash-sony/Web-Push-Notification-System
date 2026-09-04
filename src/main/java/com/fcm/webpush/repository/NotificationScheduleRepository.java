package com.fcm.webpush.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fcm.webpush.entity.NotificationSchedule;

@Repository
public interface NotificationScheduleRepository extends JpaRepository<NotificationSchedule, Long> {

	List<NotificationSchedule> findByIsActiveTrue();
}
