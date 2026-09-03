package com.fcm.webpush.dto.response;

import com.fcm.webpush.enums.NotificationType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationTemplateResponseDto {

	private Long 				id;
	private NotificationType 	code;
	private String 				title;
	private String 				bodyTemplate;
	private boolean 			isActive;
}
