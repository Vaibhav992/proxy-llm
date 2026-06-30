package com.featureflag.proxy.exception;

import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import com.featureflag.proxy.dto.ErrorResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(LlmClientException.class)
	public ResponseEntity<ErrorResponse> handleLlmClientException(LlmClientException ex, WebRequest request) {
		log.warn("Upstream LLM error on {}: {}", request.getDescription(false), ex.getMessage());
		return buildResponse(HttpStatus.BAD_GATEWAY, ex.getMessage(), request);
	}

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ErrorResponse> handleApiException(ApiException ex, WebRequest request) {
		return buildResponse(ex.getStatus(), ex.getMessage(), request);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidationException(
			MethodArgumentNotValidException ex, WebRequest request) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.collect(Collectors.joining(", "));
		return buildResponse(HttpStatus.BAD_REQUEST, message, request);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception ex, WebRequest request) {
		log.error("Unexpected error on {}", request.getDescription(false), ex);
		return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request);
	}

	private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message, WebRequest request) {
		ErrorResponse body = ErrorResponse.builder()
				.status(status.value())
				.message(message)
				.path(extractPath(request))
				.build();
		return ResponseEntity.status(status).body(body);
	}

	private String extractPath(WebRequest request) {
		String description = request.getDescription(false);
		return description.replace("uri=", "");
	}

}
