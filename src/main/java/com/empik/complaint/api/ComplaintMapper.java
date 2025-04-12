package com.empik.complaint.api;

import com.empik.complaint.api.dto.ComplaintFullResponse;
import com.empik.complaint.api.dto.ComplaintResponse;
import com.empik.complaint.model.Complaint;

public interface ComplaintMapper {

	static ComplaintResponse toComplaintResponse(Complaint complaint) {
		return ComplaintResponse.builder()
				.id(complaint.getId())
				.productId(complaint.getProductId())
				.content(complaint.getContent())
				.creationDate(complaint.getCreationDate())
				.updateDate(complaint.getUpdateDate())
				.country(complaint.getCountry())
				.build();
	}

	static ComplaintFullResponse toFullResponse(Complaint complaint) {
		return ComplaintFullResponse.builder()
				.id(complaint.getId())
				.productId(complaint.getProductId())
				.content(complaint.getContent())
				.creationDate(complaint.getCreationDate())
				.updateDate(complaint.getUpdateDate())
				.complainantId(complaint.getComplainantId())
				.country(complaint.getCountry())
				.counter(complaint.getCounter())
				.build();
	}
}
