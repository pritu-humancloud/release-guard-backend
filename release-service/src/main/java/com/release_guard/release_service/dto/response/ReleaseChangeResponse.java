package com.release_guard.release_service.dto.response;

import com.release_guard.release_service.entity.ChangeType;
import com.release_guard.release_service.entity.ReleaseChange;

import java.util.UUID;

public record ReleaseChangeResponse(
		UUID id,
		String fileName,
		ChangeType changeType,
		int linesAdded,
		int linesRemoved
) {
	public static ReleaseChangeResponse from(ReleaseChange change) {
		return new ReleaseChangeResponse(
				change.getId(),
				change.getFileName(),
				change.getChangeType(),
				change.getLinesAdded(),
				change.getLinesRemoved()
		);
	}
}
