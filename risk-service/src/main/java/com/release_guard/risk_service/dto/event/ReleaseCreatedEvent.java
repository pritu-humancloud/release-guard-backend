package com.release_guard.risk_service.dto.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ReleaseCreatedEvent(
		String eventType,
		UUID releaseId,
		String projectName,
		String version,
		String status,
		String createdBy,
		Instant occurredAt
) {
}
