package com.release_guard.risk_service.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CalculateRiskRequest(
		@NotNull UUID releaseId,
		String projectName,
		String version,
		@Valid List<ChangeSummaryItem> changes
) {
}
