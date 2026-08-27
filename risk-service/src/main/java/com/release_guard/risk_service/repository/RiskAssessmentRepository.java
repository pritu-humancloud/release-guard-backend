package com.release_guard.risk_service.repository;

import com.release_guard.risk_service.entity.RiskAssessment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RiskAssessmentRepository extends JpaRepository<RiskAssessment, UUID> {

	@EntityGraph(attributePaths = "factors")
	Optional<RiskAssessment> findFirstByReleaseIdOrderByCalculatedAtDesc(UUID releaseId);
}
