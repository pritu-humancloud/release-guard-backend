package com.release_guard.release_service.entity;

public enum ReleaseStatus {
	DRAFT,
	PENDING_REVIEW,
	APPROVED,
	BLOCKED,
	DEPLOYED,
	ROLLED_BACK
}
