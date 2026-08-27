package com.release_guard.risk_service.dto.response;

import com.release_guard.risk_service.entity.RiskFactor;

import java.util.UUID;

public record RiskFactorResponse(
		UUID id,
		String ruleName,
		int weightApplied,
		String reason
) {
	public static RiskFactorResponse from(RiskFactor factor) {
		return new RiskFactorResponse(
				factor.getId(),
				factor.getRuleName(),
				factor.getWeightApplied(),
				factor.getReason()
		);
	}
}
