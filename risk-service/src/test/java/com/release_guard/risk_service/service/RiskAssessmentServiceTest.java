package com.release_guard.risk_service.service;

import com.release_guard.risk_service.dto.request.CalculateRiskRequest;
import com.release_guard.risk_service.dto.request.ChangeSummaryItem;
import com.release_guard.risk_service.dto.request.CreateRiskRuleRequest;
import com.release_guard.risk_service.dto.response.RiskAssessmentResponse;
import com.release_guard.risk_service.dto.response.RiskRuleResponse;
import com.release_guard.risk_service.entity.RiskAssessment;
import com.release_guard.risk_service.entity.RiskLevel;
import com.release_guard.risk_service.entity.RiskRule;
import com.release_guard.risk_service.kafka.RiskEventPublisher;
import com.release_guard.risk_service.repository.RiskAssessmentRepository;
import com.release_guard.risk_service.repository.RiskRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskAssessmentServiceTest {

	@Mock
	private RiskRuleRepository riskRuleRepository;
	@Mock
	private RiskAssessmentRepository riskAssessmentRepository;
	@Mock
	private RiskCacheService riskCacheService;
	@Mock
	private RiskEventPublisher eventPublisher;

	private RiskRuleEngine riskRuleEngine;
	private AdminAccess adminAccess;
	private RiskAssessmentService service;

	@BeforeEach
	void setUp() {
		riskRuleEngine = new RiskRuleEngine();
		adminAccess = new AdminAccess("test-admin-key");
		service = new RiskAssessmentService(
				riskRuleRepository,
				riskAssessmentRepository,
				riskRuleEngine,
				riskCacheService,
				eventPublisher,
				adminAccess
		);
	}

	@Test
	void assess_calculatesAndSavesAssessment() {
		UUID releaseId = UUID.randomUUID();
		List<RiskRule> rules = List.of(
				RiskRule.builder().name("DATABASE_MIGRATION").weight(30).active(true).build()
		);
		when(riskRuleRepository.findAllByActiveTrue()).thenReturn(rules);

		when(riskAssessmentRepository.save(any(RiskAssessment.class))).thenAnswer(inv -> {
			RiskAssessment a = inv.getArgument(0);
			a.setId(UUID.randomUUID());
			return a;
		});

		CalculateRiskRequest request = new CalculateRiskRequest(
				releaseId,
				"auth-service",
				"v1.0.0",
				List.of(new ChangeSummaryItem("src/main/resources/schema.sql", "ADDED", 20, 0))
		);

		RiskAssessmentResponse response = service.assess(request);

		assertThat(response.score()).isEqualTo(30);
		assertThat(response.riskLevel()).isEqualTo(RiskLevel.LOW);
		assertThat(response.releaseId()).isEqualTo(releaseId);

		verify(riskCacheService).put(any(RiskAssessmentResponse.class));
		verify(eventPublisher).publishCalculated(any());
	}

	@Test
	void createRule_savesNewRule() {
		when(riskRuleRepository.existsByNameIgnoreCase("CUSTOM_RULE")).thenReturn(false);
		when(riskRuleRepository.save(any(RiskRule.class))).thenAnswer(inv -> {
			RiskRule r = inv.getArgument(0);
			r.setId(UUID.randomUUID());
			return r;
		});

		CreateRiskRuleRequest req = new CreateRiskRuleRequest("CUSTOM_RULE", 25, "Custom rule description", true);
		RiskRuleResponse res = service.createRule("test-admin-key", req);

		assertThat(res.name()).isEqualTo("CUSTOM_RULE");
		assertThat(res.weight()).isEqualTo(25);
	}
}
