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
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

import java.time.Duration;
import java.util.Map;

import static java.util.Objects.nonNull;

@Component
@Slf4j
@RequiredArgsConstructor
public class GeoLocationClient {

	private static final String UNKNOWN_COUNTRY = "Unknown";
	private static final String FIELD_COUNTRY = "country";

	private final WebClient geoLocationWebClient;

	@Value("${application.geolocation.retry.max-attempts:3}")
	private int maxAttempts;

	@Value("${application.geolocation.retry.backoff-ms:1000}")
	private long backoffMs;

	@Value("${application.geolocation.base-url}")
	private String geoLocationBaseUrl;

	public Mono<String> getCountryFromIp(String ipAddress) {
		log.debug("Getting country for IP: {}", ipAddress);

		return geoLocationWebClient.get()
				.uri(geoLocationBaseUrl + "/json/{ip}", ipAddress)
				.accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
				.map(response -> extractCountry(ipAddress, response))
				.retryWhen(retrySpec())
				.onErrorResume(WebClientResponseException.class, e -> handleHttpError(ipAddress, e))
				.onErrorResume(e -> handleUnexpectedError(ipAddress, e));
	}

	private String extractCountry(String ipAddress, Map<String, Object> response) {
		if (nonNull(response) && nonNull(response.get(FIELD_COUNTRY))) {
			String country = (String) response.get(FIELD_COUNTRY);
			log.debug("Country found for IP {}: {}", ipAddress, country);
			return country;
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

