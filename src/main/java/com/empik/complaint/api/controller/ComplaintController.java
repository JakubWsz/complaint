package com.empik.complaint.api.controller;

import com.empik.complaint.api.ComplaintMapper;
import com.empik.complaint.api.dto.ComplaintCreateRequest;
import com.empik.complaint.api.dto.ComplaintFullResponse;
import com.empik.complaint.api.dto.ComplaintResponse;
import com.empik.complaint.service.ComplaintService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ComplaintController implements ComplaintApi {

	private final ComplaintService complaintService;

	@Override
	public Mono<ComplaintResponse> createComplaint(
			ComplaintCreateRequest request,
			ServerWebExchange exchange) {

		String ipAddress = resolveIpAddress(exchange);

		return complaintService.createComplaint(request, ipAddress)
				.map(ComplaintMapper::toComplaintResponse);
	}

	@Override
	public Mono<ComplaintResponse> updateComplaintContent(
			String id,
			String content,
			ServerWebExchange exchange) {

		String ipAddress = resolveIpAddress(exchange);

		return complaintService.updateComplaintContent(id, content, ipAddress)
				.map(ComplaintMapper::toComplaintResponse);
	}

	@Override
	public Mono<ComplaintResponse> getComplaintById(@PathVariable String id) {
		return complaintService.getComplaintById(id)
				.map(ComplaintMapper::toComplaintResponse);
	}

	@Override
	public Flux<ComplaintFullResponse> getComplaints(String productId, String complainantId,
													 LocalDateTime fromDate, LocalDateTime toDate,
													 int page, int size) {
		return complaintService.getComplaints(productId, complainantId, fromDate, toDate, page, size)
				.map(ComplaintMapper::toFullResponse);
	}

	private static String resolveIpAddress(ServerWebExchange exchange) {
		String forwardedFor = exchange.getRequest()
				.getHeaders()
				.getFirst("X-Forwarded-For");

		String ipAddress = nonNull(forwardedFor)
				? forwardedFor
				: requireNonNull(exchange.getRequest().getRemoteAddress()).getAddress().getHostAddress();

		log.debug("Request from IP: {}", ipAddress);
		return ipAddress;
	}
}
