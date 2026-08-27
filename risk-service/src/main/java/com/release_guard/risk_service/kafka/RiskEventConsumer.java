package com.release_guard.risk_service.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RiskEventConsumer {

	private final ObjectMapper objectMapper;

	@KafkaListener(
			topics = "${app.kafka.topics.release-created:release.created}",
			groupId = "${spring.kafka.consumer.group-id:risk-service}",
			autoStartup = "${app.kafka.consumer.auto-startup:true}"
	)
	public void consumeReleaseCreated(String message) {
		try {
			JsonNode root = objectMapper.readTree(message);
			String releaseId = root.has("releaseId") ? root.get("releaseId").asText() : "unknown";
			String projectName = root.has("projectName") ? root.get("projectName").asText() : "";
			log.info("Received release.created event for releaseId: {}, project: {}", releaseId, projectName);
		} catch (Exception e) {
			log.warn("Failed to parse incoming release.created event: {}", e.getMessage());
		}
	}
}
