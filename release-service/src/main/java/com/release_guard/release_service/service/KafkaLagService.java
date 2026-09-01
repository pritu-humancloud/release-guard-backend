package com.release_guard.release_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.release_guard.release_service.dto.request.ConsumerLagTestRequest;
import com.release_guard.release_service.dto.response.ConsumerLagResponse;
import com.release_guard.release_service.dto.response.PartitionLagInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaLagService {

	private final KafkaTemplate<String, String> kafkaTemplate;
	private final ObjectMapper objectMapper;

	@Value("${spring.kafka.bootstrap-servers:localhost:9092}")
	private String bootstrapServers;

	@Value("${app.kafka.topics.risk-calculated:risk.calculated}")
	private String riskCalculatedTopic;

	@Value("${app.kafka.topics.release-status-changed:release.status.changed}")
	private String releaseStatusChangedTopic;

	@Value("${app.kafka.topics.incident-created:health.incident.created}")
	private String incidentCreatedTopic;

	@Value("${app.kafka.topics.release-created:release.created}")
	private String releaseCreatedTopic;

	private static final String DEFAULT_CONSUMER_GROUP = "notification-service";

	public ConsumerLagResponse simulateAndMeasureLag(ConsumerLagTestRequest request) {
		int count = request.countOrDefault();
		String eventType = request.eventTypeOrDefault();
		long delayMs = request.delayMsOrDefault();
		String projectName = request.projectNameOrDefault();

		List<String> targetTopics = determineTopics(eventType);
		List<Object> sampleEvents = new ArrayList<>();

		log.info("Starting consumer lag test: publishing {} event(s) of type '{}' to topics {}", count, eventType, targetTopics);

		for (int i = 1; i <= count; i++) {
			ObjectNode eventPayload = generateEventPayload(eventType, i, projectName);
			String topic = eventPayload.get("targetTopic").asText();
			eventPayload.remove("targetTopic");

			String key = eventPayload.has("releaseId")
					? eventPayload.get("releaseId").asText()
					: UUID.randomUUID().toString();

			try {
				String jsonString = objectMapper.writeValueAsString(eventPayload);
				kafkaTemplate.send(topic, key, jsonString);

				if (sampleEvents.size() < 5) {
					sampleEvents.add(eventPayload);
				}

				if (delayMs > 0 && i < count) {
					Thread.sleep(delayMs);
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				log.warn("Interrupted while simulating lag test delay: {}", e.getMessage());
				break;
			} catch (Exception e) {
				log.error("Failed to publish test event #{} to Kafka topic {}: {}", i, topic, e.getMessage());
			}
		}

		// Allow async sends to complete and broker to register offsets
		try {
			Thread.sleep(100);
		} catch (InterruptedException ignored) {
			Thread.currentThread().interrupt();
		}

		List<PartitionLagInfo> partitionDetails = getConsumerLag(DEFAULT_CONSUMER_GROUP, targetTopics);
		long totalLag = partitionDetails.stream().mapToLong(PartitionLagInfo::lag).sum();

		Map<String, String> verificationEndpoints = new LinkedHashMap<>();
		verificationEndpoints.put("1_ListNotifications", "GET http://localhost:8080/api/notifications");
		verificationEndpoints.put("2_RealtimeSseStream", "GET http://localhost:8080/api/notifications/stream");
		verificationEndpoints.put("3_WebSocketStomp", "ws://localhost:8080/ws/notifications (Topic: /topic/notifications)");
		verificationEndpoints.put("4_CurrentLagStatus", "GET http://localhost:8080/api/releases/test/consumer-lag");

		String summaryMessage = String.format(
				"Successfully published %d event(s) to Kafka. Consumer '%s' is automatically consuming events in the background without any API invocation.",
				count, DEFAULT_CONSUMER_GROUP
		);

		return new ConsumerLagResponse(
				"SUCCESS",
				summaryMessage,
				count,
				eventType,
				targetTopics,
				DEFAULT_CONSUMER_GROUP,
				totalLag,
				partitionDetails,
				verificationEndpoints,
				sampleEvents
		);
	}

	public ConsumerLagResponse getCurrentLagStatus() {
		List<String> monitoredTopics = List.of(
				riskCalculatedTopic,
				releaseStatusChangedTopic,
				incidentCreatedTopic,
				releaseCreatedTopic
		);

		List<PartitionLagInfo> partitionDetails = getConsumerLag(DEFAULT_CONSUMER_GROUP, monitoredTopics);
		long totalLag = partitionDetails.stream().mapToLong(PartitionLagInfo::lag).sum();

		Map<String, String> verificationEndpoints = new LinkedHashMap<>();
		verificationEndpoints.put("1_ListNotifications", "GET http://localhost:8080/api/notifications");
		verificationEndpoints.put("2_RealtimeSseStream", "GET http://localhost:8080/api/notifications/stream");
		verificationEndpoints.put("3_TriggerLagTest", "POST http://localhost:8080/api/releases/test/consumer-lag?count=10&eventType=ALL");

		return new ConsumerLagResponse(
				"SUCCESS",
				"Current Kafka consumer lag status for consumer group: " + DEFAULT_CONSUMER_GROUP,
				0,
				"METRICS_QUERY",
				monitoredTopics,
				DEFAULT_CONSUMER_GROUP,
				totalLag,
				partitionDetails,
				verificationEndpoints,
				Collections.emptyList()
		);
	}

	public List<PartitionLagInfo> getConsumerLag(String consumerGroup, List<String> topics) {
		List<PartitionLagInfo> results = new ArrayList<>();
		Properties props = new Properties();
		props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		props.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "3000");
		props.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, "3000");

		try (AdminClient adminClient = AdminClient.create(props)) {
			// 1. Get committed offsets for consumer group
			Map<TopicPartition, OffsetAndMetadata> groupOffsets = Collections.emptyMap();
			try {
				groupOffsets = adminClient.listConsumerGroupOffsets(consumerGroup)
						.partitionsToOffsetAndMetadata()
						.get(3, TimeUnit.SECONDS);
			} catch (Exception e) {
				log.debug("Consumer group '{}' offsets query info: {}", consumerGroup, e.getMessage());
			}

			// 2. Query topic metadata
			Map<String, TopicDescription> topicDescriptions = Collections.emptyMap();
			try {
				topicDescriptions = adminClient.describeTopics(topics)
						.allTopicNames()
						.get(3, TimeUnit.SECONDS);
			} catch (Exception e) {
				log.debug("Topic description info for {}: {}", topics, e.getMessage());
			}

			// 3. Assemble partitions
			List<TopicPartition> partitions = new ArrayList<>();
			for (Map.Entry<String, TopicDescription> entry : topicDescriptions.entrySet()) {
				String topic = entry.getKey();
				for (TopicPartitionInfo pInfo : entry.getValue().partitions()) {
					partitions.add(new TopicPartition(topic, pInfo.partition()));
				}
			}

			if (partitions.isEmpty() && !groupOffsets.isEmpty()) {
				partitions.addAll(groupOffsets.keySet());
			}

			if (partitions.isEmpty()) {
				for (String topic : topics) {
					partitions.add(new TopicPartition(topic, 0));
				}
			}

			// 4. Fetch latest log end offsets
			Map<TopicPartition, OffsetSpec> offsetSpecMap = partitions.stream()
					.collect(Collectors.toMap(tp -> tp, tp -> OffsetSpec.latest()));

			Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> endOffsetsMap = Collections.emptyMap();
			try {
				endOffsetsMap = adminClient.listOffsets(offsetSpecMap)
						.all()
						.get(3, TimeUnit.SECONDS);
			} catch (Exception e) {
				log.debug("ListOffsets info: {}", e.getMessage());
			}

			// 5. Build results
			for (TopicPartition tp : partitions) {
				long logEndOffset = endOffsetsMap.containsKey(tp) ? endOffsetsMap.get(tp).offset() : 0L;
				OffsetAndMetadata metadata = groupOffsets.get(tp);
				long currentOffset = metadata != null ? metadata.offset() : 0L;
				long lag = Math.max(0, logEndOffset - currentOffset);

				results.add(new PartitionLagInfo(
						tp.topic(),
						tp.partition(),
						logEndOffset,
						currentOffset,
						lag
				));
			}
		} catch (Exception e) {
			log.warn("Could not query Kafka AdminClient (Kafka may be offline or initializing): {}", e.getMessage());
		}

		return results;
	}

	private List<String> determineTopics(String eventType) {
		return switch (eventType.toUpperCase()) {
			case "RISK_HIGH" -> List.of(riskCalculatedTopic);
			case "RELEASE_BLOCKED" -> List.of(releaseStatusChangedTopic);
			case "INCIDENT" -> List.of(incidentCreatedTopic);
			case "RELEASE_CREATED" -> List.of(releaseCreatedTopic);
			default -> List.of(riskCalculatedTopic, releaseStatusChangedTopic, incidentCreatedTopic, releaseCreatedTopic);
		};
	}

	private ObjectNode generateEventPayload(String eventType, int index, String projectName) {
		String selectedType = eventType.toUpperCase();
		if ("ALL".equals(selectedType)) {
			String[] types = {"RISK_HIGH", "RELEASE_BLOCKED", "INCIDENT", "RELEASE_CREATED"};
			selectedType = types[(index - 1) % types.length];
		}

		ObjectNode root = objectMapper.createObjectNode();
		String eventId = UUID.randomUUID().toString();
		String releaseId = UUID.randomUUID().toString();
		String now = Instant.now().toString();

		root.put("eventId", eventId);
		root.put("timestamp", now);

		switch (selectedType) {
			case "RISK_HIGH" -> {
				root.put("targetTopic", riskCalculatedTopic);
				root.put("eventType", "risk.calculated");
				root.put("releaseId", releaseId);

				ObjectNode data = root.putObject("data");
				data.put("releaseId", releaseId);
				data.put("projectName", projectName);
				data.put("score", 75 + (index % 25)); // score >= 70 triggers RISK_HIGH notification
				data.put("riskLevel", "HIGH");
				data.put("assessmentId", UUID.randomUUID().toString());
				data.put("message", "Simulated automated high risk trigger #" + index);
			}
			case "RELEASE_BLOCKED" -> {
				root.put("targetTopic", releaseStatusChangedTopic);
				root.put("eventType", "release.status.changed");
				root.put("releaseId", releaseId);
				root.put("projectName", projectName);
				root.put("version", "v" + index + ".0.0");
				root.put("previousStatus", "DRAFT");
				root.put("newStatus", "BLOCKED");

				ObjectNode data = root.putObject("data");
				data.put("releaseId", releaseId);
				data.put("projectName", projectName);
				data.put("version", "v" + index + ".0.0");
				data.put("newStatus", "BLOCKED");
			}
			case "INCIDENT" -> {
				root.put("targetTopic", incidentCreatedTopic);
				root.put("eventType", "health.incident.created");

				ObjectNode data = root.putObject("data");
				data.put("incidentId", "INC-" + UUID.randomUUID().toString().substring(0, 8));
				data.put("serviceName", projectName);
				data.put("message", "Simulated production outage / anomaly detected (test #" + index + ")");
			}
			default -> {
				root.put("targetTopic", releaseCreatedTopic);
				root.put("eventType", "release.created");
				root.put("releaseId", releaseId);
				root.put("projectName", projectName);
				root.put("version", "v" + index + ".0.0");
				root.put("createdBy", "test-user");
			}
		}

		return root;
	}
}
