package com.empik.complaint.repository;


import com.empik.complaint.model.Complaint;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface ComplaintRepository extends ReactiveMongoRepository<Complaint, String>, ComplaintRepositoryCustom {

	Mono<Complaint> findByProductIdAndComplainantId(String productId, String complainantId);
}
