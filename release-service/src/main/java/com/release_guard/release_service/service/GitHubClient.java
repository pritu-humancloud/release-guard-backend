package com.release_guard.release_service.service;

import com.release_guard.release_service.dto.github.GitHubPullFile;
import com.release_guard.release_service.dto.github.GitHubPullResponse;
import com.release_guard.release_service.exception.GitHubSyncException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GitHubClient {

	private final RestClient githubRestClient;

	public GitHubPullResponse getPullRequest(String owner, String repo, int prNumber) {
		return githubRestClient.get()
				.uri("/repos/{owner}/{repo}/pulls/{prNumber}", owner, repo, prNumber)
				.retrieve()
				.onStatus(HttpStatusCode::isError, (request, response) -> {
					throw new GitHubSyncException("GitHub PR lookup failed: " + response.getStatusCode());
				})
				.body(GitHubPullResponse.class);
	}

	public List<GitHubPullFile> getPullRequestFiles(String owner, String repo, int prNumber) {
		List<GitHubPullFile> files = githubRestClient.get()
				.uri("/repos/{owner}/{repo}/pulls/{prNumber}/files?per_page=100", owner, repo, prNumber)
				.retrieve()
				.onStatus(HttpStatusCode::isError, (request, response) -> {
					throw new GitHubSyncException("GitHub PR files lookup failed: " + response.getStatusCode());
				})
				.body(new ParameterizedTypeReference<>() {
				});
		return files == null ? List.of() : files;
	}
}
