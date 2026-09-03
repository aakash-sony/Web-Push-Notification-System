package com.fcm.webpush.service.impl;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.fcm.webpush.dto.request.UserLoginRequestDto;
import com.fcm.webpush.dto.request.UserRegistrationRequestDto;
import com.fcm.webpush.dto.response.UserResponseDto;
import com.fcm.webpush.entity.User;
import com.fcm.webpush.repository.UserRepository;
import com.fcm.webpush.service.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	@Override
	public UserResponseDto registerUser(final UserRegistrationRequestDto request) {
		if (!request.getPassword().equals(request.getConfirmPassword()))
			throw new IllegalArgumentException("Password and confirm password do not match");

		if (userRepository.existsByUsername(request.getUsername()))
			throw new IllegalArgumentException("Username is already taken");

		final var encodedPassword = passwordEncoder.encode(request.getPassword());

		final var user = User.builder()
				.username(request.getUsername())
				.password(encodedPassword)
				.build();

		final var savedUser = userRepository.save(user);

		return mapToResponseDto(savedUser);
	}

	@Override
	public UserResponseDto loginUser(final UserLoginRequestDto request) {
		final var user 			= userRepository.findByUsername(request.getUsername()).orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

		if (!passwordEncoder.matches(request.getPassword(), user.getPassword()))
			throw new IllegalArgumentException("Invalid username or password");

		return mapToResponseDto(user);
	}

	private UserResponseDto mapToResponseDto(final User user) {
		return UserResponseDto.builder()
				.id(user.getId())
				.username(user.getUsername())
				.build();
	}

}
