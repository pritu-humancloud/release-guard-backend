package com.release_guard.risk_service.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateRiskRuleRequest(
		@NotBlank String name,
		@NotNull @Min(0) @Max(100) Integer weight,
		String description,
		Boolean active
) {
}
