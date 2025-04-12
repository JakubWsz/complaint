package com.empik.complaint.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@Schema(description = "Basic complaint response")
public record ComplaintResponse(
		@Schema(description = "Complaint ID", example = "663e71f9a1a34d4a99a6c458") String id,
		@Schema(description = "Product ID", example = "31f871b0-321f-4063-88b2-b4aeca45adf0") String productId,
		@Schema(description = "Content of the complaint", example = "Product was defective.") String content,
		@Schema(description = "Date and time when the complaint was created") LocalDateTime creationDate,
		@Schema(description = "Date and time when the complaint was updated") LocalDateTime updateDate,
		@Schema(description = "Detected country", example = "Poland") String country
) {}
