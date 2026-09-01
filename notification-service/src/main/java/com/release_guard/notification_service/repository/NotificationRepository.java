package com.release_guard.notification_service.repository;

import com.release_guard.notification_service.entity.Notification;
import com.release_guard.notification_service.entity.NotificationStatus;
import com.release_guard.notification_service.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

	List<Notification> findAllByOrderBySentAtDesc();

	List<Notification> findByRecipientOrderBySentAtDesc(String recipient);

	@Query("""
			SELECT n FROM Notification n
			WHERE (:recipient IS NULL OR LOWER(n.recipient) = LOWER(:recipient))
			  AND (:type IS NULL OR n.type = :type)
			  AND (:status IS NULL OR n.status = :status)
			ORDER BY n.sentAt DESC
			""")
	List<Notification> findAllFiltered(
			@Param("recipient") String recipient,
			@Param("type") NotificationType type,
			@Param("status") NotificationStatus status
	);
}
