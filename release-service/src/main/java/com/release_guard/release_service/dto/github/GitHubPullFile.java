package com.release_guard.release_service.dto.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubPullFile(
		String filename,
		String status,
		int additions,
		int deletions
) {
}
