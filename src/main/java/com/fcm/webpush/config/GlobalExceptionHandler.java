package com.fcm.webpush.config;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, Object>> handleIllegalArgument(final IllegalArgumentException ex) {
		log.warn("Illegal argument error: {}", ex.getMessage());
		final var body = new HashMap<String, Object>();
		body.put("timestamp", Instant.now().toString());
		body.put("status", HttpStatus.BAD_REQUEST.value());
		body.put("error", "Bad Request");
		body.put("message", ex.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> handleValidationExceptions(final MethodArgumentNotValidException ex) {
		final var fieldErrors = new HashMap<String, String>();
		for (final var error : ex.getBindingResult().getAllErrors()) {
			if (error instanceof FieldError fieldError) {
				fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
			} else {
				fieldErrors.put(error.getObjectName(), error.getDefaultMessage());
			}
		}
		final var body = new HashMap<String, Object>();
		body.put("timestamp", Instant.now().toString());
		body.put("status", HttpStatus.BAD_REQUEST.value());
		body.put("error", "Validation Failed");
		body.put("fieldErrors", fieldErrors);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<Map<String, Object>> handleResponseStatus(final ResponseStatusException ex) {
		final var body = new HashMap<String, Object>();
		body.put("timestamp", Instant.now().toString());
		body.put("status", ex.getStatusCode().value());
		body.put("error", ex.getStatusCode().toString());
		body.put("message", ex.getReason());
		return ResponseEntity.status(ex.getStatusCode()).body(body);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, Object>> handleGeneralException(final Exception ex) {
		log.error("Unhandled server exception", ex);
		final var body = new HashMap<String, Object>();
		body.put("timestamp", Instant.now().toString());
		body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
		body.put("error", "Internal Server Error");
		body.put("message", "An unexpected error occurred. Please try again later.");
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
	}
}
