package com.release_guard.notification_service.dto.request;

import com.release_guard.notification_service.entity.NotificationStatus;
import com.release_guard.notification_service.entity.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateNotificationRequest(
		@NotNull NotificationType type,
		@NotBlank String recipient,
		@NotBlank String message,
		NotificationStatus status
) {
}
