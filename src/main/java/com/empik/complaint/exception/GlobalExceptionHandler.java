package com.empik.complaint.exception;

import com.empik.complaint.exception.ComplaintNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ComplaintNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleComplaintNotFoundException(ComplaintNotFoundException ex, ServerWebExchange exchange) {
		ErrorResponse errorResponse = new ErrorResponse(
				LocalDateTime.now(),
				HttpStatus.NOT_FOUND.value(),
				HttpStatus.NOT_FOUND.getReasonPhrase(),
				ex.getMessage(),
				exchange.getRequest().getPath().value(),
				ex.getClass().getSimpleName()
		);

		return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex, ServerWebExchange exchange) {
		ErrorResponse errorResponse = new ErrorResponse(
				LocalDateTime.now(),
				HttpStatus.INTERNAL_SERVER_ERROR.value(),
				HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
				ex.getMessage(),
				exchange.getRequest().getPath().value(),
				ex.getClass().getSimpleName()
		);

		return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	public record ErrorResponse(
			LocalDateTime timestamp,
			int status,
			String error,
			String message,
			String path,
			String exceptionType
	) {
	}
}
