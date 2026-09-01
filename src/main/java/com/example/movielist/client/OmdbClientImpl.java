package com.example.movielist.client;

import com.example.movielist.config.OmdbProperties;
import com.example.movielist.exception.ExternalApiException;
import com.example.movielist.util.OmdbApiRequest;
import com.example.movielist.util.OmdbRateLimiterSingleton;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Calls the real OMDb HTTP API via the RestClient bean, rate-limited and error-wrapped. */
@Slf4j
@Component
@RequiredArgsConstructor
public class OmdbClientImpl implements OmdbClient {

	private final RestClient omdbRestClient;
	private final OmdbProperties properties;

	/** Sends a `s=` search request to OMDb and returns the matching items. */
	@Override
	public List<OmdbSearchItem> search(String searchTerm) {
		OmdbApiRequest request = OmdbApiRequest.builder()
				.apiKey(properties.key())
				.searchTerm(searchTerm)
				.build();
		OmdbRateLimiterSingleton.INSTANCE.acquire();

		OmdbSearchResponse response;
		try {
			response = omdbRestClient.get()
					.uri(uriBuilder -> uriBuilder
							.queryParam("apikey", request.apiKey())
							.queryParam("s", request.searchTerm())
							.build())
					.retrieve()
					.body(OmdbSearchResponse.class);
		} catch (RestClientException e) {
			log.error("OMDb search call failed for term='{}'", searchTerm, e);
			throw new ExternalApiException("Movie search is temporarily unavailable", e);
		}

		// Response=False (e.g. "Movie not found!") is OMDb's normal "no results" signal,
		// not a failure — surfaced as an empty list, not an exception.
		if (response == null || !response.isSuccess() || response.search() == null) {
			return List.of();
		}
		return response.search();
	}

	/** Sends an `i=` lookup request to OMDb and returns the full movie detail, if found. */
	@Override
	public Optional<OmdbMovieResponse> fetchByImdbId(String imdbId) {
		OmdbApiRequest request = OmdbApiRequest.builder()
				.apiKey(properties.key())
				.imdbId(imdbId)
				.build();
		OmdbRateLimiterSingleton.INSTANCE.acquire();

		OmdbMovieResponse response;
		try {
			response = omdbRestClient.get()
					.uri(uriBuilder -> uriBuilder
							.queryParam("apikey", request.apiKey())
							.queryParam("i", request.imdbId())
							.build())
					.retrieve()
					.body(OmdbMovieResponse.class);
		} catch (RestClientException e) {
			log.error("OMDb detail fetch failed for imdbId={}", imdbId, e);
			throw new ExternalApiException("Movie lookup is temporarily unavailable", e);
		}

		if (response == null || !response.isFound()) {
			return Optional.empty();
		}
		return Optional.of(response);
	}
}
