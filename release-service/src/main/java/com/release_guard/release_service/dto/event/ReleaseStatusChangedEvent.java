package com.release_guard.release_service.dto.event;

import com.release_guard.release_service.entity.ReleaseStatus;

import java.time.Instant;
import java.util.UUID;

public record ReleaseStatusChangedEvent(
		String eventType,
		UUID releaseId,
		String projectName,
		String version,
		ReleaseStatus previousStatus,
		ReleaseStatus newStatus,
		Instant occurredAt
) {
}
