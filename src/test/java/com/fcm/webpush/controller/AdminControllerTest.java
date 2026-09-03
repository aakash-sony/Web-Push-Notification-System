package com.fcm.webpush.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fcm.webpush.config.FirebaseConfig;
import com.fcm.webpush.dto.request.SendNotificationRequestDto;
import com.fcm.webpush.dto.response.SendNotificationResponseDto;
import com.fcm.webpush.service.NotificationService;

@SpringBootTest
@TestPropertySource(properties = {
		"spring.datasource.url=jdbc:h2:mem:testdb_admin_ctrl;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
		"admin.username=admin",
		"admin.password=admin123"
})
class AdminControllerTest {

	@MockitoBean
	private FirebaseConfig firebaseConfig;

	@MockitoBean
	private NotificationService notificationService;

	@Autowired
	private WebApplicationContext webApplicationContext;

	private MockMvc mockMvc;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
	}

	@Test
	void testSendNotification_AuthorizedAdmin_ReturnsSuccess() throws Exception {
		MockHttpSession session = new MockHttpSession();
		session.setAttribute("USERNAME", "admin");

		SendNotificationRequestDto request = SendNotificationRequestDto.builder()
				.userIds(List.of(1L))
				.templateId(10L)
				.build();

		SendNotificationResponseDto responseDto = SendNotificationResponseDto.builder()
				.message("Notification sent successfully")
				.status("SUCCESS")
				.templateId(10L)
				.usersSelected(1)
				.guestsSelected(0)
				.tokensFound(1)
				.notificationsSent(1)
				.notificationsFailed(0)
				.build();

		when(notificationService.sendNotification(any(SendNotificationRequestDto.class))).thenReturn(responseDto);

		mockMvc.perform(post("/admin/notifications/send")
						.session(session)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("SUCCESS"))
				.andExpect(jsonPath("$.notificationsSent").value(1));
	}

	@Test
	void testSendNotification_Unauthenticated_ReturnsUnauthorized() throws Exception {
		SendNotificationRequestDto request = SendNotificationRequestDto.builder()
				.userIds(List.of(1L))
				.templateId(10L)
				.build();

		mockMvc.perform(post("/admin/notifications/send")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isUnauthorized());
	}
}
