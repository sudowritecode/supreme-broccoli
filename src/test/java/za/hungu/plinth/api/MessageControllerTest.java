package za.hungu.plinth.api;

import za.hungu.plinth.auth.AuthenticatedDevice;
import za.hungu.plinth.auth.DeviceAuthenticator;
import za.hungu.plinth.messaging.MessageIngressResult;
import za.hungu.plinth.messaging.MessageIngressService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MessageController.class)
class MessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DeviceAuthenticator deviceAuthenticator;

    @MockBean
    private MessageIngressService messageIngressService;

    @Test
    void acceptsAuthenticatedClientEncryptedEnvelope() throws Exception {
        UUID messageId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        UUID senderDeviceId = UUID.randomUUID();
        UUID senderAccountId = UUID.randomUUID();
        UUID recipientDeviceId = UUID.randomUUID();
        Instant receivedAt = Instant.parse("2026-08-18T12:00:00Z");

        when(deviceAuthenticator.require("development-token"))
                .thenReturn(new AuthenticatedDevice(senderDeviceId, senderAccountId));
        when(messageIngressService.queue(any(), any()))
                .thenReturn(new MessageIngressResult(messageId, receivedAt, false));

        mockMvc.perform(post("/api/v1/messages")
                        .header(DeviceAuthenticator.HEADER_NAME, "development-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "messageId": "%s",
                                  "conversationId": "%s",
                                  "recipientDeviceId": "%s",
                                  "ciphertext": "ZXhhbXBsZS1jaXBoZXJ0ZXh0"
                                }
                                """.formatted(messageId, conversationId, recipientDeviceId)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.messageId").value(messageId.toString()))
                .andExpect(jsonPath("$.status").value("queued"));
    }
}
