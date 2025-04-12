package com.empik.complaint.api.controller;

import com.empik.complaint.api.dto.ComplaintCreateRequest;
import com.empik.complaint.api.dto.ComplaintFullResponse;
import com.empik.complaint.api.dto.ComplaintResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Tag(name = "Complaints", description = "API for managing complaints")
@RequestMapping("/api/v1/complaints")
public interface ComplaintApi {

	@Operation(summary = "Create a new complaint")
	@PostMapping
	Mono<ComplaintResponse> createComplaint(
			@Valid @RequestBody ComplaintCreateRequest request,
			ServerWebExchange exchange);

	@Operation(summary = "Update complaint content by ID")
	@PutMapping("/{id}/content")
	Mono<ComplaintResponse> updateComplaintContent(
			@PathVariable String id,
			@RequestParam String content,
			ServerWebExchange exchange);

	@Operation(summary = "Get a complaint by ID")
	@GetMapping("/{id}")
	Mono<ComplaintResponse> getComplaintById(@PathVariable String id);

	@Operation(summary = "List complaints with optional filters and pagination")
	@GetMapping
	Flux<ComplaintFullResponse> getComplaints(
			@RequestParam(required = false) String productId,
			@RequestParam(required = false) String complainantId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size);
}
