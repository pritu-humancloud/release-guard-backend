package com.release_guard.risk_service.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpdateRiskRuleRequest(
		@Min(0) @Max(100) Integer weight,
		Boolean active
) {
}
