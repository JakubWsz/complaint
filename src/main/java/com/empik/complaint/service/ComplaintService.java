package com.empik.complaint.service;

import com.empik.complaint.api.dto.ComplaintCreateRequest;
import com.empik.complaint.client.GeoLocationClient;
import com.empik.complaint.exception.ComplaintNotFoundException;
import com.empik.complaint.model.Complaint;
import com.empik.complaint.repository.ComplaintRepository;
import com.mongodb.DuplicateKeyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class ComplaintService {

	private static final String UNKNOWN_COUNTRY = "Unknown";

	private final ComplaintRepository complaintRepository;
	private final GeoLocationClient geoLocationClient;

	public Mono<Complaint> createComplaint(ComplaintCreateRequest request, String ipAddress) {
		log.debug("Creating complaint for product ID: {} from complainant ID: {}",
				request.productId(), request.complainantId());

		return complaintRepository.findByProductIdAndComplainantId(request.productId(), request.complainantId())
				.flatMap(this::incrementComplaintCounter)
				.switchIfEmpty(Mono.defer(() -> handleNewComplaint(request, ipAddress)));
	}

	public Mono<Complaint> updateComplaintContent(String id, String content, String ipAddress) {
		log.debug("Updating content for complaint ID: {}", id);

		return complaintRepository.findById(id)
				.flatMap(complaint -> enrichAndApplyContentUpdate(complaint, content, ipAddress))
				.doOnNext(updated -> log.debug("Updated complaint ID: {}", updated.getId()))
				.switchIfEmpty(complaintNotFound(id));
	}

	public Mono<Complaint> getComplaintById(String id) {
		log.debug("Getting complaint by ID: {}", id);
		return complaintRepository.findById(id)
				.switchIfEmpty(complaintNotFound(id));
	}

	public Flux<Complaint> getComplaints(String productId, String complainantId,
										 LocalDateTime fromDate, LocalDateTime toDate,
										 int page, int size) {
		return complaintRepository.findByFilters(productId, complainantId, fromDate, toDate, page, size);
	}

	private Mono<Complaint> incrementComplaintCounter(Complaint existingComplaint) {
		log.debug("Complaint already exists, incrementing counter");
		existingComplaint.setCounter(existingComplaint.getCounter() + 1);
		return complaintRepository.save(existingComplaint);
	}

	private Mono<Complaint> enrichAndApplyContentUpdate(Complaint complaint, String content, String ipAddress) {
		return enrichCountryIfUnknown(complaint, ipAddress)
				.flatMap(enrichedComplaint -> applyContentUpdate(enrichedComplaint, content));
	}

	private Mono<Complaint> applyContentUpdate(Complaint complaint, String content) {
		complaint.setContent(content);
		complaint.setUpdateDate(LocalDateTime.now());
		return complaintRepository.save(complaint);
	}

	private Mono<Complaint> enrichCountryIfUnknown(Complaint complaint, String ipAddress) {
		if (!UNKNOWN_COUNTRY.equalsIgnoreCase(complaint.getCountry())) {
			return Mono.just(complaint);
		}
		return geoLocationClient.getCountryFromIp(ipAddress)
				.map(country -> {
					if (!UNKNOWN_COUNTRY.equalsIgnoreCase(country)) {
						log.debug("Enriching complaint {} with country: {}", complaint.getId(), country);
						complaint.setCountry(country);
					}
					return complaint;
				});
	}

	private Mono<Complaint> handleNewComplaint(ComplaintCreateRequest request, String ipAddress) {
		return geoLocationClient.getCountryFromIp(ipAddress)
				.flatMap(country -> saveNewComplaint(request, country));
	}

	private Mono<Complaint> saveNewComplaint(ComplaintCreateRequest request, String country) {
		Complaint newComplaint = Complaint.builder()
				.productId(request.productId())
				.content(request.content())
				.complainantId(request.complainantId())
				.country(country)
				.build();

		return complaintRepository.save(newComplaint)
				.doOnSuccess(saved -> log.info("New complaint saved with ID: {}", saved.getId()))
				.onErrorResume(DuplicateKeyException.class, e -> {
					log.debug("DuplicateKeyException caught, complaint already exists, incrementing counter");
					return complaintRepository.findByProductIdAndComplainantId(request.productId(), request.complainantId())
							.flatMap(this::incrementComplaintCounter);
				});
	}

	private Mono<Complaint> complaintNotFound(String id) {
		return Mono.error(new ComplaintNotFoundException("Complaint not found with ID: " + id));
	}
}
