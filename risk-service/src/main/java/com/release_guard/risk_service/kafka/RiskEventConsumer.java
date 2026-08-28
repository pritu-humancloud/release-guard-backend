package com.release_guard.risk_service.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.release_guard.risk_service.dto.request.CalculateRiskRequest;
import com.release_guard.risk_service.dto.request.ChangeSummaryItem;
import com.release_guard.risk_service.dto.response.RiskAssessmentResponse;
import com.release_guard.risk_service.service.RiskAssessmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RiskEventConsumer {

	private final ObjectMapper objectMapper;
	private final RiskAssessmentService riskAssessmentService;

	@KafkaListener(
			topics = "${app.kafka.topics.release-created:release.created}",
			groupId = "${spring.kafka.consumer.group-id:risk-service}",
			autoStartup = "${app.kafka.consumer.auto-startup:true}"
	)
	public void consumeReleaseCreated(String message) {
		try {
			log.info("Received release.created Kafka message: {}", message);
			JsonNode root = objectMapper.readTree(message);
			JsonNode data = root.has("data") ? root.get("data") : root;

			String releaseIdStr = data.has("releaseId")
					? data.get("releaseId").asText()
					: (root.has("releaseId") ? root.get("releaseId").asText() : null);

			if (releaseIdStr == null || releaseIdStr.isBlank() || "unknown".equalsIgnoreCase(releaseIdStr)) {
				log.warn("releaseId missing or invalid in release.created event: {}", message);
				return;
			}

			UUID releaseId;
			try {
				releaseId = UUID.fromString(releaseIdStr);
			} catch (IllegalArgumentException ex) {
				log.warn("Invalid UUID format for releaseId '{}' in release.created event: {}", releaseIdStr, ex.getMessage());
				return;
			}

			String projectName = data.has("projectName")
					? data.get("projectName").asText()
					: (root.has("projectName") ? root.get("projectName").asText() : "");

			String version = data.has("version")
					? data.get("version").asText()
					: (root.has("version") ? root.get("version").asText() : "");

			List<ChangeSummaryItem> changes = new ArrayList<>();
			JsonNode changesNode = data.has("changes") ? data.get("changes") : root.get("changes");
			if (changesNode != null && changesNode.isArray()) {
				for (JsonNode item : changesNode) {
					String fileName = item.has("fileName") ? item.get("fileName").asText() : "";
					String changeType = item.has("changeType") ? item.get("changeType").asText() : "MODIFIED";
					int linesAdded = item.has("linesAdded") ? item.get("linesAdded").asInt() : 0;
					int linesRemoved = item.has("linesRemoved") ? item.get("linesRemoved").asInt() : 0;
					if (!fileName.isBlank()) {
						changes.add(new ChangeSummaryItem(fileName, changeType, linesAdded, linesRemoved));
					}
				}
			}

			CalculateRiskRequest request = new CalculateRiskRequest(releaseId, projectName, version, changes);
			RiskAssessmentResponse response = riskAssessmentService.assess(request);

			log.info("Successfully assessed and published risk for release {}: score={}, level={}",
					releaseId, response.score(), response.riskLevel());
		} catch (Exception e) {
			log.error("Failed to process release.created event: {}", e.getMessage(), e);
		}
	}
}
