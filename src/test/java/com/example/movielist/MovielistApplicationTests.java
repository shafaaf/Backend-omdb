package com.example.movielist;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * The full-context smoke test: does the entire ApplicationContext wire up
 * successfully — every @Bean, every constructor-injected dependency actually
 * resolvable, JPA entities mapping cleanly, Spring Security's filter chain
 * assembling without a missing bean. This is the cheapest test that would have
 * caught, for example, a typo'd bean name or a circular dependency, at the cost
 * of not asserting anything about behavior. The "test" profile (see
 * application.yml) supplies dummy JWT/OMDb secrets so this doesn't need real
 * environment variables.
 */
@SpringBootTest
@ActiveProfiles("test")
class MovielistApplicationTests {

	@Test
	void contextLoads() {
	}

}
