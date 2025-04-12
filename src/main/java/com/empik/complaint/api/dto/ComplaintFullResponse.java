package com.empik.complaint.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@Schema(description = "Full response containing complaint details")
public record ComplaintFullResponse(
		@Schema(description = "Complaint ID", example = "663e71f9a1a34d4a99a6c458") String id,
		@Schema(description = "Product ID", example = "31f871b0-321f-4063-88b2-b4aeca45adf0") String productId,
		@Schema(description = "Content of the complaint", example = "This product broke after two days.") String content,
		@Schema(description = "Date and time when the complaint was created") LocalDateTime creationDate,
		@Schema(description = "Date and time when the complaint was updated") LocalDateTime updateDate,
		@Schema(description = "Complainant ID", example = "2a0863a2-563f-4a6c-abd3-5305bbfa6436") String complainantId,
		@Schema(description = "Detected country based on IP address", example = "Poland") String country,
		@Schema(description = "Access counter for this complaint", example = "4") int counter
) {
}
