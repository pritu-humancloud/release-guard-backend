package com.release_guard.risk_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.release_guard.risk_service.dto.response.RiskAssessmentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class RiskCacheService {

	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;
	private final String keyPrefix;
	private final Duration ttl;

	public RiskCacheService(
			StringRedisTemplate redisTemplate,
			ObjectMapper objectMapper,
			@Value("${app.risk.cache.key-prefix}") String keyPrefix,
			@Value("${app.risk.cache.ttl-seconds}") long ttlSeconds
	) {
		this.redisTemplate = redisTemplate;
		this.objectMapper = objectMapper;
		this.keyPrefix = keyPrefix;
		this.ttl = Duration.ofSeconds(ttlSeconds);
	}

	public Optional<RiskAssessmentResponse> get(UUID releaseId) {
		try {
			String json = redisTemplate.opsForValue().get(key(releaseId));
			if (json == null || json.isBlank()) {
				return Optional.empty();
			}
			return Optional.of(objectMapper.readValue(json, RiskAssessmentResponse.class));
		} catch (Exception e) {
			log.warn("Redis read failed for release {}: {}", releaseId, e.getMessage());
			return Optional.empty();
		}
	}

	public void put(RiskAssessmentResponse assessment) {
		try {
			String json = objectMapper.writeValueAsString(assessment);
			redisTemplate.opsForValue().set(key(assessment.releaseId()), json, ttl);
		} catch (JsonProcessingException e) {
			log.warn("Failed to serialize risk assessment for cache: {}", e.getMessage());
		} catch (Exception e) {
			log.warn("Redis write failed for release {}: {}", assessment.releaseId(), e.getMessage());
		}
	}

	private String key(UUID releaseId) {
		return keyPrefix + releaseId;
	}
}
