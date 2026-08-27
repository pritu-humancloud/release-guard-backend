package com.release_guard.notification_service.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.release_guard.notification_service.dto.request.CreateNotificationRequest;
import com.release_guard.notification_service.entity.NotificationStatus;
import com.release_guard.notification_service.entity.NotificationType;
import com.release_guard.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationKafkaConsumer {

	private final ObjectMapper objectMapper;
	private final NotificationService notificationService;

	@KafkaListener(
			topics = "${app.kafka.topics.risk-calculated:risk.calculated}",
			groupId = "${spring.kafka.consumer.group-id:notification-service}",
			autoStartup = "${app.kafka.consumer.auto-startup:true}"
	)
	public void consumeRiskCalculated(String message) {
		try {
			log.info("Received risk.calculated Kafka message: {}", message);
			JsonNode root = objectMapper.readTree(message);

			JsonNode data = root.has("data") ? root.get("data") : root;
			int score = data.has("score") ? data.get("score").asInt() : (root.has("score") ? root.get("score").asInt() : 0);
			String releaseId = data.has("releaseId") ? data.get("releaseId").asText() : (root.has("releaseId") ? root.get("releaseId").asText() : "unknown");
			String riskLevel = data.has("riskLevel") ? data.get("riskLevel").asText() : (root.has("riskLevel") ? root.get("riskLevel").asText() : "UNKNOWN");

			if (score >= 70) {
				String alertMsg = String.format("High risk alert for release %s: score=%d (%s). Review required before deployment.",
						releaseId, score, riskLevel);

				notificationService.createAndSend(new CreateNotificationRequest(
						NotificationType.RISK_HIGH,
						"release-team",
						alertMsg,
						NotificationStatus.SENT
				));
				log.warn("Created and dispatched RISK_HIGH notification for release {}", releaseId);
			} else {
				log.info("Risk score {} for release {} is below threshold 70; no RISK_HIGH notification needed", score, releaseId);
			}
		} catch (Exception e) {
			log.error("Failed to process risk.calculated event: {}", e.getMessage(), e);
		}
	}

	@KafkaListener(
			topics = "${app.kafka.topics.release-status-changed:release.status.changed}",
			groupId = "${spring.kafka.consumer.group-id:notification-service}",
			autoStartup = "${app.kafka.consumer.auto-startup:true}"
	)
	public void consumeReleaseStatusChanged(String message) {
		try {
			log.info("Received release.status.changed Kafka message: {}", message);
			JsonNode root = objectMapper.readTree(message);

			JsonNode data = root.has("data") ? root.get("data") : root;
			String releaseId = data.has("releaseId") ? data.get("releaseId").asText() : (root.has("releaseId") ? root.get("releaseId").asText() : "unknown");
			String projectName = data.has("projectName") ? data.get("projectName").asText() : (root.has("projectName") ? root.get("projectName").asText() : "Unknown Project");
			String version = data.has("version") ? data.get("version").asText() : (root.has("version") ? root.get("version").asText() : "");
			String newStatus = data.has("newStatus") ? data.get("newStatus").asText() : (root.has("newStatus") ? root.get("newStatus").asText() : "");

			if ("BLOCKED".equalsIgnoreCase(newStatus)) {
				String alertMsg = String.format("Release %s (%s %s) has been BLOCKED due to risk/policy violations.",
						releaseId, projectName, version);

				notificationService.createAndSend(new CreateNotificationRequest(
						NotificationType.RELEASE_BLOCKED,
						"release-team",
						alertMsg,
						NotificationStatus.SENT
				));
				log.warn("Created and dispatched RELEASE_BLOCKED notification for release {}", releaseId);
			}
		} catch (Exception e) {
			log.error("Failed to process release.status.changed event: {}", e.getMessage(), e);
		}
	}

	@KafkaListener(
			topics = "${app.kafka.topics.incident-created:health.incident.created}",
			groupId = "${spring.kafka.consumer.group-id:notification-service}",
			autoStartup = "${app.kafka.consumer.auto-startup:true}"
	)
	public void consumeIncidentCreated(String message) {
		try {
			log.info("Received health.incident.created Kafka message: {}", message);
			JsonNode root = objectMapper.readTree(message);

			JsonNode data = root.has("data") ? root.get("data") : root;
			String incidentId = data.has("incidentId") ? data.get("incidentId").asText() : (root.has("incidentId") ? root.get("incidentId").asText() : "unknown");
			String serviceName = data.has("serviceName") ? data.get("serviceName").asText() : (root.has("serviceName") ? root.get("serviceName").asText() : "Production Service");
			String details = data.has("message") ? data.get("message").asText() : (root.has("message") ? root.get("message").asText() : "Incident occurred");

			String alertMsg = String.format("Production INCIDENT detected on %s (ID: %s): %s", serviceName, incidentId, details);

			notificationService.createAndSend(new CreateNotificationRequest(
					NotificationType.INCIDENT,
					"oncall-team",
					alertMsg,
					NotificationStatus.SENT
			));
			log.warn("Created and dispatched INCIDENT notification for incident {}", incidentId);
		} catch (Exception e) {
			log.error("Failed to process health.incident.created event: {}", e.getMessage(), e);
		}
	}
}
