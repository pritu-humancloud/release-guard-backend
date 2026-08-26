package com.release_guard.release_service.dto.response;

import com.release_guard.release_service.entity.PullRequestInfo;

import java.time.Instant;
import java.util.UUID;

public record PullRequestInfoResponse(
		UUID id,
		String repoName,
		int prNumber,
		String title,
		String author,
		Instant mergedAt
) {
	public static PullRequestInfoResponse from(PullRequestInfo info) {
		return new PullRequestInfoResponse(
				info.getId(),
				info.getRepoName(),
				info.getPrNumber(),
				info.getTitle(),
				info.getAuthor(),
				info.getMergedAt()
		);
	}
}
