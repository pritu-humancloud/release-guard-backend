package com.release_guard.notification_service.dto.response;

import com.release_guard.notification_service.entity.Notification;
import com.release_guard.notification_service.entity.NotificationStatus;
import com.release_guard.notification_service.entity.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
		UUID id,
		NotificationType type,
		String recipient,
		String message,
		Instant sentAt,
		NotificationStatus status
) {
	public static NotificationResponse from(Notification notification) {
		return new NotificationResponse(
				notification.getId(),
				notification.getType(),
				notification.getRecipient(),
				notification.getMessage(),
				notification.getSentAt(),
				notification.getStatus()
		);
	}
}
