package com.example.movielist.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Kept as its own @Configuration class rather than @EnableJpaAuditing directly on
 * MovielistApplication so narrow test slices that don't load JPA at all
 * (@WebMvcTest, which only boots the web layer) don't pull it in and fail with
 * "JPA metamodel must not be empty" — @EnableJpaAuditing on the main
 * @SpringBootApplication class is picked up by every slice that uses it as the
 * configuration root, JPA-aware or not.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
