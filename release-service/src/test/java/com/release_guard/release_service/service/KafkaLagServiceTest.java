package com.release_guard.release_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.release_guard.release_service.dto.request.ConsumerLagTestRequest;
import com.release_guard.release_service.dto.response.ConsumerLagResponse;
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
class KafkaLagServiceTest {

	@Mock
	private KafkaTemplate<String, String> kafkaTemplate;

	private final ObjectMapper objectMapper = new ObjectMapper();
	private KafkaLagService kafkaLagService;

	@BeforeEach
	void setUp() {
		kafkaLagService = new KafkaLagService(kafkaTemplate, objectMapper);
		ReflectionTestUtils.setField(kafkaLagService, "bootstrapServers", "localhost:9092");
		ReflectionTestUtils.setField(kafkaLagService, "riskCalculatedTopic", "risk.calculated");
		ReflectionTestUtils.setField(kafkaLagService, "releaseStatusChangedTopic", "release.status.changed");
		ReflectionTestUtils.setField(kafkaLagService, "incidentCreatedTopic", "health.incident.created");
		ReflectionTestUtils.setField(kafkaLagService, "releaseCreatedTopic", "release.created");
	}

	@Test
	void shouldSimulateAndPublishEventsSuccessfully() {
		when(kafkaTemplate.send(anyString(), anyString(), anyString()))
				.thenReturn(new CompletableFuture<>());

		ConsumerLagTestRequest request = new ConsumerLagTestRequest(4, "ALL", 0L, "order-service");
		ConsumerLagResponse response = kafkaLagService.simulateAndMeasureLag(request);

		assertThat(response).isNotNull();
		assertThat(response.status()).isEqualTo("SUCCESS");
		assertThat(response.eventsPublished()).isEqualTo(4);
		assertThat(response.targetConsumerGroup()).isEqualTo("notification-service");
		assertThat(response.verificationEndpoints()).containsKey("1_ListNotifications");

		verify(kafkaTemplate, atLeastOnce()).send(anyString(), anyString(), any());
	}

	@Test
	void shouldReturnLagStatusMetrics() {
		ConsumerLagResponse response = kafkaLagService.getCurrentLagStatus();

		assertThat(response).isNotNull();
		assertThat(response.status()).isEqualTo("SUCCESS");
		assertThat(response.targetConsumerGroup()).isEqualTo("notification-service");
		assertThat(response.targetTopics()).contains("risk.calculated", "release.status.changed");
	}
}
