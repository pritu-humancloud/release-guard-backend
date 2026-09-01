package com.release_guard.risk_service.dto.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ReleaseCreatedEvent(
		UUID eventId,
		String eventType,
		UUID releaseId,
		Instant timestamp,
		Map<String, Object> data,
		String projectName,
		String version,
		String status,
		String createdBy,
		Instant occurredAt
) {
}
