package com.release_guard.release_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record GitHubSyncRequest(
		@NotBlank String repoName,
		@NotNull @Positive Integer prNumber
) {
}
