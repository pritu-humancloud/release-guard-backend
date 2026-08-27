package com.release_guard.notification_service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.release_guard.notification_service.dto.request.CreateNotificationRequest;
import com.release_guard.notification_service.entity.NotificationType;
import com.release_guard.notification_service.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationKafkaConsumerTest {

	@Mock
	private NotificationService notificationService;

	private ObjectMapper objectMapper;
	private NotificationKafkaConsumer consumer;

	@BeforeEach
	void setUp() {
		objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
		consumer = new NotificationKafkaConsumer(objectMapper, notificationService);
	}

	@Test
	void consumeRiskCalculated_scoreAbove70_triggersRiskHighNotification() {
		UUID releaseId = UUID.randomUUID();
		String message = """
				{
				  "eventId": "%s",
				  "eventType": "risk.calculated",
				  "releaseId": "%s",
				  "score": 75,
				  "riskLevel": "HIGH",
				  "data": {
				    "score": 75,
				    "riskLevel": "HIGH"
				  }
				}
				""".formatted(UUID.randomUUID(), releaseId);

		consumer.consumeRiskCalculated(message);

		ArgumentCaptor<CreateNotificationRequest> captor = ArgumentCaptor.forClass(CreateNotificationRequest.class);
		verify(notificationService).createAndSend(captor.capture());

		CreateNotificationRequest captured = captor.getValue();
		assertThat(captured.type()).isEqualTo(NotificationType.RISK_HIGH);
		assertThat(captured.recipient()).isEqualTo("release-team");
		assertThat(captured.message()).contains(releaseId.toString()).contains("75");
	}

	@Test
	void consumeRiskCalculated_scoreBelow70_noNotification() {
		String message = """
				{
				  "eventId": "%s",
				  "eventType": "risk.calculated",
				  "releaseId": "%s",
				  "score": 40,
				  "riskLevel": "MEDIUM"
				}
				""".formatted(UUID.randomUUID(), UUID.randomUUID());

		consumer.consumeRiskCalculated(message);

		verify(notificationService, never()).createAndSend(any());
	}

	@Test
	void consumeReleaseStatusChanged_blockedStatus_triggersReleaseBlockedNotification() {
		UUID releaseId = UUID.randomUUID();
		String message = """
				{
				  "eventId": "%s",
				  "eventType": "release.status.changed",
				  "releaseId": "%s",
				  "projectName": "billing-service",
				  "version": "v1.5.0",
				  "newStatus": "BLOCKED"
				}
				""".formatted(UUID.randomUUID(), releaseId);

		consumer.consumeReleaseStatusChanged(message);

		ArgumentCaptor<CreateNotificationRequest> captor = ArgumentCaptor.forClass(CreateNotificationRequest.class);
		verify(notificationService).createAndSend(captor.capture());

		CreateNotificationRequest captured = captor.getValue();
		assertThat(captured.type()).isEqualTo(NotificationType.RELEASE_BLOCKED);
		assertThat(captured.recipient()).isEqualTo("release-team");
		assertThat(captured.message()).contains("BLOCKED").contains("billing-service");
	}

	@Test
	void consumeIncidentCreated_triggersIncidentNotification() {
		String message = """
				{
				  "eventId": "%s",
				  "eventType": "health.incident.created",
				  "incidentId": "INC-1002",
				  "serviceName": "payment-gateway",
				  "message": "Elevated 5xx error rate detected"
				}
				""".formatted(UUID.randomUUID());

		consumer.consumeIncidentCreated(message);

		ArgumentCaptor<CreateNotificationRequest> captor = ArgumentCaptor.forClass(CreateNotificationRequest.class);
		verify(notificationService).createAndSend(captor.capture());

		CreateNotificationRequest captured = captor.getValue();
		assertThat(captured.type()).isEqualTo(NotificationType.INCIDENT);
		assertThat(captured.recipient()).isEqualTo("oncall-team");
		assertThat(captured.message()).contains("payment-gateway").contains("INC-1002");
	}
}
