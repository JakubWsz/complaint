package com.empik.complaint.repository;

import com.empik.complaint.model.Complaint;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.nonNull;

@Repository
@RequiredArgsConstructor
public class ComplaintRepositoryImpl implements ComplaintRepositoryCustom {

	private final ReactiveMongoTemplate mongoTemplate;

	@Override
	public Flux<Complaint> findByFilters(String productId, String complainantId,
										 LocalDateTime fromDate, LocalDateTime toDate,
										 int page, int size) {
		List<Criteria> criteriaList = new ArrayList<>();

		if (nonNull(productId)) criteriaList.add(Criteria.where("productId").is(productId));
		if (nonNull(complainantId)) criteriaList.add(Criteria.where("complainantId").is(complainantId));
		if (nonNull(fromDate)) criteriaList.add(Criteria.where("creationDate").gte(fromDate));
		if (nonNull(toDate)) criteriaList.add(Criteria.where("creationDate").lte(toDate));

		Criteria criteria = criteriaList.isEmpty() ? new Criteria() : new Criteria().andOperator(criteriaList.toArray(new Criteria[0]));
		Query query = Query.query(criteria)
				.skip((long) page * size)
				.limit(size);

		return mongoTemplate.find(query, Complaint.class);
	}
}
