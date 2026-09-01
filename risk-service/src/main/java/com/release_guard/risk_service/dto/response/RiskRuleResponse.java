package com.release_guard.risk_service.dto.response;

import com.release_guard.risk_service.entity.RiskRule;

import java.util.UUID;

public record RiskRuleResponse(
		UUID id,
		String name,
		int weight,
		String description,
		boolean active
) {
	public static RiskRuleResponse from(RiskRule rule) {
		return new RiskRuleResponse(
				rule.getId(),
				rule.getName(),
				rule.getWeight(),
				rule.getDescription(),
				rule.isActive()
		);
	}
}
