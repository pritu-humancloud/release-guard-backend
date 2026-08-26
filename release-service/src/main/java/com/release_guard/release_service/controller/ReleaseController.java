package com.release_guard.release_service.controller;

import com.release_guard.release_service.dto.request.CreateReleaseRequest;
import com.release_guard.release_service.dto.request.GitHubSyncRequest;
import com.release_guard.release_service.dto.request.UpdateReleaseStatusRequest;
import com.release_guard.release_service.dto.response.ReleaseChangeResponse;
import com.release_guard.release_service.dto.response.ReleaseDetailResponse;
import com.release_guard.release_service.dto.response.ReleaseResponse;
import com.release_guard.release_service.entity.ReleaseStatus;
import com.release_guard.release_service.service.ReleaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/releases")
@RequiredArgsConstructor
@Tag(name = "Releases")
public class ReleaseController {

	private final ReleaseService releaseService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create a release manually")
	public ReleaseResponse create(@Valid @RequestBody CreateReleaseRequest request) {
		return releaseService.create(request);
	}

	@GetMapping
	@Operation(summary = "List releases (filter by project/status)")
	public List<ReleaseResponse> list(
			@RequestParam(required = false) String projectName,
			@RequestParam(required = false) ReleaseStatus status
	) {
		return releaseService.list(projectName, status);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Release detail including file changes")
	public ReleaseDetailResponse getById(@PathVariable UUID id) {
		return releaseService.getById(id);
	}

	@PutMapping("/{id}/status")
	@Operation(summary = "Update release status")
	public ReleaseResponse updateStatus(
			@PathVariable UUID id,
			@Valid @RequestBody UpdateReleaseStatusRequest request
	) {
		return releaseService.updateStatus(id, request);
	}

	@GetMapping("/{id}/changes")
	@Operation(summary = "List file-level changes")
	public List<ReleaseChangeResponse> listChanges(@PathVariable UUID id) {
		return releaseService.listChanges(id);
	}

	@PostMapping("/{id}/github-sync")
	@Operation(summary = "Pull PR diff from GitHub and store changes + PR info")
	public ReleaseDetailResponse githubSync(
			@PathVariable UUID id,
			@Valid @RequestBody GitHubSyncRequest request
	) {
		return releaseService.syncFromGitHub(id, request);
	}
}
