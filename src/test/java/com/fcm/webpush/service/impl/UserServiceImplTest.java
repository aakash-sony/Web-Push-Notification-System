package com.fcm.webpush.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.fcm.webpush.dto.request.UserLoginRequestDto;
import com.fcm.webpush.dto.request.UserRegistrationRequestDto;
import com.fcm.webpush.dto.response.UserResponseDto;
import com.fcm.webpush.entity.User;
import com.fcm.webpush.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private BCryptPasswordEncoder encoder;

    @BeforeEach
    void setUp() {
        encoder = new BCryptPasswordEncoder();
        ReflectionTestUtils.setField(userService, "passwordEncoder", encoder);
    }

    @Test
    @DisplayName("Should successfully register a new user with hashed password")
    void registerUser_Success() {
        UserRegistrationRequestDto request = UserRegistrationRequestDto.builder()
                .username("john_doe")
                .password("securePassword123")
                .confirmPassword("securePassword123")
                .build();

        User savedUser = User.builder()
                .id(1L)
                .username("john_doe")
                .password(encoder.encode("securePassword123"))
                .build();

        when(userRepository.existsByUsername("john_doe")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserResponseDto response = userService.registerUser(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("john_doe", response.getUsername());
        assertEquals("User registered successfully", response.getMessage());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when registration password and confirmPassword do not match")
    void registerUser_PasswordMismatch_ThrowsException() {
        UserRegistrationRequestDto request = UserRegistrationRequestDto.builder()
                .username("john_doe")
                .password("securePassword123")
                .confirmPassword("differentPassword")
                .build();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.registerUser(request));

        assertEquals("Password and confirm password do not match", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when registering with existing username")
    void registerUser_DuplicateUsername_ThrowsException() {
        UserRegistrationRequestDto request = UserRegistrationRequestDto.builder()
                .username("john_doe")
                .password("securePassword123")
                .confirmPassword("securePassword123")
                .build();

        when(userRepository.existsByUsername("john_doe")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.registerUser(request));

        assertEquals("Username is already taken", exception.getMessage());
    }

    @Test
    @DisplayName("Should successfully authenticate user with correct credentials")
    void loginUser_Success() {
        UserLoginRequestDto request = UserLoginRequestDto.builder()
                .username("john_doe")
                .password("securePassword123")
                .build();

        User user = User.builder()
                .id(1L)
                .username("john_doe")
                .password(encoder.encode("securePassword123"))
                .build();

        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(user));

        UserResponseDto response = userService.loginUser(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("john_doe", response.getUsername());
        assertEquals("Login successful", response.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when login password is incorrect")
    void loginUser_WrongPassword_ThrowsException() {
        UserLoginRequestDto request = UserLoginRequestDto.builder()
                .username("john_doe")
                .password("wrongPassword")
                .build();

        User user = User.builder()
                .id(1L)
                .username("john_doe")
                .password(encoder.encode("securePassword123"))
                .build();

        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(user));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.loginUser(request));

        assertEquals("Invalid username or password", exception.getMessage());
    }
}
