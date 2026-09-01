package com.release_guard.release_service.dto.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubPullResponse(
		int number,
		String title,
		GitHubUser user,
		@JsonProperty("merged_at") Instant mergedAt
) {
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record GitHubUser(String login) {
	}
}
