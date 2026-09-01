package com.release_guard.release_service.repository;

import com.release_guard.release_service.entity.ReleaseChange;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReleaseChangeRepository extends JpaRepository<ReleaseChange, UUID> {

	List<ReleaseChange> findByReleaseId(UUID releaseId);

	void deleteByReleaseId(UUID releaseId);
}
