package com.release_guard.risk_service.service;

import com.release_guard.risk_service.dto.request.CalculateRiskRequest;
import com.release_guard.risk_service.dto.request.ChangeSummaryItem;
import com.release_guard.risk_service.entity.RiskLevel;
import com.release_guard.risk_service.entity.RiskRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RiskRuleEngineTest {

	private RiskRuleEngine engine;
	private List<RiskRule> defaultRules;

	@BeforeEach
	void setUp() {
		engine = new RiskRuleEngine();
		defaultRules = List.of(
				RiskRule.builder().name("DATABASE_MIGRATION").weight(30).active(true).build(),
				RiskRule.builder().name("AUTH_SECURITY_CHANGE").weight(40).active(true).build(),
				RiskRule.builder().name("CONFIG_CHANGE").weight(20).active(true).build(),
				RiskRule.builder().name("DELETED_FILES").weight(15).active(true).build(),
				RiskRule.builder().name("LARGE_DIFF").weight(25).active(true).build(),
				RiskRule.builder().name("HIGH_FILE_COUNT").weight(20).active(true).build()
		);
	}

	@Test
	void evaluate_lowRiskSmallChange_returnsLow() {
		CalculateRiskRequest request = new CalculateRiskRequest(
				UUID.randomUUID(),
				"user-service",
				"v1.0.0",
				List.of(new ChangeSummaryItem("src/main/java/UserHelper.java", "MODIFIED", 10, 5))
		);

		RiskRuleEngine.Evaluation eval = engine.evaluate(defaultRules, request);

		assertThat(eval.score()).isEqualTo(0);
		assertThat(eval.riskLevel()).isEqualTo(RiskLevel.LOW);
		assertThat(eval.factors()).isEmpty();
	}

	@Test
	void evaluate_databaseAndSecurityChange_accumulatesScoreAndTriggersHighRisk() {
		CalculateRiskRequest request = new CalculateRiskRequest(
				UUID.randomUUID(),
				"core-service",
				"v1.2.0",
				List.of(
						new ChangeSummaryItem("src/main/resources/db/migration/V2__users.sql", "ADDED", 50, 0),
						new ChangeSummaryItem("src/main/java/security/JwtAuthFilter.java", "MODIFIED", 80, 20)
				)
		);

		RiskRuleEngine.Evaluation eval = engine.evaluate(defaultRules, request);

		// 30 (DATABASE_MIGRATION) + 40 (AUTH_SECURITY_CHANGE) = 70
		assertThat(eval.score()).isEqualTo(70);
		assertThat(eval.riskLevel()).isEqualTo(RiskLevel.HIGH);
		assertThat(eval.factors()).hasSize(2);
	}

	@Test
	void evaluate_criticalAllRulesTriggered_clampedTo100AndCritical() {
		List<ChangeSummaryItem> changes = new java.util.ArrayList<>();
		changes.add(new ChangeSummaryItem("db/migration/V1.sql", "ADDED", 300, 0));
		changes.add(new ChangeSummaryItem("src/security/OAuth2Config.java", "MODIFIED", 200, 50));
		changes.add(new ChangeSummaryItem("application.yml", "MODIFIED", 10, 2));
		changes.add(new ChangeSummaryItem("old/DeprecatedUtil.java", "DELETED", 0, 100));

		// Add more files to trigger HIGH_FILE_COUNT (>= 15)
		for (int i = 1; i <= 15; i++) {
			changes.add(new ChangeSummaryItem("service/Service" + i + ".java", "MODIFIED", 5, 2));
		}

		CalculateRiskRequest request = new CalculateRiskRequest(
				UUID.randomUUID(),
				"enterprise-app",
				"v2.0.0",
				changes
		);

		RiskRuleEngine.Evaluation eval = engine.evaluate(defaultRules, request);

		assertThat(eval.score()).isGreaterThanOrEqualTo(80);
		assertThat(eval.score()).isLessThanOrEqualTo(100);
		assertThat(eval.riskLevel()).isEqualTo(RiskLevel.HIGH);
	}
}
