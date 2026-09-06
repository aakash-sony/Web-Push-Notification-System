package com.fcm.webpush.controller;

import java.time.Instant;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootController {

	@GetMapping({"/", "/health"})
	public ResponseEntity<Map<String, Object>> healthCheck() {
		return ResponseEntity.ok(Map.of(
				"status", "UP",
				"service", "webpush-backend",
				"timestamp", Instant.now().toString()
		));
	}
}
