package com.release_guard.release_service.dto.event;

import com.release_guard.release_service.entity.ReleaseStatus;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public record ReleaseStatusChangedEvent(
		UUID eventId,
		String eventType,
		UUID releaseId,
		Instant timestamp,
		Map<String, Object> data,
		String projectName,
		String version,
		ReleaseStatus previousStatus,
		ReleaseStatus newStatus
) {
	public static ReleaseStatusChangedEvent of(
			UUID releaseId,
			String projectName,
			String version,
			ReleaseStatus previousStatus,
			ReleaseStatus newStatus
	) {
		UUID eventId = UUID.randomUUID();
		Instant now = Instant.now();

		Map<String, Object> data = new HashMap<>();
		data.put("releaseId", releaseId);
		data.put("projectName", projectName);
		data.put("version", version);
		data.put("previousStatus", previousStatus != null ? previousStatus.name() : null);
		data.put("newStatus", newStatus != null ? newStatus.name() : null);

		return new ReleaseStatusChangedEvent(
				eventId,
				"release.status.changed",
				releaseId,
				now,
				data,
				projectName,
				version,
				previousStatus,
				newStatus
		);
	}
}
