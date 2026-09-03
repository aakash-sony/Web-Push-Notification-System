package com.fcm.webpush.dto.response;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationLogResponseDto {
	private Long 				id;
	private String 				userId;
	private String 				guestId;
	private Long 				templateId;
	private String 				title;
	private String 				body;
	private String 				code;
	private boolean 			isRead;
	private Instant 			readAt;
	private Instant 			createdAt;
}
