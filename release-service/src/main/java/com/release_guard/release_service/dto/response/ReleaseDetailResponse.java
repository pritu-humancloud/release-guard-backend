package com.release_guard.release_service.dto.response;

import com.release_guard.release_service.entity.Release;
import com.release_guard.release_service.entity.ReleaseStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReleaseDetailResponse(
		UUID id,
		String projectName,
		String version,
		String description,
		ReleaseStatus status,
		String createdBy,
		Instant createdAt,
		Instant deployedAt,
		List<ReleaseChangeResponse> changes,
		List<PullRequestInfoResponse> pullRequests
) {
	public static ReleaseDetailResponse from(Release release) {
		return new ReleaseDetailResponse(
				release.getId(),
				release.getProjectName(),
				release.getVersion(),
				release.getDescription(),
				release.getStatus(),
				release.getCreatedBy(),
				release.getCreatedAt(),
				release.getDeployedAt(),
				release.getChanges().stream().map(ReleaseChangeResponse::from).toList(),
				release.getPullRequests().stream().map(PullRequestInfoResponse::from).toList()
		);
	}
}
