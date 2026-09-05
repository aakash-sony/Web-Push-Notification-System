package com.fcm.webpush.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class SessionValidationService {

	@Value("${admin.username}")
	private String adminUsername;

	public String getAuthenticatedUsername(final HttpServletRequest request) {
		if (request == null) {
			return null;
		}
		final var session = request.getSession(false);
		if (session != null) {
			final var sessionUser = (String) session.getAttribute("username");
			if (sessionUser != null && !sessionUser.isBlank())
				return sessionUser.trim();
		}

		return null;
	}

	public void validateAdminSession(final HttpServletRequest request) {
		final var effectiveUsername = getAuthenticatedUsername(request);
		if (effectiveUsername == null || effectiveUsername.isBlank())
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated. Please log in.");

		if (!adminUsername.equals(effectiveUsername))
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Admin privileges required");
	}

	public void validateUserSession(final HttpServletRequest request, final String targetUserId) {
		final var effectiveUsername = getAuthenticatedUsername(request);
		if (effectiveUsername == null || effectiveUsername.isBlank())
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated. Please log in.");

		if (targetUserId == null || targetUserId.isBlank())
			return;

		final var session = request.getSession(false);
		final var sessionUserId = session != null && session.getAttribute("userId") != null
				? String.valueOf(session.getAttribute("userId"))
				: null;

		final var isMatched = targetUserId.equals(effectiveUsername)
				|| (sessionUserId != null && targetUserId.equals(sessionUserId))
				|| adminUsername.equals(effectiveUsername);

		if (!isMatched)
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You can only access your own notification data");
	}
}
