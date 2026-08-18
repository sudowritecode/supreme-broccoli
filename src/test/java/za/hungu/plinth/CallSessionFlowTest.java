package za.hungu.plinth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import za.hungu.plinth.auth.DeviceAuthenticator;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CallSessionFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void directConversationCallsRequireActiveMembershipAndDoNotIssueLiveMediaCredentials() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        JsonNode alice = register("alice" + suffix, "Alice Android", "alice-public-key");
        JsonNode bob = register("bob" + suffix, "Bob Android", "bob-public-key");
        JsonNode mallory = register("mallory" + suffix, "Mallory Android", "mallory-public-key");

        String aliceToken = alice.path("deviceToken").asText();
        String bobToken = bob.path("deviceToken").asText();
        String malloryToken = mallory.path("deviceToken").asText();
        UUID bobDeviceId = UUID.fromString(bob.path("deviceId").asText());
        UUID conversationId = establishConversation(aliceToken, bobToken, "bob" + suffix);

        UUID callSessionId = UUID.fromString(objectMapper.readTree(mockMvc.perform(post("/api/v1/calls/conversations/{conversationId}", conversationId)
                        .header(DeviceAuthenticator.HEADER_NAME, aliceToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.callerParticipantStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.mediaProviderConfigured").value(false))
                .andReturn()
                .getResponse()
                .getContentAsString()).path("callSessionId").asText());

        mockMvc.perform(post("/api/v1/calls/{callSessionId}/join", callSessionId)
                        .header(DeviceAuthenticator.HEADER_NAME, malloryToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/calls/{callSessionId}/join", callSessionId)
                        .header(DeviceAuthenticator.HEADER_NAME, bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.mediaProviderConfigured").value(false));

        mockMvc.perform(post("/api/v1/calls/{callSessionId}/end", callSessionId)
                        .header(DeviceAuthenticator.HEADER_NAME, bobToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/calls/{callSessionId}/leave", callSessionId)
                        .header(DeviceAuthenticator.HEADER_NAME, bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LEFT"));

        mockMvc.perform(post("/api/v1/calls/{callSessionId}/end", callSessionId)
                        .header(DeviceAuthenticator.HEADER_NAME, aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENDED"));

        mockMvc.perform(post("/api/v1/calls/{callSessionId}/join", callSessionId)
                        .header(DeviceAuthenticator.HEADER_NAME, bobToken))
                .andExpect(status().isConflict());

        mockMvc.perform(delete("/api/v1/devices/{deviceId}", bobDeviceId)
                        .header(DeviceAuthenticator.HEADER_NAME, bobToken))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/calls/conversations/{conversationId}", conversationId)
                        .header(DeviceAuthenticator.HEADER_NAME, bobToken))
                .andExpect(status().isUnauthorized());
    }

    private UUID establishConversation(String senderToken, String recipientToken, String recipientUsername) throws Exception {
        String requestId = objectMapper.readTree(mockMvc.perform(post("/api/v1/contact-requests")
                        .header(DeviceAuthenticator.HEADER_NAME, senderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recipientUsername\":\"%s\"}".formatted(recipientUsername)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString()).path("requestId").asText();
        return UUID.fromString(objectMapper.readTree(mockMvc.perform(post("/api/v1/contact-requests/{requestId}/accept", requestId)
                        .header(DeviceAuthenticator.HEADER_NAME, recipientToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString()).path("conversationId").asText());
    }

    private JsonNode register(String username, String deviceLabel, String publicIdentityKey) throws Exception {
        return objectMapper.readTree(mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "deviceLabel": "%s",
                                  "publicIdentityKey": "%s"
                                }
                                """.formatted(username, deviceLabel, publicIdentityKey)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString());
    }
}
