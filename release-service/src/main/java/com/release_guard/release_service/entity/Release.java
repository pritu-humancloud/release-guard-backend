package com.release_guard.release_service.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "releases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Release {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false)
	private String projectName;

	@Column(nullable = false)
	private String version;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private ReleaseStatus status;

	@Column(nullable = false)
	private String createdBy;

	@Column(nullable = false)
	private Instant createdAt;

	private Instant deployedAt;

	@OneToMany(mappedBy = "release", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private List<ReleaseChange> changes = new ArrayList<>();

	@OneToMany(mappedBy = "release", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private List<PullRequestInfo> pullRequests = new ArrayList<>();

	@PrePersist
	void onCreate() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
		if (status == null) {
			status = ReleaseStatus.DRAFT;
		}
	}
}
