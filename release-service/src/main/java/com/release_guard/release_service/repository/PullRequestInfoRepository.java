package com.release_guard.release_service.repository;

import com.release_guard.release_service.entity.PullRequestInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PullRequestInfoRepository extends JpaRepository<PullRequestInfo, UUID> {

	List<PullRequestInfo> findByReleaseId(UUID releaseId);

	Optional<PullRequestInfo> findByReleaseIdAndRepoNameAndPrNumber(UUID releaseId, String repoName, int prNumber);
}
