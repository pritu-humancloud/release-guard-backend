package com.release_guard.risk_service.dto.response;

import com.release_guard.risk_service.entity.RiskAssessment;
import com.release_guard.risk_service.entity.RiskLevel;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RiskAssessmentResponse(
		UUID id,
		UUID releaseId,
		int score,
		RiskLevel riskLevel,
		Instant calculatedAt,
		List<RiskFactorResponse> factors
) {
	public static RiskAssessmentResponse from(RiskAssessment assessment) {
		return new RiskAssessmentResponse(
				assessment.getId(),
				assessment.getReleaseId(),
				assessment.getScore(),
				assessment.getRiskLevel(),
				assessment.getCalculatedAt(),
				assessment.getFactors().stream().map(RiskFactorResponse::from).toList()
		);
	}
}
