package com.fcm.webpush.service.impl;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
	@Transactional
	public UserResponseDto registerUser(final UserRegistrationRequestDto request) {
		if (!request.getPassword().equals(request.getConfirmPassword()))
			throw new IllegalArgumentException("Password and confirm password do not match");

		if (userRepository.existsByUsername(request.getUsername()))
			throw new IllegalArgumentException("Username is already taken");

		final String encodedPassword = passwordEncoder.encode(request.getPassword());

		final User user = User.builder()
				.username(request.getUsername())
				.password(encodedPassword)
				.build();

		final User savedUser = userRepository.save(user);

		return UserResponseDto.builder()
				.id(savedUser.getId())
				.username(savedUser.getUsername())
				.message("User registered successfully")
				.build();
	}

	@Override
	@Transactional(readOnly = true)
	public UserResponseDto loginUser(final UserLoginRequestDto request) {
		final User user = userRepository.findByUsername(request.getUsername())
				.orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

		if (!passwordEncoder.matches(request.getPassword(), user.getPassword()))
			throw new IllegalArgumentException("Invalid username or password");

		return UserResponseDto.builder()
				.id(user.getId())
				.username(user.getUsername())
				.message("Login successful")
				.build();
	}
}
