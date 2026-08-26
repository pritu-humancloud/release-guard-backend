package com.release_guard.release_service.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateReleaseRequest(
		@NotBlank String projectName,
		@NotBlank String version,
		String description,
		@NotBlank String createdBy
) {
}
