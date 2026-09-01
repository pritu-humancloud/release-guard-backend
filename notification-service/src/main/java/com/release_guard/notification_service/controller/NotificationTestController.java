package com.release_guard.notification_service.controller;

import com.release_guard.notification_service.dto.request.ConsumerLagTestRequest;
import com.release_guard.notification_service.dto.response.ConsumerLagResponse;
import com.release_guard.notification_service.service.NotificationKafkaLagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Kafka Consumer Lag Testing", description = "Endpoints to test Kafka event publishing, measure consumer lag, and verify automated background consumption in Notification Service")
public class NotificationTestController {

	private final NotificationKafkaLagService notificationKafkaLagService;

	@PostMapping("/test/consumer-lag")
	@ResponseStatus(HttpStatus.OK)
	@Operation(
			summary = "Test Kafka Consumer Lag & Automated Consumption",
			description = "Publishes test events to Kafka topics in burst to simulate load/lag. The Notification Service automatically consumes them via @KafkaListener without any manual API hit."
	)
	public ConsumerLagResponse testConsumerLag(
			@RequestParam(required = false) @Parameter(description = "Number of events to publish (default 10)") Integer count,
			@RequestParam(required = false) @Parameter(description = "Event type: RISK_HIGH, RELEASE_BLOCKED, INCIDENT, or ALL") String eventType,
			@RequestParam(required = false) @Parameter(description = "Delay between publishes in ms (default 0)") Long delayMs,
			@RequestParam(required = false) @Parameter(description = "Project name") String projectName,
			@RequestBody(required = false) ConsumerLagTestRequest body
	) {
		Integer resolvedCount = body != null && body.count() != null ? body.count() : count;
		String resolvedEventType = body != null && body.eventType() != null ? body.eventType() : eventType;
		Long resolvedDelayMs = body != null && body.delayMs() != null ? body.delayMs() : delayMs;
		String resolvedProject = body != null && body.projectName() != null ? body.projectName() : projectName;

		ConsumerLagTestRequest request = new ConsumerLagTestRequest(
				resolvedCount,
				resolvedEventType,
				resolvedDelayMs,
				resolvedProject
		);

		return notificationKafkaLagService.simulateAndMeasureLag(request);
	}

	@GetMapping("/consumer-lag")
	@Operation(
			summary = "Get Real-time Kafka Consumer Lag Metrics",
			description = "Fetches latest topic partition log end offsets, consumer group committed offsets, and consumer lag for notification-service."
	)
	public ConsumerLagResponse getConsumerLagMetrics() {
		return notificationKafkaLagService.getCurrentLagStatus();
	}
}
