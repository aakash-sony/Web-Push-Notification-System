package com.fcm.webpush.config;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<Map<String, Object>> handleNoResourceFound(final NoResourceFoundException ex) {
		log.warn("Resource not found: {}", ex.getResourcePath());
		final var body = new HashMap<String, Object>();
		body.put("timestamp", Instant.now().toString());
		body.put("status", HttpStatus.NOT_FOUND.value());
		body.put("error", "Not Found");
		body.put("message", "The requested resource '/" + ex.getResourcePath() + "' was not found");
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<Map<String, Object>> handleMethodNotSupported(final HttpRequestMethodNotSupportedException ex) {
		log.warn("HTTP method not supported: {} on URL, supported: {}", ex.getMethod(), Arrays.toString(ex.getSupportedMethods()));
		final var body = new HashMap<String, Object>();
		body.put("timestamp", Instant.now().toString());
		body.put("status", HttpStatus.METHOD_NOT_ALLOWED.value());
		body.put("error", "Method Not Allowed");
		body.put("message", "HTTP method '" + ex.getMethod() + "' is not supported. Supported methods: " + Arrays.toString(ex.getSupportedMethods()));
		return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(body);
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<Map<String, Object>> handleMissingParameter(final MissingServletRequestParameterException ex) {
		log.warn("Missing request parameter: {}", ex.getParameterName());
		final var body = new HashMap<String, Object>();
		body.put("timestamp", Instant.now().toString());
		body.put("status", HttpStatus.BAD_REQUEST.value());
		body.put("error", "Bad Request");
		body.put("message", "Required parameter '" + ex.getParameterName() + "' is missing");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<Map<String, Object>> handleTypeMismatch(final MethodArgumentTypeMismatchException ex) {
		log.warn("Method argument type mismatch for parameter '{}': {}", ex.getName(), ex.getValue());
		final var body = new HashMap<String, Object>();
		body.put("timestamp", Instant.now().toString());
		body.put("status", HttpStatus.BAD_REQUEST.value());
		body.put("error", "Bad Request");
		body.put("message", "Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName() + "'");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<Map<String, Object>> handleNotReadable(final HttpMessageNotReadableException ex) {
		log.warn("HTTP message not readable: {}", ex.getMessage());
		final var body = new HashMap<String, Object>();
		body.put("timestamp", Instant.now().toString());
		body.put("status", HttpStatus.BAD_REQUEST.value());
		body.put("error", "Bad Request");
		body.put("message", "Malformed JSON request body or incompatible data types");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<Map<String, Object>> handleConstraintViolation(final ConstraintViolationException ex) {
		log.warn("Constraint violation: {}", ex.getMessage());
		final var body = new HashMap<String, Object>();
		body.put("timestamp", Instant.now().toString());
		body.put("status", HttpStatus.BAD_REQUEST.value());
		body.put("error", "Validation Failed");
		body.put("message", ex.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(final DataIntegrityViolationException ex) {
		log.warn("Data integrity violation: {}", ex.getMessage());
		final var body = new HashMap<String, Object>();
		body.put("timestamp", Instant.now().toString());
		body.put("status", HttpStatus.CONFLICT.value());
		body.put("error", "Conflict");
		body.put("message", "Data integrity violation or duplicate record");
		return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<Map<String, Object>> handleMediaTypeNotSupported(final HttpMediaTypeNotSupportedException ex) {
		log.warn("Unsupported media type: {}", ex.getContentType());
		final var body = new HashMap<String, Object>();
		body.put("timestamp", Instant.now().toString());
		body.put("status", HttpStatus.UNSUPPORTED_MEDIA_TYPE.value());
		body.put("error", "Unsupported Media Type");
		body.put("message", "Content type '" + ex.getContentType() + "' is not supported");
		return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(body);
	}

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
