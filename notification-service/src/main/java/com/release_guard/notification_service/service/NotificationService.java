package com.release_guard.notification_service.service;

import com.release_guard.notification_service.dto.request.CreateNotificationRequest;
import com.release_guard.notification_service.dto.response.NotificationResponse;
import com.release_guard.notification_service.entity.Notification;
import com.release_guard.notification_service.entity.NotificationStatus;
import com.release_guard.notification_service.entity.NotificationType;
import com.release_guard.notification_service.exception.ResourceNotFoundException;
import com.release_guard.notification_service.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

	private final NotificationRepository notificationRepository;
	private final NotificationPushService notificationPushService;

	@Transactional
	public NotificationResponse createAndSend(CreateNotificationRequest request) {
		Notification notification = Notification.builder()
				.type(request.type())
				.recipient(request.recipient())
				.message(request.message())
				.sentAt(Instant.now())
				.status(request.status() != null ? request.status() : NotificationStatus.SENT)
				.build();

		Notification saved = notificationRepository.save(notification);
		NotificationResponse response = NotificationResponse.from(saved);

		// Push to real-time subscribers via WebSocket / SSE
		notificationPushService.push(response);

		log.info("Saved and dispatched notification {} of type {} to {}",
				saved.getId(), saved.getType(), saved.getRecipient());

		return response;
	}

	public List<NotificationResponse> list(String recipient, NotificationType type, NotificationStatus status) {
		return notificationRepository.findAllFiltered(blankToNull(recipient), type, status).stream()
				.map(NotificationResponse::from)
				.toList();
	}

	public NotificationResponse getById(UUID id) {
		return notificationRepository.findById(id)
				.map(NotificationResponse::from)
				.orElseThrow(() -> ResourceNotFoundException.notification(id));
	}

	@Transactional
	public NotificationResponse updateStatus(UUID id, NotificationStatus status) {
		Notification notification = notificationRepository.findById(id)
				.orElseThrow(() -> ResourceNotFoundException.notification(id));

		notification.setStatus(status);
		Notification saved = notificationRepository.save(notification);
		return NotificationResponse.from(saved);
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value;
	}
}
