package com.release_guard.release_service.dto.event;

import com.release_guard.release_service.entity.Release;
import com.release_guard.release_service.entity.ReleaseStatus;

import java.time.Instant;
import java.util.UUID;

public record ReleaseCreatedEvent(
		String eventType,
		UUID releaseId,
		String projectName,
		String version,
		ReleaseStatus status,
		String createdBy,
		Instant occurredAt
) {
	public static ReleaseCreatedEvent from(Release release) {
		return new ReleaseCreatedEvent(
				"release.created",
				release.getId(),
				release.getProjectName(),
				release.getVersion(),
				release.getStatus(),
				release.getCreatedBy(),
				Instant.now()
		);
	}
}
