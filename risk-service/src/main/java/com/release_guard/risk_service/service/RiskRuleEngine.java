package com.release_guard.risk_service.service;

import com.release_guard.risk_service.dto.request.CalculateRiskRequest;
import com.release_guard.risk_service.dto.request.ChangeSummaryItem;
import com.release_guard.risk_service.entity.RiskFactor;
import com.release_guard.risk_service.entity.RiskLevel;
import com.release_guard.risk_service.entity.RiskRule;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class RiskRuleEngine {

	static final int LARGE_DIFF_THRESHOLD = 500;
	static final int HIGH_FILE_COUNT_THRESHOLD = 15;

	private static final Pattern DATABASE_PATTERN = Pattern.compile(
			".*(\\.sql|migration|flyway|liquibase|hibernate).*", Pattern.CASE_INSENSITIVE);
	private static final Pattern AUTH_PATTERN = Pattern.compile(
			".*(auth|security|oauth|jwt|permission|rbac).*", Pattern.CASE_INSENSITIVE);
	private static final Pattern CONFIG_PATTERN = Pattern.compile(
			".*(application\\.(yml|yaml|properties)|bootstrap\\.|\\.env|/config/).*", Pattern.CASE_INSENSITIVE);

	public Evaluation evaluate(List<RiskRule> rules, CalculateRiskRequest request) {
		List<ChangeSummaryItem> changes = request.changes() == null ? List.of() : request.changes();
		List<RiskFactor> factors = new ArrayList<>();
		int score = 0;

		for (RiskRule rule : rules) {
			String reason = matchReason(rule.getName(), changes);
			if (reason == null) {
				continue;
			}
			score += rule.getWeight();
			factors.add(RiskFactor.builder()
					.ruleName(rule.getName())
					.weightApplied(rule.getWeight())
					.reason(reason)
					.build());
		}

		int clamped = Math.min(100, Math.max(0, score));
		return new Evaluation(clamped, RiskLevel.fromScore(clamped), factors);
	}

	private static String matchReason(String ruleName, List<ChangeSummaryItem> changes) {
		return switch (ruleName) {
			case "DATABASE_MIGRATION" -> firstMatchingFile(changes, DATABASE_PATTERN)
					.map(file -> "Database/schema change detected in " + file)
					.orElse(null);
			case "AUTH_SECURITY_CHANGE" -> firstMatchingFile(changes, AUTH_PATTERN)
					.map(file -> "Auth/security change detected in " + file)
					.orElse(null);
			case "CONFIG_CHANGE" -> firstMatchingFile(changes, CONFIG_PATTERN)
					.map(file -> "Configuration change detected in " + file)
					.orElse(null);
			case "DELETED_FILES" -> changes.stream()
					.filter(item -> isDeleted(item.changeType()))
					.findFirst()
					.map(item -> "Deleted file detected: " + item.fileName())
					.orElse(null);
			case "LARGE_DIFF" -> {
				int total = changes.stream().mapToInt(item -> item.linesAdded() + item.linesRemoved()).sum();
				yield total >= LARGE_DIFF_THRESHOLD
						? "Large diff of " + total + " changed lines (threshold " + LARGE_DIFF_THRESHOLD + ")"
						: null;
			}
			case "HIGH_FILE_COUNT" -> changes.size() >= HIGH_FILE_COUNT_THRESHOLD
					? "High file count: " + changes.size() + " files (threshold " + HIGH_FILE_COUNT_THRESHOLD + ")"
					: null;
			default -> firstMatchingFile(changes, customNamePattern(ruleName))
					.map(file -> "Custom rule " + ruleName + " matched file " + file)
					.orElse(null);
		};
	}

	private static java.util.Optional<String> firstMatchingFile(List<ChangeSummaryItem> changes, Pattern pattern) {
		return changes.stream()
				.map(ChangeSummaryItem::fileName)
				.filter(name -> name != null && pattern.matcher(name).matches())
				.findFirst();
	}

	private static boolean isDeleted(String changeType) {
		if (changeType == null) {
			return false;
		}
		String normalized = changeType.toUpperCase(Locale.ROOT);
		return "DELETED".equals(normalized) || "REMOVED".equals(normalized);
	}

	private static Pattern customNamePattern(String ruleName) {
		String[] parts = ruleName.toLowerCase(Locale.ROOT).split("_+");
		StringBuilder regex = new StringBuilder(".*");
		for (int i = 0; i < parts.length; i++) {
			if (parts[i].isBlank()) {
				continue;
			}
			if (i > 0) {
				regex.append(".*");
			}
			regex.append(Pattern.quote(parts[i]));
		}
		regex.append(".*");
		return Pattern.compile(regex.toString(), Pattern.CASE_INSENSITIVE);
	}

	public record Evaluation(int score, RiskLevel riskLevel, List<RiskFactor> factors) {
	}
}
