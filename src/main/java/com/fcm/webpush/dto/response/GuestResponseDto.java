package com.fcm.webpush.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuestResponseDto {

	private Long 		id;
	private String 		guestId;
	private String 		deviceType;
	private boolean 	isActive;
}
