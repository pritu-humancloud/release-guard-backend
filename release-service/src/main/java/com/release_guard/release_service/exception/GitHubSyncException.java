package com.release_guard.release_service.exception;

public class GitHubSyncException extends RuntimeException {

	public GitHubSyncException(String message) {
		super(message);
	}

	public GitHubSyncException(String message, Throwable cause) {
		super(message, cause);
	}
}
