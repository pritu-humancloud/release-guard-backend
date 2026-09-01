package com.release_guard.release_service.dto.event;

import com.release_guard.release_service.entity.Release;
import com.release_guard.release_service.entity.ReleaseStatus;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public record ReleaseCreatedEvent(
		UUID eventId,
		String eventType,
		UUID releaseId,
		Instant timestamp,
		Map<String, Object> data,
		String projectName,
		String version,
		ReleaseStatus status,
		String createdBy
) {
	public static ReleaseCreatedEvent from(Release release) {
		UUID eventId = UUID.randomUUID();
		Instant now = Instant.now();

		Map<String, Object> data = new HashMap<>();
		data.put("releaseId", release.getId());
		data.put("projectName", release.getProjectName());
		data.put("version", release.getVersion());
		data.put("status", release.getStatus() != null ? release.getStatus().name() : null);
		data.put("createdBy", release.getCreatedBy());

		return new ReleaseCreatedEvent(
				eventId,
				"release.created",
				release.getId(),
				now,
				data,
				release.getProjectName(),
				release.getVersion(),
				release.getStatus(),
				release.getCreatedBy()
		);
	}
}
