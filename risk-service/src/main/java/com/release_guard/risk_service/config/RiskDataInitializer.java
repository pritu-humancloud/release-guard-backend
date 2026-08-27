package com.release_guard.risk_service.config;

import com.release_guard.risk_service.entity.RiskRule;
import com.release_guard.risk_service.repository.RiskRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RiskDataInitializer implements CommandLineRunner {

	private final RiskRuleRepository riskRuleRepository;

	@Override
	public void run(String... args) {
		if (riskRuleRepository.count() > 0) {
			log.info("Risk rules already initialized (count: {})", riskRuleRepository.count());
			return;
		}

		List<RiskRule> defaults = List.of(
				RiskRule.builder()
						.name("DATABASE_MIGRATION")
						.weight(30)
						.description("Detects database schema, migration scripts, flyway, or hibernate changes")
						.active(true)
						.build(),
				RiskRule.builder()
						.name("AUTH_SECURITY_CHANGE")
						.weight(40)
						.description("Detects authentication, security, oauth, jwt, rbac, or permission changes")
						.active(true)
						.build(),
				RiskRule.builder()
						.name("CONFIG_CHANGE")
						.weight(20)
						.description("Detects configuration file modifications (.yml, .properties, .env, /config/)")
						.active(true)
						.build(),
				RiskRule.builder()
						.name("DELETED_FILES")
						.weight(15)
						.description("Detects file deletions which might indicate breaking refactoring")
						.active(true)
						.build(),
				RiskRule.builder()
						.name("LARGE_DIFF")
						.weight(25)
						.description("Detects diff changes exceeding 500 lines of code")
						.active(true)
						.build(),
				RiskRule.builder()
						.name("HIGH_FILE_COUNT")
						.weight(20)
						.description("Detects pull requests touching 15 or more files")
						.active(true)
						.build()
		);

		riskRuleRepository.saveAll(defaults);
		log.info("Successfully seeded {} default risk rules", defaults.size());
	}
}
