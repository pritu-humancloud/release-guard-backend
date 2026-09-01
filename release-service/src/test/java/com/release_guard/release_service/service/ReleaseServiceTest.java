package com.release_guard.release_service.service;

import com.release_guard.release_service.dto.github.GitHubPullFile;
import com.release_guard.release_service.dto.github.GitHubPullResponse;
import com.release_guard.release_service.dto.request.CreateReleaseRequest;
import com.release_guard.release_service.dto.request.GitHubSyncRequest;
import com.release_guard.release_service.dto.request.UpdateReleaseStatusRequest;
import com.release_guard.release_service.dto.response.ReleaseDetailResponse;
import com.release_guard.release_service.dto.response.ReleaseResponse;
import com.release_guard.release_service.entity.ChangeType;
import com.release_guard.release_service.entity.Release;
import com.release_guard.release_service.entity.ReleaseStatus;
import com.release_guard.release_service.kafka.ReleaseEventPublisher;
import com.release_guard.release_service.repository.PullRequestInfoRepository;
import com.release_guard.release_service.repository.ReleaseChangeRepository;
import com.release_guard.release_service.repository.ReleaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReleaseServiceTest {

	@Mock
	private ReleaseRepository releaseRepository;
	@Mock
	private ReleaseChangeRepository releaseChangeRepository;
	@Mock
	private PullRequestInfoRepository pullRequestInfoRepository;
	@Mock
	private GitHubClient gitHubClient;
	@Mock
	private ReleaseEventPublisher eventPublisher;

	private ReleaseService releaseService;

	@BeforeEach
	void setUp() {
		releaseService = new ReleaseService(
				releaseRepository,
				releaseChangeRepository,
				pullRequestInfoRepository,
				gitHubClient,
				eventPublisher
		);
	}

	@Test
	void create_createsReleaseAndPublishesEvent() {
		when(releaseRepository.save(any(Release.class))).thenAnswer(inv -> {
			Release r = inv.getArgument(0);
			r.setId(UUID.randomUUID());
			r.setCreatedAt(Instant.now());
			return r;
		});

		CreateReleaseRequest req = new CreateReleaseRequest(
				"payments-service",
				"v2.1.0",
				"New payment flow",
				"alice"
		);

		ReleaseResponse res = releaseService.create(req);

		assertThat(res.id()).isNotNull();
		assertThat(res.projectName()).isEqualTo("payments-service");
		assertThat(res.version()).isEqualTo("v2.1.0");
		assertThat(res.status()).isEqualTo(ReleaseStatus.DRAFT);

		verify(eventPublisher).publishCreated(any());
	}

	@Test
	void syncFromGitHub_attachesPRAndChanges() {
		UUID releaseId = UUID.randomUUID();
		Release release = Release.builder()
				.id(releaseId)
				.projectName("payments-service")
				.version("v2.1.0")
				.status(ReleaseStatus.DRAFT)
				.createdBy("alice")
				.changes(new ArrayList<>())
				.pullRequests(new ArrayList<>())
				.build();

		when(releaseRepository.findById(releaseId)).thenReturn(Optional.of(release));
		when(releaseRepository.save(any(Release.class))).thenAnswer(inv -> inv.getArgument(0));

		GitHubPullResponse pullResponse = new GitHubPullResponse(
				101,
				"Add stripe webhook support",
				new GitHubPullResponse.GitHubUser("bob"),
				Instant.now()
		);
		List<GitHubPullFile> files = List.of(
				new GitHubPullFile("src/main/resources/db/V3__stripe.sql", "added", 40, 0),
				new GitHubPullFile("src/main/java/StripeClient.java", "modified", 120, 10)
		);

		when(gitHubClient.getPullRequest("org", "payments-repo", 101)).thenReturn(pullResponse);
		when(gitHubClient.getPullRequestFiles("org", "payments-repo", 101)).thenReturn(files);

		GitHubSyncRequest syncRequest = new GitHubSyncRequest("org/payments-repo", 101);
		ReleaseDetailResponse detail = releaseService.syncFromGitHub(releaseId, syncRequest);

		assertThat(detail.changes()).hasSize(2);
		assertThat(detail.changes().get(0).fileName()).isEqualTo("src/main/resources/db/V3__stripe.sql");
		assertThat(detail.changes().get(0).changeType()).isEqualTo(ChangeType.ADDED);
	}

	@Test
	void updateStatus_transitionsStatusAndPublishesEvent() {
		UUID releaseId = UUID.randomUUID();
		Release release = Release.builder()
				.id(releaseId)
				.projectName("payments-service")
				.version("v2.1.0")
				.status(ReleaseStatus.DRAFT)
				.createdBy("alice")
				.build();

		when(releaseRepository.findById(releaseId)).thenReturn(Optional.of(release));
		when(releaseRepository.save(any(Release.class))).thenAnswer(inv -> inv.getArgument(0));

		UpdateReleaseStatusRequest updateReq = new UpdateReleaseStatusRequest(ReleaseStatus.BLOCKED);
		ReleaseResponse updated = releaseService.updateStatus(releaseId, updateReq);

		assertThat(updated.status()).isEqualTo(ReleaseStatus.BLOCKED);
		verify(eventPublisher).publishStatusChanged(any());
	}

	@Test
	void updateStatus_deployedSetsDeployedAt() {
		UUID releaseId = UUID.randomUUID();
		Release release = Release.builder()
				.id(releaseId)
				.projectName("payments-service")
				.version("v2.1.0")
				.status(ReleaseStatus.APPROVED)
				.createdBy("alice")
				.build();

		when(releaseRepository.findById(releaseId)).thenReturn(Optional.of(release));
		when(releaseRepository.save(any(Release.class))).thenAnswer(inv -> inv.getArgument(0));

		UpdateReleaseStatusRequest updateReq = new UpdateReleaseStatusRequest(ReleaseStatus.DEPLOYED);
		ReleaseResponse updated = releaseService.updateStatus(releaseId, updateReq);

		assertThat(updated.status()).isEqualTo(ReleaseStatus.DEPLOYED);
		assertThat(release.getDeployedAt()).isNotNull();
		verify(eventPublisher).publishStatusChanged(any());
	}
}
