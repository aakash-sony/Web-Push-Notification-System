package com.fcm.webpush.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendNotificationRequestDto {

	private List<Long> userIds;

	private List<String> guestIds;

	@NotNull(message = "Template ID is required")
	private Long templateId;
}
