package com.fcm.webpush.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class FirebaseConfig {

	@Value("${firebase.credentials.path:}")
	private String credentialsPath;

	@PostConstruct
	public void initializeFirebase() throws IOException {
		if (!FirebaseApp.getApps().isEmpty()) {
			return;
		}

		GoogleCredentials credentials = null;

		if (credentialsPath != null && !credentialsPath.isBlank()) {
			final var externalFile = new File(credentialsPath.trim());
			if (externalFile.exists()) {
				log.info("Loading Firebase credentials from external file: {}", externalFile.getAbsolutePath());
				try (final InputStream is = new FileInputStream(externalFile)) {
					credentials = GoogleCredentials.fromStream(is);
				}
			} else {
				log.warn("Configured firebase.credentials.path '{}' does not exist. Falling back to alternatives.", credentialsPath);
			}
		}

		if (credentials == null) {
			final var googleAppCreds = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
			if (googleAppCreds != null && !googleAppCreds.isBlank()) {
				log.info("Loading Firebase credentials from GOOGLE_APPLICATION_CREDENTIALS environment variable");
				credentials = GoogleCredentials.getApplicationDefault();
			}
		}

		if (credentials == null) {
			final var classpathResource = new ClassPathResource("push-notification-5366b-firebase-adminsdk-fbsvc-cc7267adcb.json");
			if (classpathResource.exists()) {
				log.info("Loading Firebase credentials from classpath fallback");
				try (final InputStream is = classpathResource.getInputStream()) {
					credentials = GoogleCredentials.fromStream(is);
				}
			}
		}

		if (credentials == null) {
			throw new IllegalStateException("Could not initialize Firebase: No valid credentials found via firebase.credentials.path, GOOGLE_APPLICATION_CREDENTIALS, or classpath.");
		}

		final var options = FirebaseOptions.builder()
				.setCredentials(credentials)
				.build();

		FirebaseApp.initializeApp(options);
		log.info("Firebase application initialized successfully");
	}

	@Bean
	public FirebaseMessaging firebaseMessaging() {
		return FirebaseMessaging.getInstance();
	}
}
