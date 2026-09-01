package com.release_guard.notification_service.service;

import com.release_guard.notification_service.dto.request.CreateNotificationRequest;
import com.release_guard.notification_service.dto.response.NotificationResponse;
import com.release_guard.notification_service.entity.Notification;
import com.release_guard.notification_service.entity.NotificationStatus;
import com.release_guard.notification_service.entity.NotificationType;
import com.release_guard.notification_service.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

	@Mock
	private NotificationRepository notificationRepository;
	@Mock
	private NotificationPushService notificationPushService;

	private NotificationService notificationService;

	@BeforeEach
	void setUp() {
		notificationService = new NotificationService(notificationRepository, notificationPushService);
	}

	@Test
	void createAndSend_savesAndPushesNotification() {
		when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
			Notification n = inv.getArgument(0);
			n.setId(UUID.randomUUID());
			return n;
		});

		CreateNotificationRequest request = new CreateNotificationRequest(
				NotificationType.RISK_HIGH,
				"release-lead",
				"High risk score 85 detected on release",
				NotificationStatus.SENT
		);

		NotificationResponse response = notificationService.createAndSend(request);

		assertThat(response.id()).isNotNull();
		assertThat(response.type()).isEqualTo(NotificationType.RISK_HIGH);
		assertThat(response.recipient()).isEqualTo("release-lead");
		assertThat(response.status()).isEqualTo(NotificationStatus.SENT);

		verify(notificationPushService).push(any(NotificationResponse.class));
	}

	@Test
	void list_filtersNotifications() {
		Notification n = Notification.builder()
				.id(UUID.randomUUID())
				.type(NotificationType.RELEASE_BLOCKED)
				.recipient("release-team")
				.message("Release blocked")
				.sentAt(Instant.now())
				.status(NotificationStatus.SENT)
				.build();

		when(notificationRepository.findAllFiltered("release-team", NotificationType.RELEASE_BLOCKED, NotificationStatus.SENT))
				.thenReturn(List.of(n));

		List<NotificationResponse> results = notificationService.list("release-team", NotificationType.RELEASE_BLOCKED, NotificationStatus.SENT);

		assertThat(results).hasSize(1);
		assertThat(results.get(0).type()).isEqualTo(NotificationType.RELEASE_BLOCKED);
	}
}
