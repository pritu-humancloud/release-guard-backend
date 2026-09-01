package com.release_guard.api_gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.cloud.gateway.server.webflux.enabled=true"
})
class ApiGatewayApplicationTests {

	@Test
	void contextLoads() {
	}

}
