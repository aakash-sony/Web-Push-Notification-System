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
public class AssociateGuestRequestDto {

	@NotBlank(message = "guestId is required")
	private String guestId;

	private String userId;
}
