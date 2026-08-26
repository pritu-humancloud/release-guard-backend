package com.release_guard.release_service.service;

import com.release_guard.release_service.dto.event.ReleaseCreatedEvent;
import com.release_guard.release_service.dto.event.ReleaseStatusChangedEvent;
import com.release_guard.release_service.dto.github.GitHubPullFile;
import com.release_guard.release_service.dto.github.GitHubPullResponse;
import com.release_guard.release_service.dto.request.CreateReleaseRequest;
import com.release_guard.release_service.dto.request.GitHubSyncRequest;
import com.release_guard.release_service.dto.request.UpdateReleaseStatusRequest;
import com.release_guard.release_service.dto.response.ReleaseChangeResponse;
import com.release_guard.release_service.dto.response.ReleaseDetailResponse;
import com.release_guard.release_service.dto.response.ReleaseResponse;
import com.release_guard.release_service.entity.ChangeType;
import com.release_guard.release_service.entity.PullRequestInfo;
import com.release_guard.release_service.entity.Release;
import com.release_guard.release_service.entity.ReleaseChange;
import com.release_guard.release_service.entity.ReleaseStatus;
import com.release_guard.release_service.exception.ResourceNotFoundException;
import com.release_guard.release_service.kafka.ReleaseEventPublisher;
import com.release_guard.release_service.repository.PullRequestInfoRepository;
import com.release_guard.release_service.repository.ReleaseChangeRepository;
import com.release_guard.release_service.repository.ReleaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReleaseService {

	private final ReleaseRepository releaseRepository;
	private final ReleaseChangeRepository releaseChangeRepository;
	private final PullRequestInfoRepository pullRequestInfoRepository;
	private final GitHubClient gitHubClient;
	private final ReleaseEventPublisher eventPublisher;

	@Transactional
	public ReleaseResponse create(CreateReleaseRequest request) {
		Release release = Release.builder()
				.projectName(request.projectName())
				.version(request.version())
				.description(request.description())
				.createdBy(request.createdBy())
				.status(ReleaseStatus.DRAFT)
				.build();

		Release saved = releaseRepository.save(release);
		eventPublisher.publishCreated(ReleaseCreatedEvent.from(saved));
		return ReleaseResponse.from(saved);
	}

	public List<ReleaseResponse> list(String projectName, ReleaseStatus status) {
		return releaseRepository.findAllFiltered(blankToNull(projectName), status).stream()
				.map(ReleaseResponse::from)
				.toList();
	}

	public ReleaseDetailResponse getById(UUID id) {
		return ReleaseDetailResponse.from(getRelease(id));
	}

	@Transactional
	public ReleaseResponse updateStatus(UUID id, UpdateReleaseStatusRequest request) {
		Release release = getRelease(id);
		ReleaseStatus previous = release.getStatus();
		ReleaseStatus next = request.status();

		if (previous == next) {
			return ReleaseResponse.from(release);
		}

		release.setStatus(next);
		if (next == ReleaseStatus.DEPLOYED && release.getDeployedAt() == null) {
			release.setDeployedAt(Instant.now());
		}

		Release saved = releaseRepository.save(release);
		eventPublisher.publishStatusChanged(new ReleaseStatusChangedEvent(
				"release.status.changed",
				saved.getId(),
				saved.getProjectName(),
				saved.getVersion(),
				previous,
				next,
				Instant.now()
		));
		return ReleaseResponse.from(saved);
	}

	public List<ReleaseChangeResponse> listChanges(UUID id) {
		if (!releaseRepository.existsById(id)) {
			throw ResourceNotFoundException.release(id);
		}
		return releaseChangeRepository.findByReleaseId(id).stream()
				.map(ReleaseChangeResponse::from)
				.toList();
	}

	@Transactional
	public ReleaseDetailResponse syncFromGitHub(UUID id, GitHubSyncRequest request) {
		Release release = getRelease(id);
		RepoRef repo = parseRepo(request.repoName());

		GitHubPullResponse pull = gitHubClient.getPullRequest(repo.owner(), repo.name(), request.prNumber());
		List<GitHubPullFile> files = gitHubClient.getPullRequestFiles(repo.owner(), repo.name(), request.prNumber());

		PullRequestInfo prInfo = pullRequestInfoRepository
				.findByReleaseIdAndRepoNameAndPrNumber(id, request.repoName(), request.prNumber())
				.orElseGet(() -> {
					PullRequestInfo created = PullRequestInfo.builder()
							.release(release)
							.repoName(request.repoName())
							.prNumber(request.prNumber())
							.build();
					release.getPullRequests().add(created);
					return created;
				});
		prInfo.setTitle(pull.title());
		prInfo.setAuthor(pull.user() != null ? pull.user().login() : null);
		prInfo.setMergedAt(pull.mergedAt());
		pullRequestInfoRepository.save(prInfo);

		release.getChanges().clear();

		for (GitHubPullFile file : files) {
			ReleaseChange change = ReleaseChange.builder()
					.release(release)
					.fileName(file.filename())
					.changeType(mapChangeType(file.status()))
					.linesAdded(file.additions())
					.linesRemoved(file.deletions())
					.build();
			release.getChanges().add(change);
		}

		Release saved = releaseRepository.save(release);
		return ReleaseDetailResponse.from(saved);
	}

	private Release getRelease(UUID id) {
		return releaseRepository.findById(id)
				.orElseThrow(() -> ResourceNotFoundException.release(id));
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value;
	}

	private static RepoRef parseRepo(String repoName) {
		String[] parts = repoName.split("/", 2);
		if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
			throw new IllegalArgumentException("repoName must be in owner/repo format");
		}
		return new RepoRef(parts[0], parts[1]);
	}

	private static ChangeType mapChangeType(String githubStatus) {
		if (githubStatus == null) {
			return ChangeType.MODIFIED;
		}
		return switch (githubStatus.toLowerCase()) {
			case "added" -> ChangeType.ADDED;
			case "removed" -> ChangeType.DELETED;
			default -> ChangeType.MODIFIED;
		};
	}

	private record RepoRef(String owner, String name) {
	}
}
