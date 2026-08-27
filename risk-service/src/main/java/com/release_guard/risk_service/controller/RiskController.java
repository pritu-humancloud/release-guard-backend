package com.release_guard.risk_service.controller;

import com.release_guard.risk_service.dto.request.CalculateRiskRequest;
import com.release_guard.risk_service.dto.request.CreateRiskRuleRequest;
import com.release_guard.risk_service.dto.request.UpdateRiskRuleRequest;
import com.release_guard.risk_service.dto.response.RiskAssessmentResponse;
import com.release_guard.risk_service.dto.response.RiskRuleResponse;
import com.release_guard.risk_service.service.RiskAssessmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/risk")
@RequiredArgsConstructor
@Tag(name = "Risk Assessment")
public class RiskController {

	private final RiskAssessmentService riskAssessmentService;

	@PostMapping("/assess")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Evaluate changes and calculate risk assessment for a release")
	public RiskAssessmentResponse assess(@Valid @RequestBody CalculateRiskRequest request) {
		return riskAssessmentService.assess(request);
	}

	@GetMapping("/releases/{releaseId}")
	@Operation(summary = "Get the latest risk assessment for a release")
	public RiskAssessmentResponse getByReleaseId(@PathVariable UUID releaseId) {
		return riskAssessmentService.getByReleaseId(releaseId);
	}

	@GetMapping("/rules")
	@Operation(summary = "List all risk scoring rules")
	public List<RiskRuleResponse> listRules() {
		return riskAssessmentService.listRules();
	}

	@PostMapping("/rules")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create a risk rule (Requires Admin Key)")
	public RiskRuleResponse createRule(
			@RequestHeader(name = "X-Admin-Key", required = false)
			@Parameter(description = "Admin access key") String adminKey,
			@Valid @RequestBody CreateRiskRuleRequest request
	) {
		return riskAssessmentService.createRule(adminKey, request);
	}

	@PutMapping("/rules/{id}")
	@Operation(summary = "Update an existing risk rule (Requires Admin Key)")
	public RiskRuleResponse updateRule(
			@RequestHeader(name = "X-Admin-Key", required = false)
			@Parameter(description = "Admin access key") String adminKey,
			@PathVariable UUID id,
			@Valid @RequestBody UpdateRiskRuleRequest request
	) {
		return riskAssessmentService.updateRule(adminKey, id, request);
	}

	@DeleteMapping("/rules/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Delete a risk rule (Requires Admin Key)")
	public void deleteRule(
			@RequestHeader(name = "X-Admin-Key", required = false)
			@Parameter(description = "Admin access key") String adminKey,
			@PathVariable UUID id
	) {
		riskAssessmentService.deleteRule(adminKey, id);
	}
}
