package com.release_guard.release_service.dto.response;

import com.release_guard.release_service.entity.Release;
import com.release_guard.release_service.entity.ReleaseStatus;

import java.time.Instant;
import java.util.UUID;

public record ReleaseResponse(
		UUID id,
		String projectName,
		String version,
		String description,
		ReleaseStatus status,
		String createdBy,
		Instant createdAt,
		Instant deployedAt
) {
	public static ReleaseResponse from(Release release) {
		return new ReleaseResponse(
				release.getId(),
				release.getProjectName(),
				release.getVersion(),
				release.getDescription(),
				release.getStatus(),
				release.getCreatedBy(),
				release.getCreatedAt(),
				release.getDeployedAt()
		);
	}
}
