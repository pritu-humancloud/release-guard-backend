package com.release_guard.notification_service.controller;

import com.release_guard.notification_service.dto.request.CreateNotificationRequest;
import com.release_guard.notification_service.dto.response.NotificationResponse;
import com.release_guard.notification_service.entity.NotificationStatus;
import com.release_guard.notification_service.entity.NotificationType;
import com.release_guard.notification_service.service.NotificationPushService;
import com.release_guard.notification_service.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications")
public class NotificationController {

	private final NotificationService notificationService;
	private final NotificationPushService notificationPushService;

	@GetMapping
	@Operation(summary = "List notifications with optional filters for recipient, type, and status")
	public List<NotificationResponse> list(
			@RequestParam(required = false) String recipient,
			@RequestParam(required = false) NotificationType type,
			@RequestParam(required = false) NotificationStatus status
	) {
		return notificationService.list(recipient, type, status);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get notification details by ID")
	public NotificationResponse getById(@PathVariable UUID id) {
		return notificationService.getById(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create and dispatch a notification")
	public NotificationResponse create(@Valid @RequestBody CreateNotificationRequest request) {
		return notificationService.createAndSend(request);
	}

	@PutMapping("/{id}/status")
	@Operation(summary = "Update notification delivery status")
	public NotificationResponse updateStatus(
			@PathVariable UUID id,
			@RequestParam NotificationStatus status
	) {
		return notificationService.updateStatus(id, status);
	}

	@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	@Operation(summary = "Subscribe to real-time notifications stream (Server-Sent Events)")
	public SseEmitter subscribeStream() {
		return notificationPushService.subscribeSse();
	}
}
