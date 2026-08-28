package com.release_guard.release_service.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

public record ConsumerLagResponse(
		@Schema(description = "Execution status", example = "SUCCESS")
		String status,

		@Schema(description = "Human-readable summary of the test execution", example = "Published 10 test event(s) to Kafka. Consumer will process them automatically in the background.")
		String message,

		@Schema(description = "Number of events published to Kafka", example = "10")
		int eventsPublished,

		@Schema(description = "Event type simulated", example = "ALL")
		String eventType,

		@Schema(description = "Target Kafka topics published to", example = "[\"risk.calculated\", \"release.status.changed\"]")
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
