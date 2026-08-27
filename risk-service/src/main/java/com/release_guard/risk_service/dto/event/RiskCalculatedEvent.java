package com.release_guard.risk_service.dto.event;

import com.release_guard.risk_service.entity.RiskAssessment;
import com.release_guard.risk_service.entity.RiskLevel;

import java.time.Instant;
import java.util.UUID;

public record RiskCalculatedEvent(
		String eventType,
		UUID releaseId,
		UUID assessmentId,
		int score,
		RiskLevel riskLevel,
		Instant occurredAt
) {
	public static RiskCalculatedEvent from(RiskAssessment assessment) {
		return new RiskCalculatedEvent(
				"risk.calculated",
				assessment.getReleaseId(),
				assessment.getId(),
				assessment.getScore(),
				assessment.getRiskLevel(),
				Instant.now()
		);
	}
}
