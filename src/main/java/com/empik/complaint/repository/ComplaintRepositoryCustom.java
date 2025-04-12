package com.empik.complaint.repository;

import com.empik.complaint.model.Complaint;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;

public interface ComplaintRepositoryCustom {
	Flux<Complaint> findByFilters(String productId, String complainantId,
								  LocalDateTime fromDate, LocalDateTime toDate,
								  int page, int size);
}