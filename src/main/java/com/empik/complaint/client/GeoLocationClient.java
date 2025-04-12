package com.empik.complaint.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.function.Function;

import static java.util.Objects.nonNull;

@Component
@Slf4j
@RequiredArgsConstructor
public class GeoLocationClient {

	private static final String FIELD_COUNTRY_NAME = "country_name";
	private static final String PARAM_API_KEY = "apiKey";
	private static final String PARAM_IP = "ip";
	private static final String PARAM_FIELDS = "fields";
	private static final String UNKNOWN_COUNTRY = "Unknown";

	private final WebClient geoLocationWebClient;

	@Value("${application.geolocation.api-key}")
	private String apiKey;

	@Value("${application.geolocation.endpoint}")
	private String endpoint;

	@Value("${application.geolocation.retry.max-attempts:3}")
	private int maxAttempts;

	@Value("${application.geolocation.retry.backoff-ms:1000}")
	private long backoffMs;

	public Mono<String> getCountryFromIp(String ipAddress) {
		log.debug("Getting country for IP: {}", ipAddress);

		return fetchCountryResponse(ipAddress)
				.map(response -> extractCountry(ipAddress, response))
				.retryWhen(retrySpec())
				.onErrorResume(WebClientResponseException.class, e -> handleHttpError(ipAddress, e))
				.onErrorResume(e -> handleUnexpectedError(ipAddress, e));
	}

	private Mono<Map<String, Object>> fetchCountryResponse(String ipAddress) {
		return geoLocationWebClient.get()
				.uri(buildRequest(ipAddress))
				.accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.bodyToMono(new ParameterizedTypeReference<>() {
				});
	}

	private Function<UriBuilder, URI> buildRequest(String ipAddress) {
		return uriBuilder -> uriBuilder
				.path(endpoint)
				.queryParam(PARAM_API_KEY, apiKey)
				.queryParam(PARAM_IP, ipAddress)
				.queryParam(PARAM_FIELDS, FIELD_COUNTRY_NAME)
				.build();
	}

	private String extractCountry(String ipAddress, Map<String, Object> response) {
		if (nonNull(response) && nonNull(response.get(FIELD_COUNTRY_NAME))) {
			String countryName = (String) response.get(FIELD_COUNTRY_NAME);
			log.debug("Country found for IP {}: {}", ipAddress, countryName);
			return countryName;
		}
		log.warn("Country not found for IP: {}", ipAddress);
		return UNKNOWN_COUNTRY;
	}

	private RetryBackoffSpec retrySpec() {
		return Retry.backoff(maxAttempts, Duration.ofMillis(backoffMs))
				.filter(this::isRetryable)
				.onRetryExhaustedThrow((spec, signal) -> signal.failure());
	}

	private Mono<String> handleHttpError(String ipAddress, WebClientResponseException e) {
		log.error("Error getting country for IP {}: {} - {}", ipAddress, e.getStatusCode(), e.getMessage());
		return Mono.just(UNKNOWN_COUNTRY);
	}

	private Mono<String> handleUnexpectedError(String ipAddress, Throwable e) {
		log.error("Unexpected error getting country for IP {}: {}", ipAddress, e.getMessage());
		return Mono.just(UNKNOWN_COUNTRY);
	}

	private boolean isRetryable(Throwable throwable) {
		return throwable instanceof WebClientRequestException;
	}
}
