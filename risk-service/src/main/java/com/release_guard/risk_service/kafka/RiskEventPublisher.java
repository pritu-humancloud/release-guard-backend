package com.release_guard.risk_service.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.release_guard.risk_service.dto.event.RiskCalculatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RiskEventPublisher {

	private final KafkaTemplate<String, String> kafkaTemplate;
	private final ObjectMapper objectMapper;

	@Value("${app.kafka.topics.risk-calculated}")
	private String riskCalculatedTopic;

	public void publishCalculated(RiskCalculatedEvent event) {
		send(riskCalculatedTopic, event.releaseId().toString(), event);
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
