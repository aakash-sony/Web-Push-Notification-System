package com.fcm.webpush.service;

import com.fcm.webpush.dto.request.UserLoginRequestDto;
import com.fcm.webpush.dto.request.UserRegistrationRequestDto;
import com.fcm.webpush.dto.response.UserResponseDto;

public interface UserService {

    UserResponseDto registerUser(UserRegistrationRequestDto request);

    UserResponseDto loginUser(UserLoginRequestDto request);
}
