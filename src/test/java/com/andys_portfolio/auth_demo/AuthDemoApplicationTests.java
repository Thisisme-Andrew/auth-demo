package com.andys_portfolio.auth_demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.docker.compose.skip.in-tests=false",
		"spring.jpa.hibernate.ddl-auto=update"
})
class AuthDemoApplicationTests {

	@Test
	void contextLoads() {
	}

}
