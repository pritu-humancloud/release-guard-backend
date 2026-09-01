package com.release_guard.risk_service.repository;

import com.release_guard.risk_service.entity.RiskRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RiskRuleRepository extends JpaRepository<RiskRule, UUID> {

	List<RiskRule> findAllByActiveTrue();

	List<RiskRule> findAllByOrderByNameAsc();

	boolean existsByNameIgnoreCase(String name);
}
