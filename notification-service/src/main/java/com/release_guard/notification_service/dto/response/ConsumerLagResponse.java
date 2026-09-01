package com.release_guard.notification_service.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

public record ConsumerLagResponse(
		@Schema(description = "Execution status", example = "SUCCESS")
		String status,

		@Schema(description = "Human-readable summary", example = "Published 10 test event(s) to Kafka. notification-service is consuming events automatically in the background.")
		String message,

		@Schema(description = "Number of events published to Kafka", example = "10")
		int eventsPublished,

		@Schema(description = "Event type simulated", example = "ALL")
		String eventType,

		@Schema(description = "Target Kafka topics", example = "[\"risk.calculated\", \"release.status.changed\", \"health.incident.created\"]")
		List<String> targetTopics,

		@Schema(description = "Consumer group monitoring target", example = "notification-service")
		String targetConsumerGroup,

		@Schema(description = "Sum of consumer lag across all topic partitions", example = "0")
		long totalLag,

		@Schema(description = "Per-partition offset and lag breakdown")
		List<PartitionLagInfo> partitionDetails,

		@Schema(description = "Helpful endpoints to verify automatic consumption and notifications")
		Map<String, String> verificationEndpoints,

		@Schema(description = "Sample list of event payloads published")
		List<Object> sampleEvents
) {
}
