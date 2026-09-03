package com.fcm.webpush.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendNotificationResponseDto {
	private String 				message;
	private String 				status;
	private Long 				templateId;
	private int 				usersSelected;
	private int 				guestsSelected;
	private int 				tokensFound;
	private int 				notificationsSent;
	private int 				notificationsFailed;
}
