package com.release_guard.risk_service.service;

import com.release_guard.risk_service.dto.request.CalculateRiskRequest;
import com.release_guard.risk_service.dto.request.ChangeSummaryItem;
import com.release_guard.risk_service.entity.RiskFactor;
import com.release_guard.risk_service.entity.RiskLevel;
import com.release_guard.risk_service.entity.RiskRule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Slf4j
@Component
public class RiskRuleEngine {

	/*
	 * Thresholds
	 */
	static final int LARGE_DIFF_THRESHOLD = 500;
	static final int HIGH_FILE_COUNT_THRESHOLD = 15;

	/*
	 * File patterns
	 */
	private static final Pattern DATABASE_PATTERN = Pattern.compile(
			".*(\\.sql|migration|flyway|liquibase|hibernate).*",
			Pattern.CASE_INSENSITIVE
	);

	private static final Pattern AUTH_PATTERN = Pattern.compile(
			".*(auth|security|oauth|jwt|permission|rbac).*",
			Pattern.CASE_INSENSITIVE
	);

	private static final Pattern CONFIG_PATTERN = Pattern.compile(
			".*(application\\.(yml|yaml|properties)|bootstrap\\.|\\.env|[/\\\\]config[/\\\\]).*",
			Pattern.CASE_INSENSITIVE
	);

	/**
	 * Evaluates all configured risk rules against the changed files.
	 */
	public Evaluation evaluate(
			List<RiskRule> rules,
			CalculateRiskRequest request
	) {

		List<ChangeSummaryItem> changes =
				request == null || request.changes() == null
						? List.of()
						: request.changes();

		List<RiskRule> activeRules =
				rules == null
						? List.of()
						: rules;

		List<RiskFactor> factors = new ArrayList<>();

		int score = 0;

		log.info(
				"Starting risk evaluation. Changed files={}, configured rules={}",
				changes.size(),
				activeRules.size()
		);

		/*
		 * Evaluate every configured rule.
		 */
		for (RiskRule rule : activeRules) {

			if (rule == null || rule.getName() == null) {
				continue;
			}

			String ruleName = normalizeRuleName(rule.getName());

			int weight = Math.max(0, rule.getWeight());

			String reason = matchReason(ruleName, changes);

			if (reason == null) {

				log.debug(
						"Risk rule did not match: {}",
						ruleName
				);

				continue;
			}

			score += weight;

			factors.add(
					RiskFactor.builder()
							.ruleName(ruleName)
							.weightApplied(weight)
							.reason(reason)
							.build()
			);

			log.info(
					"Risk rule matched: {} | weight={} | reason={}",
					ruleName,
					weight,
					reason
			);
		}

		/*
		 * Never allow the score to go outside 0-100.
		 */
		int clampedScore = Math.min(
				100,
				Math.max(0, score)
		);

		RiskLevel riskLevel = RiskLevel.fromScore(clampedScore);

		log.info(
				"Risk evaluation completed. Raw score={}, final score={}, riskLevel={}, factors={}",
				score,
				clampedScore,
				riskLevel,
				factors.size()
		);

		return new Evaluation(
				clampedScore,
				riskLevel,
				factors
		);
	}

	/**
	 * Determines whether a particular risk rule matches the change set.
	 */
	private static String matchReason(
			String ruleName,
			List<ChangeSummaryItem> changes
	) {

		return switch (ruleName) {

			/*
			 * Database / schema changes
			 */
			case "DATABASE_MIGRATION" -> {

				List<String> files =
						matchingFiles(changes, DATABASE_PATTERN);

				yield files.isEmpty()
						? null
						: "Database/schema changes detected in "
						+ files.size()
						+ " file(s): "
						+ summarizeFiles(files);
			}

			/*
			 * Authentication / security changes
			 */
			case "AUTH_SECURITY_CHANGE" -> {

				List<String> files =
						matchingFiles(changes, AUTH_PATTERN);

				yield files.isEmpty()
						? null
						: "Auth/security changes detected in "
						+ files.size()
						+ " file(s): "
						+ summarizeFiles(files);
			}

			/*
			 * Configuration changes
			 */
			case "CONFIG_CHANGE" -> {

				List<String> files =
						matchingFiles(changes, CONFIG_PATTERN);

				yield files.isEmpty()
						? null
						: "Configuration changes detected in "
						+ files.size()
						+ " file(s): "
						+ summarizeFiles(files);
			}

			/*
			 * Deleted files
			 */
			case "DELETED_FILES" -> {

				List<String> deletedFiles = changes.stream()
						.filter(item ->
								item != null &&
										isDeleted(item.changeType())
						)
						.map(ChangeSummaryItem::fileName)
						.filter(name -> name != null && !name.isBlank())
						.toList();

				yield deletedFiles.isEmpty()
						? null
						: "Deleted files detected: "
						+ deletedFiles.size()
						+ " file(s): "
						+ summarizeFiles(deletedFiles);
			}

			/*
			 * Large total diff.
			 */
			case "LARGE_DIFF" -> {

				int totalAdded = changes.stream()
						.filter(item -> item != null)
						.mapToInt(ChangeSummaryItem::linesAdded)
						.sum();

				int totalRemoved = changes.stream()
						.filter(item -> item != null)
						.mapToInt(ChangeSummaryItem::linesRemoved)
						.sum();

				int totalChangedLines =
						totalAdded + totalRemoved;

				yield totalChangedLines >= LARGE_DIFF_THRESHOLD
						? "Large diff detected: "
						+ totalChangedLines
						+ " changed lines "
						+ "(added="
						+ totalAdded
						+ ", removed="
						+ totalRemoved
						+ ", threshold="
						+ LARGE_DIFF_THRESHOLD
						+ ")"
						: null;
			}

			/*
			 * Large number of changed files.
			 */
			case "HIGH_FILE_COUNT" -> {

				int fileCount = changes.size();

				yield fileCount >= HIGH_FILE_COUNT_THRESHOLD
						? "High file count: "
						+ fileCount
						+ " files "
						+ "(threshold="
						+ HIGH_FILE_COUNT_THRESHOLD
						+ ")"
						: null;
			}

			/*
			 * Custom rule.
			 *
			 * Example:
			 *
			 * Rule name:
			 * PAYMENT_SERVICE_CHANGE
			 *
			 * Will look for:
			 * payment
			 * service
			 *
			 * anywhere in the file path.
			 */
			default -> {

				Pattern customPattern =
						customNamePattern(ruleName);

				List<String> files =
						matchingFiles(changes, customPattern);

				yield files.isEmpty()
						? null
						: "Custom rule "
						+ ruleName
						+ " matched "
						+ files.size()
						+ " file(s): "
						+ summarizeFiles(files);
			}
		};
	}

	/**
	 * Returns all files matching a pattern.
	 *
	 * Uses find() instead of matches().
	 *
	 * This is important because we want to detect a pattern
	 * anywhere inside a full file path.
	 */
	private static List<String> matchingFiles(
			List<ChangeSummaryItem> changes,
			Pattern pattern
	) {

		return changes.stream()
				.filter(item -> item != null)
				.map(ChangeSummaryItem::fileName)
				.filter(name ->
						name != null &&
								!name.isBlank()
				)
				.filter(name ->
						pattern.matcher(name).find()
				)
				.toList();
	}

	/**
	 * Converts a list of files into a readable message.
	 *
	 * We don't want extremely large log messages if hundreds
	 * of files match.
	 */
	private static String summarizeFiles(
			List<String> files
	) {

		int maxFilesToShow = 5;

		if (files.size() <= maxFilesToShow) {
			return String.join(", ", files);
		}

		return String.join(
				", ",
				files.subList(0, maxFilesToShow)
		) + " ...";
	}

	/**
	 * Determines whether a change represents a deleted file.
	 */
	private static boolean isDeleted(
			String changeType
	) {

		if (changeType == null) {
			return false;
		}

		String normalized =
				changeType.trim()
						.toUpperCase(Locale.ROOT);

		return "DELETED".equals(normalized)
				|| "REMOVED".equals(normalized);
	}

	/**
	 * Creates a pattern for custom rules.
	 *
	 * Example:
	 *
	 * HIGH_PAYMENT_RISK
	 *
	 * becomes approximately:
	 *
	 * .*high.*payment.*risk.*
	 */
	private static Pattern customNamePattern(
			String ruleName
	) {

		String[] parts =
				ruleName.toLowerCase(Locale.ROOT)
						.split("_+");

		StringBuilder regex =
				new StringBuilder(".*");

		boolean firstPart = true;

		for (String part : parts) {

			if (part == null || part.isBlank()) {
				continue;
			}

			if (!firstPart) {
				regex.append(".*");
			}

			regex.append(
					Pattern.quote(part)
			);

			firstPart = false;
		}

		regex.append(".*");

		return Pattern.compile(
				regex.toString(),
				Pattern.CASE_INSENSITIVE
		);
	}

	/**
	 * Normalizes rule names coming from DB/configuration.
	 *
	 * Example:
	 *
	 * " high_file_count "
	 *
	 * becomes:
	 *
	 * "HIGH_FILE_COUNT"
	 */
	private static String normalizeRuleName(
			String ruleName
	) {

		return ruleName
				.trim()
				.toUpperCase(Locale.ROOT);
	}

	public record Evaluation(
			int score,
			RiskLevel riskLevel,
			List<RiskFactor> factors
	) {
	}
}