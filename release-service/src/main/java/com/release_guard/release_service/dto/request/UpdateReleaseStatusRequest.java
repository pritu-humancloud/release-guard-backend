package com.release_guard.release_service.dto.request;

import com.release_guard.release_service.entity.ReleaseStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateReleaseStatusRequest(
		@NotNull ReleaseStatus status
) {
}
