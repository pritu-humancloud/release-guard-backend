package com.release_guard.risk_service.repository;

import com.release_guard.risk_service.entity.RiskFactor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RiskFactorRepository extends JpaRepository<RiskFactor, UUID> {
}
