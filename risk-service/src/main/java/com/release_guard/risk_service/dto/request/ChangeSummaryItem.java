package com.release_guard.risk_service.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ChangeSummaryItem(
		@NotBlank String fileName,
		String changeType,
		int linesAdded,
		int linesRemoved
) {
}
