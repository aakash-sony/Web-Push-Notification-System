package com.fcm.webpush.service;

import java.util.List;

import com.fcm.webpush.dto.response.GuestResponseDto;
import com.fcm.webpush.dto.response.NotificationTemplateResponseDto;
import com.fcm.webpush.dto.response.UserResponseDto;

public interface AdminService {

	void verifyAdminAuthorization();

	List<GuestResponseDto> getAllGuests();

	List<UserResponseDto> getAllUsers();

	List<NotificationTemplateResponseDto> getAllNotificationTemplates();
}
