package com.fcm.webpush.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fcm.webpush.service.NotificationScheduleService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

	private final NotificationScheduleService notificationScheduleService;

	@Scheduled(fixedDelay = 120000)
	@SchedulerLock(name = "runScheduledNotifications", lockAtLeastFor = "15s", lockAtMostFor = "5m")
	public void runScheduledNotifications() {
		log.debug("Running periodic notification schedule check...");
		try {
			notificationScheduleService.processDueSchedules();
		} catch (final Exception e) {
			log.error("Unhandled error in periodic notification scheduler", e);
		}
	}
}
