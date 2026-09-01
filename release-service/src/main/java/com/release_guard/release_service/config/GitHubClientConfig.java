package com.release_guard.release_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

@Configuration
public class GitHubClientConfig {

	@Bean
	RestClient githubRestClient(
			@Value("${github.api.base-url}") String baseUrl,
			@Value("${github.token:}") String token
	) {
		RestClient.Builder builder = RestClient.builder()
				.baseUrl(baseUrl)
				.defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
				.defaultHeader(HttpHeaders.USER_AGENT, "release-guard");

		if (token != null && !token.isBlank()) {
			builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
		}

		return builder.build();
	}
}
