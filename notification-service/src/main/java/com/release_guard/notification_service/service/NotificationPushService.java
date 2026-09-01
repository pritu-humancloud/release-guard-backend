package com.release_guard.notification_service.service;

import com.release_guard.notification_service.dto.response.NotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationPushService {

	private final SimpMessagingTemplate messagingTemplate;
	private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

	public void push(NotificationResponse notification) {
		// 1. STOMP over WebSocket broadcast to all subscribers
		try {
			messagingTemplate.convertAndSend("/topic/notifications", notification);
			if (notification.recipient() != null && !notification.recipient().isBlank()) {
				messagingTemplate.convertAndSendToUser(notification.recipient(), "/queue/notifications", notification);
			}
			log.info("Pushed notification {} via WebSocket to /topic/notifications", notification.id());
		} catch (Exception e) {
			log.warn("Failed to push notification via WebSocket: {}", e.getMessage());
		}

		// 2. Real-time push to all SSE subscribers
		List<SseEmitter> deadEmitters = new java.util.ArrayList<>();
		for (SseEmitter emitter : emitters) {
			try {
				emitter.send(SseEmitter.event()
						.name("notification")
						.data(notification));
			} catch (IOException | IllegalStateException e) {
				deadEmitters.add(emitter);
			}
		}
		emitters.removeAll(deadEmitters);
	}

	public SseEmitter subscribeSse() {
		SseEmitter emitter = new SseEmitter(180_000L); // 3 minute timeout
		emitters.add(emitter);

		emitter.onCompletion(() -> emitters.remove(emitter));
		emitter.onTimeout(() -> emitters.remove(emitter));
		emitter.onError((ex) -> emitters.remove(emitter));

		try {
			emitter.send(SseEmitter.event().name("connected").data("Notification stream connected"));
		} catch (IOException e) {
			emitters.remove(emitter);
		}

		return emitter;
	}
}
