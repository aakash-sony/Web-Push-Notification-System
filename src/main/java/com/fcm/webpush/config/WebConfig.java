package com.fcm.webpush.config;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	@Value("${app.cors.allowed-origins:*}")
	private String allowedOrigins;

	@Override
	public void addCorsMappings(final CorsRegistry registry) {
		final Set<String> originPatterns = new LinkedHashSet<>();

		// Always permit local development and deployed Vercel origins
		originPatterns.add("http://localhost:[*]");
		originPatterns.add("http://127.0.0.1:[*]");
		originPatterns.add("https://webpush-frontend.vercel.app");
		originPatterns.add("https://*.vercel.app");

		if (allowedOrigins != null && !allowedOrigins.isBlank()) {
			Arrays.stream(allowedOrigins.split(","))
					.map(String::trim)
					.filter(s -> !s.isEmpty())
					.forEach(originPatterns::add);
		}

		final String[] effectiveOrigins = originPatterns.toArray(String[]::new);

		registry.addMapping("/**")
				.allowedOriginPatterns(effectiveOrigins)
				.allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
				.allowedHeaders("*")
				.allowCredentials(true)
				.maxAge(3600);
	}
}
