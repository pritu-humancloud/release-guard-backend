package com.release_guard.risk_service.entity;

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
@Table(name = "risk_assessments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskAssessment {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "release_id", nullable = false)
	private UUID releaseId;

	@Column(nullable = false)
	private int score;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private RiskLevel riskLevel;

	@Column(nullable = false)
	private Instant calculatedAt;

	@OneToMany(mappedBy = "assessment", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private List<RiskFactor> factors = new ArrayList<>();

	@PrePersist
	void onCreate() {
		if (calculatedAt == null) {
			calculatedAt = Instant.now();
		}
	}
}
