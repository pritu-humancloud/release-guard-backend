package com.release_guard.release_service.exception;

import java.util.UUID;

public class ResourceNotFoundException extends RuntimeException {

	public ResourceNotFoundException(String message) {
		super(message);
	}

	public static ResourceNotFoundException release(UUID id) {
		return new ResourceNotFoundException("Release not found: " + id);
	}
}
