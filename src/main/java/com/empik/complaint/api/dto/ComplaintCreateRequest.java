package com.empik.complaint.api.dto;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to create a new complaint")
public record ComplaintCreateRequest(

		@Schema(description = "Product identifier", example = "31f871b0-321f-4063-88b2-b4aeca45adf0")
		@NotBlank(message = "Product ID is required")
		String productId,

		@Schema(description = "Content of the complaint", example = "This product broke after two days.")
		@NotBlank(message = "Content is required")
		String content,

		@Schema(description = "Identifier of the complainant", example = "2a0863a2-563f-4a6c-abd3-5305bbfa6436")
		@NotBlank(message = "Complainant ID is required")
		String complainantId
) {
}
