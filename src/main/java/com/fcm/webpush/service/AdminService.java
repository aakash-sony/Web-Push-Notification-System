package com.fcm.webpush.service;

import java.util.List;

import com.fcm.webpush.dto.response.GuestResponseDto;
import com.fcm.webpush.dto.response.NotificationTemplateResponseDto;
import com.fcm.webpush.dto.response.UserResponseDto;

import jakarta.servlet.http.HttpSession;

public interface AdminService {

	void verifyAdminAuthorization(HttpSession session);

	List<GuestResponseDto> getAllGuests();

	List<UserResponseDto> getAllUsers();

	List<NotificationTemplateResponseDto> getAllNotificationTemplates();
}
