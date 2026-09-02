package com.fcm.webpush.controller;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fcm.webpush.dto.request.AssociateGuestRequestDto;
import com.fcm.webpush.service.NotificationService;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(notificationController).build();
    }

    @Test
    @DisplayName("PATCH /api/notifications/subscriptions/associate - Should succeed when user session exists")
    void associateGuest_Authenticated_Success() throws Exception {
        AssociateGuestRequestDto request = AssociateGuestRequestDto.builder()
                .guestId("GUEST-999")
                .build();

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("USER_ID", "42");

        mockMvc.perform(patch("/api/notifications/subscriptions/associate")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(notificationService).associateGuestWithUser("GUEST-999", "42");
    }

    @Test
    @DisplayName("PATCH /api/notifications/subscriptions/associate - Should fail with 401 Unauthorized when no user session")
    void associateGuest_Unauthenticated_Returns401() throws Exception {
        AssociateGuestRequestDto request = AssociateGuestRequestDto.builder()
                .guestId("GUEST-999")
                .build();

        mockMvc.perform(patch("/api/notifications/subscriptions/associate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
