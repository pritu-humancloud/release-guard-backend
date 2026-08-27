package com.release_guard.risk_service.entity;

public enum RiskLevel {
	LOW,
	MEDIUM,
	HIGH;

	public static RiskLevel fromScore(int score) {
		if (score >= 67) {
			return HIGH;
		}
		if (score >= 34) {
			return MEDIUM;
		}
		return LOW;
	}
}
