package com.empik.complaint.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "complaints")
@CompoundIndex(name = "idx_product_complainant", def = "{'productId': 1, 'complainantId': 1}", unique = true)
public class Complaint {

	@Id
	private String id;

	@NotBlank(message = "Product ID is required")
	private String productId;

	@NotBlank(message = "Content is required")
	private String content;

	@Builder.Default
	private LocalDateTime creationDate = LocalDateTime.now();

	private LocalDateTime updateDate;

	@NotBlank(message = "Complainant ID is required")
	private String complainantId;

	private String country;

	@Builder.Default
	private int counter = 1;
}
