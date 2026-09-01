package com.release_guard.risk_service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.release_guard.risk_service.dto.request.CalculateRiskRequest;
import com.release_guard.risk_service.dto.response.RiskAssessmentResponse;
import com.release_guard.risk_service.entity.RiskLevel;
import com.release_guard.risk_service.service.RiskAssessmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskEventConsumerTest {

	@Mock
	private RiskAssessmentService riskAssessmentService;

	private ObjectMapper objectMapper;
	private RiskEventConsumer consumer;

	@BeforeEach
	void setUp() {
		objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
		consumer = new RiskEventConsumer(objectMapper, riskAssessmentService);
	}

	@Test
	void consumeReleaseCreated_validEvent_triggersAssessment() {
		UUID releaseId = UUID.randomUUID();
		String message = """
				{
				  "eventId": "%s",
				  "eventType": "release.created",
				  "releaseId": "%s",
				  "projectName": "order-service",
				  "version": "v1.2.0",
				  "status": "DRAFT",
				  "createdBy": "developer"
				}
				""".formatted(UUID.randomUUID(), releaseId);

		RiskAssessmentResponse dummyResponse = new RiskAssessmentResponse(
				UUID.randomUUID(),
				releaseId,
				30,
				RiskLevel.LOW,
				Instant.now(),
				List.of()
		);
		when(riskAssessmentService.assess(any(CalculateRiskRequest.class))).thenReturn(dummyResponse);

		consumer.consumeReleaseCreated(message);

		ArgumentCaptor<CalculateRiskRequest> captor = ArgumentCaptor.forClass(CalculateRiskRequest.class);
		verify(riskAssessmentService).assess(captor.capture());

		CalculateRiskRequest captured = captor.getValue();
		assertThat(captured.releaseId()).isEqualTo(releaseId);
		assertThat(captured.projectName()).isEqualTo("order-service");
		assertThat(captured.version()).isEqualTo("v1.2.0");
		assertThat(captured.changes()).isEmpty();
	}

	@Test
	void consumeReleaseCreated_withEnvelopeAndChanges_triggersAssessmentWithChanges() {
		UUID releaseId = UUID.randomUUID();
		String message = """
				{
				  "eventId": "%s",
				  "eventType": "release.created",
				  "timestamp": "2026-08-28T12:00:00Z",
				  "data": {
				    "releaseId": "%s",
				    "projectName": "auth-service",
				    "version": "v2.0.0",
				    "changes": [
				      {
				        "fileName": "db/migration/V1__init.sql",
				        "changeType": "ADDED",
				        "linesAdded": 50,
				        "linesRemoved": 0
				      },
				      {
				        "fileName": "src/security/OAuthFilter.java",
				        "changeType": "MODIFIED",
				        "linesAdded": 30,
				        "linesRemoved": 5
				      }
				    ]
				  }
				}
				""".formatted(UUID.randomUUID(), releaseId);

		RiskAssessmentResponse dummyResponse = new RiskAssessmentResponse(
				UUID.randomUUID(),
				releaseId,
				70,
				RiskLevel.HIGH,
				Instant.now(),
				List.of()
		);
		when(riskAssessmentService.assess(any(CalculateRiskRequest.class))).thenReturn(dummyResponse);

		consumer.consumeReleaseCreated(message);

		ArgumentCaptor<CalculateRiskRequest> captor = ArgumentCaptor.forClass(CalculateRiskRequest.class);
		verify(riskAssessmentService).assess(captor.capture());

		CalculateRiskRequest captured = captor.getValue();
		assertThat(captured.releaseId()).isEqualTo(releaseId);
		assertThat(captured.projectName()).isEqualTo("auth-service");
		assertThat(captured.version()).isEqualTo("v2.0.0");
		assertThat(captured.changes()).hasSize(2);
		assertThat(captured.changes().get(0).fileName()).isEqualTo("db/migration/V1__init.sql");
		assertThat(captured.changes().get(1).fileName()).isEqualTo("src/security/OAuthFilter.java");
	}

	@Test
	void consumeReleaseCreated_missingReleaseId_doesNotTriggerAssessment() {
		String message = """
				{
				  "eventId": "%s",
				  "eventType": "release.created",
				  "projectName": "billing-service"
				}
				""".formatted(UUID.randomUUID());

		consumer.consumeReleaseCreated(message);

		verify(riskAssessmentService, never()).assess(any());
	}

	@Test
	void consumeReleaseCreated_invalidUuid_doesNotTriggerAssessment() {
		String message = """
				{
				  "eventId": "%s",
				  "eventType": "release.created",
				  "releaseId": "invalid-uuid",
				  "projectName": "billing-service"
				}
				""".formatted(UUID.randomUUID());

		consumer.consumeReleaseCreated(message);

		verify(riskAssessmentService, never()).assess(any());
	}

	@Test
	void consumeReleaseCreated_malformedJson_handlesGracefully() {
		String message = "not-a-valid-json";

		consumer.consumeReleaseCreated(message);

		verify(riskAssessmentService, never()).assess(any());
	}
}
