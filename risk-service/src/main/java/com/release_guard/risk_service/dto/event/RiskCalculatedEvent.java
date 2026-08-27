package com.release_guard.risk_service.dto.event;

import com.release_guard.risk_service.entity.RiskAssessment;
import com.release_guard.risk_service.entity.RiskLevel;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public record RiskCalculatedEvent(
		UUID eventId,
		String eventType,
		UUID releaseId,
		Instant timestamp,
		Map<String, Object> data,
		UUID assessmentId,
		int score,
		RiskLevel riskLevel
) {
	public static RiskCalculatedEvent from(RiskAssessment assessment) {
		UUID eventId = UUID.randomUUID();
		Instant now = Instant.now();

		Map<String, Object> data = new HashMap<>();
		data.put("releaseId", assessment.getReleaseId());
		data.put("assessmentId", assessment.getId());
		data.put("score", assessment.getScore());
		data.put("riskLevel", assessment.getRiskLevel() != null ? assessment.getRiskLevel().name() : null);
		data.put("calculatedAt", assessment.getCalculatedAt());

		return new RiskCalculatedEvent(
				eventId,
				"risk.calculated",
				assessment.getReleaseId(),
				now,
				data,
				assessment.getId(),
				assessment.getScore(),
				assessment.getRiskLevel()
		);
	}
}
