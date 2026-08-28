package com.release_guard.notification_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.release_guard.notification_service.dto.request.ConsumerLagTestRequest;
import com.release_guard.notification_service.dto.response.ConsumerLagResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationKafkaLagServiceTest {

	@Mock
	private KafkaTemplate<String, String> kafkaTemplate;

	private final ObjectMapper objectMapper = new ObjectMapper();
	private NotificationKafkaLagService lagService;

	@BeforeEach
	void setUp() {
		lagService = new NotificationKafkaLagService(kafkaTemplate, objectMapper);
		ReflectionTestUtils.setField(lagService, "bootstrapServers", "localhost:9092");
		ReflectionTestUtils.setField(lagService, "consumerGroupId", "notification-service");
		ReflectionTestUtils.setField(lagService, "riskCalculatedTopic", "risk.calculated");
		ReflectionTestUtils.setField(lagService, "releaseStatusChangedTopic", "release.status.changed");
		ReflectionTestUtils.setField(lagService, "incidentCreatedTopic", "health.incident.created");
	}

	@Test
	void shouldPublishAndMeasureLagSuccessfully() {
		when(kafkaTemplate.send(anyString(), anyString(), anyString()))
				.thenReturn(new CompletableFuture<>());

		ConsumerLagTestRequest request = new ConsumerLagTestRequest(3, "ALL", 0L, "payment-service");
		ConsumerLagResponse response = lagService.simulateAndMeasureLag(request);

		assertThat(response).isNotNull();
		assertThat(response.status()).isEqualTo("SUCCESS");
		assertThat(response.eventsPublished()).isEqualTo(3);
		assertThat(response.targetConsumerGroup()).isEqualTo("notification-service");
		verify(kafkaTemplate, atLeastOnce()).send(anyString(), anyString(), any());
	}

	@Test
	void shouldReturnLagStatus() {
		ConsumerLagResponse response = lagService.getCurrentLagStatus();

		assertThat(response).isNotNull();
		assertThat(response.status()).isEqualTo("SUCCESS");
		assertThat(response.targetConsumerGroup()).isEqualTo("notification-service");
	}
}
