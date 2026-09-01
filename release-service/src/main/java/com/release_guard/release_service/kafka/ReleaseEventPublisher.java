package com.release_guard.release_service.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.release_guard.release_service.dto.event.ReleaseCreatedEvent;
import com.release_guard.release_service.dto.event.ReleaseStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReleaseEventPublisher {

	private final KafkaTemplate<String, String> kafkaTemplate;
	private final ObjectMapper objectMapper;

	@Value("${app.kafka.topics.release-created}")
	private String releaseCreatedTopic;

	@Value("${app.kafka.topics.release-status-changed}")
	private String releaseStatusChangedTopic;

	public void publishCreated(ReleaseCreatedEvent event) {
		send(releaseCreatedTopic, event.releaseId().toString(), event);
	}

	public void publishStatusChanged(ReleaseStatusChangedEvent event) {
		send(releaseStatusChangedTopic, event.releaseId().toString(), event);
	}

	private void send(String topic, String key, Object payload) {
		try {
			String json = objectMapper.writeValueAsString(payload);
			kafkaTemplate.send(topic, key, json).whenComplete((result, ex) -> {
				if (ex != null) {
					log.warn("Failed to publish Kafka event to {}: {}", topic, ex.getMessage());
				} else {
					log.info("Published Kafka event to {}", topic);
				}
			});
		} catch (JsonProcessingException e) {
			log.warn("Failed to serialize Kafka event for {}: {}", topic, e.getMessage());
		}
	}
}
