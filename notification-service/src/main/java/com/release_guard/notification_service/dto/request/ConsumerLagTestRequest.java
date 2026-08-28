package com.release_guard.notification_service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record ConsumerLagTestRequest(
		@Schema(description = "Number of test events to publish in burst (default: 10)", example = "10")
		Integer count,

		@Schema(description = "Event type to simulate: RISK_HIGH, RELEASE_BLOCKED, INCIDENT, or ALL", example = "ALL")
		String eventType,

		@Schema(description = "Delay in milliseconds between published events (0 for instant burst)", example = "0")
		Long delayMs,

		@Schema(description = "Project name to use in event payloads", example = "payment-gateway")
		String projectName
) {
	public int countOrDefault() {
		return count != null && count > 0 ? count : 10;
	}

	public String eventTypeOrDefault() {
		return eventType != null && !eventType.isBlank() ? eventType.toUpperCase() : "ALL";
	}

	public long delayMsOrDefault() {
		return delayMs != null && delayMs >= 0 ? delayMs : 0L;
	}

	public String projectNameOrDefault() {
		return projectName != null && !projectName.isBlank() ? projectName : "payment-gateway";
	}
}
