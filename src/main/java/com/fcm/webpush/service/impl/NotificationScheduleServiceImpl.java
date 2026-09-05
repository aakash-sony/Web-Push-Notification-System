package com.fcm.webpush.service.impl;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fcm.webpush.dto.request.NotificationScheduleRequestDto;
import com.fcm.webpush.dto.request.SendNotificationRequestDto;
import com.fcm.webpush.dto.response.NotificationScheduleResponseDto;
import com.fcm.webpush.entity.NotificationLog;
import com.fcm.webpush.entity.NotificationSchedule;
import com.fcm.webpush.entity.NotificationScheduleExecution;
import com.fcm.webpush.entity.NotificationSubscription;
import com.fcm.webpush.repository.NotificationLogRepository;
import com.fcm.webpush.repository.NotificationMasterRepository;
import com.fcm.webpush.repository.NotificationScheduleExecutionRepository;
import com.fcm.webpush.repository.NotificationScheduleRepository;
import com.fcm.webpush.repository.NotificationSubscriptionRepository;
import com.fcm.webpush.repository.UserRepository;
import com.fcm.webpush.service.NotificationScheduleService;
import com.fcm.webpush.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationScheduleServiceImpl implements NotificationScheduleService {

	private static final ZoneId APP_ZONE = ZoneId.of("Asia/Kolkata");

	private final NotificationScheduleRepository 				scheduleRepository;
	private final NotificationScheduleExecutionRepository 		executionRepository;
	private final NotificationMasterRepository 					templateRepository;
	private final UserRepository 								userRepository;
	private final NotificationSubscriptionRepository 			subscriptionRepository;
	private final NotificationLogRepository 					notificationLogRepository;
	private final NotificationService 							notificationService;

	@Override
	@Transactional
	public NotificationScheduleResponseDto createSchedule(final NotificationScheduleRequestDto request) {
		validateScheduleRequest(request);

		final var template = templateRepository.findById(request.getTemplateId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification template not found"));

		if (!template.isActive())
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected notification template is inactive");

		final var schedule = NotificationSchedule.builder()
				.template(template)
				.scheduleType(request.getScheduleType())
				.timeOfDay(request.getTimeOfDay())
				.intervalMinutes(request.getIntervalMinutes())
				.isActive(request.getIsActive() != null ? request.getIsActive() : true)
				.build();

		if (request.getOffsets() != null)
			schedule.setParsedOffsets(normalizeOffsets(request.getOffsets()));

		final var saved = scheduleRepository.save(schedule);
		return mapToResponseDto(saved);
	}

	@Override
	public List<NotificationScheduleResponseDto> getAllSchedules() {
		return scheduleRepository.findAll().stream()
				.map(this::mapToResponseDto)
				.collect(Collectors.toList());
	}

	@Override
	public NotificationScheduleResponseDto getScheduleById(final Long id) {
		final var schedule = scheduleRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification schedule not found with id: " + id));
		return mapToResponseDto(schedule);
	}

	@Override
	@Transactional
	public NotificationScheduleResponseDto updateSchedule(final Long id, final NotificationScheduleRequestDto request) {
		final var schedule = scheduleRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification schedule not found with id: " + id));

		validateScheduleRequest(request);

		final var template = templateRepository.findById(request.getTemplateId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification template not found"));

		if (!template.isActive())
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected notification template is inactive");

		schedule.setTemplate(template);
		schedule.setScheduleType(request.getScheduleType());
		schedule.setTimeOfDay(request.getTimeOfDay());
		schedule.setIntervalMinutes(request.getIntervalMinutes());
		if (request.getIsActive() != null)
			schedule.setActive(request.getIsActive());

		if (request.getOffsets() != null)
			schedule.setParsedOffsets(normalizeOffsets(request.getOffsets()));
		else
			schedule.setOffsets(null);

		final var updated = scheduleRepository.save(schedule);
		return mapToResponseDto(updated);
	}

	@Override
	@Transactional
	public NotificationScheduleResponseDto updateScheduleStatus(final Long id, final boolean active) {
		final var schedule = scheduleRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification schedule not found with id: " + id));

		schedule.setActive(active);
		final var updated = scheduleRepository.save(schedule);
		return mapToResponseDto(updated);
	}

	@Override
	public void processDueSchedules() {
		final var activeSchedules = scheduleRepository.findByIsActiveTrue();
		if (activeSchedules.isEmpty())
			return;

		final var now 				= ZonedDateTime.now(APP_ZONE);
		final var today 			= now.toLocalDate();
		final var currentTime 		= now.toLocalTime();

		for (final var schedule : activeSchedules)
			try {
				processSingleSchedule(schedule, now, today, currentTime);
			} catch (final Exception e) {
				log.error("Error processing notification schedule id: {}", schedule.getId(), e);
			}
	}

	private void processSingleSchedule(final NotificationSchedule schedule, final ZonedDateTime now, final LocalDate today, final LocalTime currentTime) {
		if (schedule.getTemplate() == null || !schedule.getTemplate().isActive())
			return;

		final var type 					= schedule.getScheduleType();

		switch (type) {
		case DAILY: {
			if (schedule.getTimeOfDay() != null && currentTime.isBefore(schedule.getTimeOfDay()))
				return;
			final var occurrenceKey 	= "DAILY_" + today;
			processScheduleForRecipients(schedule, occurrenceKey, now, today);
			break;
		}
		case OFFSET:
			if (schedule.getTimeOfDay() != null && currentTime.isBefore(schedule.getTimeOfDay()))
				return;
			processOffsetScheduleForRecipients(schedule, now, today);
			break;
		case LOGIN_REMINDER:
			processLoginReminderForRecipients(schedule, now);
			break;
		case null:
		default:
			break;
		}
	}

	private void processScheduleForRecipients(final NotificationSchedule schedule, final String occurrenceKey, final ZonedDateTime now, final LocalDate today) {
		final var CHUNK_SIZE = 500;
		final var pageable = PageRequest.of(0, CHUNK_SIZE);

		var lastUserId = 0L;
		List<Object[]> userChunk;
		do {
			userChunk = userRepository.findUserIdAndCreatedAtChunk(lastUserId, pageable);
			if (userChunk.isEmpty()) break;

			final var userIds = new ArrayList<String>(userChunk.size());
			for (final var row : userChunk) {
				final var uId = (Long) row[0];
				userIds.add(String.valueOf(uId));
				lastUserId = uId;
			}
			dispatchBatch(schedule, "USER", userIds, occurrenceKey);
		} while (userChunk.size() == CHUNK_SIZE);

		var lastSubId = 0L;
		List<Object[]> guestChunk;
		do {
			guestChunk = subscriptionRepository.findActiveGuestIdAndCreatedAtChunk(lastSubId, pageable);
			if (guestChunk.isEmpty()) break;

			final var guestIds = new ArrayList<String>(guestChunk.size());
			for (final var row : guestChunk) {
				final var subId = (Long) row[0];
				final var guestId = (String) row[1];
				if (guestId != null && !guestId.isBlank()) {
					guestIds.add(guestId);
				}
				lastSubId = subId;
			}
			dispatchBatch(schedule, "GUEST", guestIds, occurrenceKey);
		} while (guestChunk.size() == CHUNK_SIZE);
	}

	private void processOffsetScheduleForRecipients(final NotificationSchedule schedule, final ZonedDateTime now, final LocalDate today) {
		final var offsets = schedule.getParsedOffsets();
		if (offsets.isEmpty())
			return;

		final var CHUNK_SIZE = 500;
		final var pageable = PageRequest.of(0, CHUNK_SIZE);

		var lastUserId = 0L;
		List<Object[]> userChunk;
		do {
			userChunk = userRepository.findUserIdAndCreatedAtChunk(lastUserId, pageable);
			if (userChunk.isEmpty()) break;

			final var groupedByUserKey = new HashMap<String, List<String>>();
			for (final var row : userChunk) {
				final var uId = (Long) row[0];
				final var anchorInstant = (Instant) row[1];
				lastUserId = uId;

				if (anchorInstant == null) continue;

				final var anchorDate = anchorInstant.atZone(APP_ZONE).toLocalDate();
				final var elapsedDays = ChronoUnit.DAYS.between(anchorDate, today);

				if (elapsedDays >= 0 && offsets.contains((int) elapsedDays)) {
					final var occurrenceKey = "OFFSET_" + elapsedDays + "_" + today;
					groupedByUserKey.computeIfAbsent(occurrenceKey, k -> new ArrayList<>()).add(String.valueOf(uId));
				}
			}

			groupedByUserKey.forEach((occKey, uIds) -> dispatchBatch(schedule, "USER", uIds, occKey));
		} while (userChunk.size() == CHUNK_SIZE);

		var lastSubId = 0L;
		List<Object[]> guestChunk;
		final var processedGuestIds = new HashSet<String>();
		do {
			guestChunk = subscriptionRepository.findActiveGuestIdAndCreatedAtChunk(lastSubId, pageable);
			if (guestChunk.isEmpty()) break;

			final var groupedByGuestKey = new HashMap<String, List<String>>();
			for (final var row : guestChunk) {
				final var subId = (Long) row[0];
				final var guestId = (String) row[1];
				final var anchorInstant = (Instant) row[2];
				lastSubId = subId;

				if (guestId == null || guestId.isBlank() || processedGuestIds.contains(guestId))
					continue;
				processedGuestIds.add(guestId);

				if (anchorInstant == null) continue;
				final var anchorDate = anchorInstant.atZone(APP_ZONE).toLocalDate();
				final var elapsedDays = ChronoUnit.DAYS.between(anchorDate, today);

				if (elapsedDays >= 0 && offsets.contains((int) elapsedDays)) {
					final var occurrenceKey = "OFFSET_" + elapsedDays + "_" + today;
					groupedByGuestKey.computeIfAbsent(occurrenceKey, k -> new ArrayList<>()).add(guestId);
				}
			}

			groupedByGuestKey.forEach((occKey, gIds) -> dispatchBatch(schedule, "GUEST", gIds, occKey));
		} while (guestChunk.size() == CHUNK_SIZE);
	}

	private void processLoginReminderForRecipients(final NotificationSchedule schedule, final ZonedDateTime now) {
		final var intervalMinutes = schedule.getIntervalMinutes() != null && schedule.getIntervalMinutes() > 0
				? schedule.getIntervalMinutes() : 2;

		final var CHUNK_SIZE = 500;
		final var pageable = PageRequest.of(0, CHUNK_SIZE);

		var lastUserId = 0L;
		List<Object[]> userChunk;
		do {
			userChunk = userRepository.findUserIdAndCreatedAtChunk(lastUserId, pageable);
			if (userChunk.isEmpty()) break;

			for (final var row : userChunk) {
				final var uId = (Long) row[0];
				lastUserId = uId;
				final var userIdStr = String.valueOf(uId);

				final var latestLogOpt = notificationLogRepository.findFirstByUserIdAndTemplateIdOrderByCreatedAtDesc(userIdStr, schedule.getTemplate().getId());
				if (isEligibleForLoginReminder(latestLogOpt, now.toInstant(), intervalMinutes)) {
					final var lastLogIdStr = latestLogOpt.map(l -> String.valueOf(l.getId())).orElse("0");
					final var occurrenceKey = "LOGIN_" + lastLogIdStr + "_" + now.toInstant().toEpochMilli();
					executeRecipientNotification(schedule, "USER", userIdStr, occurrenceKey);
				}
			}
		} while (userChunk.size() == CHUNK_SIZE);

		var lastSubId = 0L;
		List<Object[]> guestChunk;
		final var processedGuestIds = new HashSet<String>();
		do {
			guestChunk = subscriptionRepository.findActiveGuestIdAndCreatedAtChunk(lastSubId, pageable);
			if (guestChunk.isEmpty()) break;

			for (final var row : guestChunk) {
				final var subId = (Long) row[0];
				final var guestId = (String) row[1];
				lastSubId = subId;

				if (guestId == null || guestId.isBlank() || processedGuestIds.contains(guestId))
					continue;
				processedGuestIds.add(guestId);

				final var latestLogOpt = notificationLogRepository.findFirstByGuestIdAndTemplateIdOrderByCreatedAtDesc(guestId, schedule.getTemplate().getId());
				if (isEligibleForLoginReminder(latestLogOpt, now.toInstant(), intervalMinutes)) {
					final var lastLogIdStr = latestLogOpt.map(l -> String.valueOf(l.getId())).orElse("0");
					final var occurrenceKey = "LOGIN_" + lastLogIdStr + "_" + now.toInstant().toEpochMilli();
					executeRecipientNotification(schedule, "GUEST", guestId, occurrenceKey);
				}
			}
		} while (guestChunk.size() == CHUNK_SIZE);
	}

	private void dispatchBatch(final NotificationSchedule schedule, final String recipientType, final List<String> recipientIds, final String occurrenceKey) {
		if (recipientIds == null || recipientIds.isEmpty())
			return;

		final var newExecutions = new ArrayList<NotificationScheduleExecution>();
		final var eligibleIds = new ArrayList<String>();

		for (final var recipientId : recipientIds) {
			final var alreadyExecuted = executionRepository.existsByScheduleIdAndRecipientTypeAndRecipientIdAndOccurrenceKey(
					schedule.getId(), recipientType, recipientId, occurrenceKey
			);
			if (alreadyExecuted)
				continue;

			final var execution = NotificationScheduleExecution.builder()
					.scheduleId(schedule.getId())
					.recipientType(recipientType)
					.recipientId(recipientId)
					.occurrenceKey(occurrenceKey)
					.executedAt(Instant.now())
					.status("PENDING")
					.build();

			try {
				newExecutions.add(executionRepository.save(execution));
				eligibleIds.add(recipientId);
			} catch (final DataIntegrityViolationException e) {
				log.debug("Duplicate execution detected by DB constraint for schedule {} recipient {}: {}", schedule.getId(), recipientId, e.getMessage());
			}
		}

		if (eligibleIds.isEmpty())
			return;

		try {
			final var sendRequest = SendNotificationRequestDto.builder()
					.templateId(schedule.getTemplate().getId())
					.userIds("USER".equals(recipientType) ? eligibleIds.stream().map(Long::parseLong).toList() : null)
					.guestIds("GUEST".equals(recipientType) ? eligibleIds : null)
					.build();

			notificationService.sendNotification(sendRequest);

			for (final var exec : newExecutions) {
				exec.setStatus("SUCCESS");
			}
			executionRepository.saveAll(newExecutions);
		} catch (final Exception e) {
			log.error("Failed executing batch push for scheduleId: {}, recipientType: {}, count: {}", schedule.getId(), recipientType, eligibleIds.size(), e);
			for (final var exec : newExecutions) {
				exec.setStatus("FAILED");
			}
			executionRepository.saveAll(newExecutions);
		}
	}

	private boolean isEligibleForLoginReminder(final Optional<NotificationLog> latestLogOpt, final Instant nowInstant, final int intervalMinutes) {
		if (latestLogOpt.isEmpty())
			return true;

		final var latestLog = latestLogOpt.get();
		if (!latestLog.isRead())
			return false;

		final var referenceTime = latestLog.getReadAt() != null ? latestLog.getReadAt() : latestLog.getCreatedAt();
		if (referenceTime == null || referenceTime.isAfter(nowInstant))
			return false;
		final var minutesSinceLast 			= Duration.between(referenceTime, nowInstant).toMinutes();
		return minutesSinceLast >= intervalMinutes;
	}

	private void executeRecipientNotification(final NotificationSchedule schedule, final String recipientType, final String recipientId, final String occurrenceKey) {
		final var alreadyExecuted 			= executionRepository.existsByScheduleIdAndRecipientTypeAndRecipientIdAndOccurrenceKey(
				schedule.getId(), recipientType, recipientId, occurrenceKey
				);

		if (alreadyExecuted)
			return;

		var execution = NotificationScheduleExecution.builder()
				.scheduleId(schedule.getId())
				.recipientType(recipientType)
				.recipientId(recipientId)
				.occurrenceKey(occurrenceKey)
				.executedAt(Instant.now())
				.status("PENDING")
				.build();

		try {
			execution 			= executionRepository.save(execution);
		} catch (final DataIntegrityViolationException e) {
			log.info("Schedule execution already recorded by concurrent worker for scheduleId: {}, recipient: {}", schedule.getId(), recipientId);
			return;
		}

		try {
			final var sendRequest 	= SendNotificationRequestDto.builder()
					.templateId(schedule.getTemplate().getId())
					.userIds("USER".equals(recipientType) ? List.of(Long.parseLong(recipientId)) : null)
					.guestIds("GUEST".equals(recipientType) ? List.of(recipientId) : null)
					.build();

			notificationService.sendNotification(sendRequest);
			execution.setStatus("SUCCESS");
			executionRepository.save(execution);
		} catch (final Exception e) {
			log.error("Failed to send scheduled notification for scheduleId: {}, recipientType: {}, recipientId: {}", schedule.getId(), recipientType, recipientId, e);
			execution.setStatus("FAILED");
			executionRepository.save(execution);
		}
	}

	private void validateScheduleRequest(final NotificationScheduleRequestDto request) {
		if (request == null)
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body cannot be null");
		if (request.getTemplateId() == null)
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Template ID is required");
		if (request.getScheduleType() == null)
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Schedule type is required");

		final var type = request.getScheduleType();

		switch (type) {
		case DAILY:
			if (request.getTimeOfDay() == null)
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Time of day is required for DAILY schedule");
			break;
		case OFFSET:
			if (request.getOffsets() == null || request.getOffsets().isEmpty())
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Offsets list cannot be empty for OFFSET schedule");
			for (final var offset : request.getOffsets())
				if (offset == null || offset < 0)
					throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Offset values cannot be negative or null");
			break;
		case LOGIN_REMINDER:
			if (request.getIntervalMinutes() == null || request.getIntervalMinutes() <= 0)
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Interval minutes must be greater than zero for LOGIN_REMINDER schedule");
			break;
		case null:
		default:
			break;
		}
	}

	private List<Integer> normalizeOffsets(final List<Integer> rawOffsets) {
		if (rawOffsets == null)
			return List.of();
		return rawOffsets.stream()
				.filter(o -> o != null && o >= 0)
				.distinct()
				.sorted()
				.collect(Collectors.toList());
	}

	private NotificationScheduleResponseDto mapToResponseDto(final NotificationSchedule schedule) {
		return NotificationScheduleResponseDto.builder()
				.id(schedule.getId())
				.templateId(schedule.getTemplate() != null ? schedule.getTemplate().getId() : null)
				.templateTitle(schedule.getTemplate() != null ? schedule.getTemplate().getTitle() : null)
				.scheduleType(schedule.getScheduleType())
				.timeOfDay(schedule.getTimeOfDay())
				.offsets(schedule.getParsedOffsets())
				.intervalMinutes(schedule.getIntervalMinutes())
				.isActive(schedule.isActive())
				.createdAt(schedule.getCreatedAt())
				.updatedAt(schedule.getUpdatedAt())
				.build();
	}
}
