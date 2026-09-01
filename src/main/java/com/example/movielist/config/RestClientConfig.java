package com.example.movielist.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/** Provides the RestClient bean used by client.OmdbClientImpl to call OMDb. */
@Configuration
@RequiredArgsConstructor
public class RestClientConfig {

	private final OmdbProperties omdbProperties;

	/** A RestClient pre-configured with OMDb's base URL. */
	@Bean
	public RestClient omdbRestClient() {
		return RestClient.builder()
				.baseUrl(omdbProperties.baseUrl())
				.build();
	}
}
