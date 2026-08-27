package com.release_guard.notification_service.exception;

import java.util.UUID;

public class ResourceNotFoundException extends RuntimeException {

	public ResourceNotFoundException(String message) {
		super(message);
	}

	public static ResourceNotFoundException notification(UUID id) {
		return new ResourceNotFoundException("Notification not found: " + id);
	}
}
