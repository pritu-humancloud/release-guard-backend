package com.release_guard.risk_service.service;

import com.release_guard.risk_service.exception.ForbiddenException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AdminAccess {

	private final String adminKey;

	public AdminAccess(@Value("${app.admin.key}") String adminKey) {
		this.adminKey = adminKey;
	}

	public void require(String providedKey) {
		if (adminKey == null || adminKey.isBlank() || !adminKey.equals(providedKey)) {
			throw new ForbiddenException("Admin key required");
		}
	}
}
