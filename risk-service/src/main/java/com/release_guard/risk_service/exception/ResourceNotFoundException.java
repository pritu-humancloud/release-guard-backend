package com.release_guard.risk_service.exception;

import java.util.UUID;

public class ResourceNotFoundException extends RuntimeException {

	public ResourceNotFoundException(String message) {
		super(message);
	}

	public static ResourceNotFoundException assessment(UUID releaseId) {
		return new ResourceNotFoundException("Risk assessment not found for release: " + releaseId);
	}

	public static ResourceNotFoundException rule(UUID id) {
		return new ResourceNotFoundException("Risk rule not found: " + id);
	}
}
