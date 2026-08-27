package com.release_guard.risk_service.service;

import com.release_guard.risk_service.dto.event.RiskCalculatedEvent;
import com.release_guard.risk_service.dto.request.CalculateRiskRequest;
import com.release_guard.risk_service.dto.request.CreateRiskRuleRequest;
import com.release_guard.risk_service.dto.request.UpdateRiskRuleRequest;
import com.release_guard.risk_service.dto.response.RiskAssessmentResponse;
import com.release_guard.risk_service.dto.response.RiskRuleResponse;
import com.release_guard.risk_service.entity.RiskAssessment;
import com.release_guard.risk_service.entity.RiskFactor;
import com.release_guard.risk_service.entity.RiskRule;
import com.release_guard.risk_service.exception.ResourceNotFoundException;
import com.release_guard.risk_service.kafka.RiskEventPublisher;
import com.release_guard.risk_service.repository.RiskAssessmentRepository;
import com.release_guard.risk_service.repository.RiskRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RiskAssessmentService {

	private final RiskRuleRepository riskRuleRepository;
	private final RiskAssessmentRepository riskAssessmentRepository;
	private final RiskRuleEngine riskRuleEngine;
	private final RiskCacheService riskCacheService;
	private final RiskEventPublisher eventPublisher;
	private final AdminAccess adminAccess;

	@Transactional
	public RiskAssessmentResponse assess(CalculateRiskRequest request) {
		List<RiskRule> activeRules = riskRuleRepository.findAllByActiveTrue();
		RiskRuleEngine.Evaluation evaluation = riskRuleEngine.evaluate(activeRules, request);

		RiskAssessment assessment = RiskAssessment.builder()
				.releaseId(request.releaseId())
				.score(evaluation.score())
				.riskLevel(evaluation.riskLevel())
				.calculatedAt(Instant.now())
				.build();

		for (RiskFactor factor : evaluation.factors()) {
			factor.setAssessment(assessment);
			assessment.getFactors().add(factor);
		}

		RiskAssessment saved = riskAssessmentRepository.save(assessment);
		RiskAssessmentResponse response = RiskAssessmentResponse.from(saved);

		riskCacheService.put(response);
		eventPublisher.publishCalculated(RiskCalculatedEvent.from(saved));

		log.info("Calculated risk for release {}: score={}, level={}",
				request.releaseId(), saved.getScore(), saved.getRiskLevel());

		return response;
	}

	public RiskAssessmentResponse getByReleaseId(UUID releaseId) {
		return riskCacheService.get(releaseId)
				.orElseGet(() -> riskAssessmentRepository.findFirstByReleaseIdOrderByCalculatedAtDesc(releaseId)
						.map(assessment -> {
							RiskAssessmentResponse response = RiskAssessmentResponse.from(assessment);
							riskCacheService.put(response);
							return response;
						})
						.orElseThrow(() -> ResourceNotFoundException.assessment(releaseId)));
	}

	public List<RiskRuleResponse> listRules() {
		return riskRuleRepository.findAllByOrderByNameAsc().stream()
				.map(RiskRuleResponse::from)
				.toList();
	}

	@Transactional
	public RiskRuleResponse createRule(String adminKey, CreateRiskRuleRequest request) {
		adminAccess.require(adminKey);

		if (riskRuleRepository.existsByNameIgnoreCase(request.name().trim())) {
			throw new IllegalArgumentException("Rule with name already exists: " + request.name());
		}

		RiskRule rule = RiskRule.builder()
				.name(request.name().trim().toUpperCase())
				.weight(request.weight())
				.description(request.description())
				.active(request.active() != null ? request.active() : true)
				.build();

		RiskRule saved = riskRuleRepository.save(rule);
		return RiskRuleResponse.from(saved);
	}

	@Transactional
	public RiskRuleResponse updateRule(String adminKey, UUID id, UpdateRiskRuleRequest request) {
		adminAccess.require(adminKey);

		RiskRule rule = riskRuleRepository.findById(id)
				.orElseThrow(() -> ResourceNotFoundException.rule(id));

		if (request.weight() != null) {
			rule.setWeight(request.weight());
		}
		if (request.active() != null) {
			rule.setActive(request.active());
		}

		RiskRule saved = riskRuleRepository.save(rule);
		return RiskRuleResponse.from(saved);
	}

	@Transactional
	public void deleteRule(String adminKey, UUID id) {
		adminAccess.require(adminKey);
		if (!riskRuleRepository.existsById(id)) {
			throw ResourceNotFoundException.rule(id);
		}
		riskRuleRepository.deleteById(id);
	}
}
