package com.release_guard.release_service.repository;

import com.release_guard.release_service.entity.Release;
import com.release_guard.release_service.entity.ReleaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ReleaseRepository extends JpaRepository<Release, UUID> {

	@Query("""
			SELECT r FROM Release r
			WHERE (:projectName IS NULL OR r.projectName = :projectName)
			  AND (:status IS NULL OR r.status = :status)
			ORDER BY r.createdAt DESC
			""")
	List<Release> findAllFiltered(@Param("projectName") String projectName,
			@Param("status") ReleaseStatus status);
}
