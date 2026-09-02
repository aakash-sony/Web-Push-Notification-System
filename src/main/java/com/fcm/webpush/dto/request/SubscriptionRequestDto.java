package com.fcm.webpush.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionRequestDto {

	@NotBlank(message = "guestId is required")
	private String guestId;

	@NotBlank(message = "fcmToken is required")
	private String fcmToken;

	private String deviceType;
}
