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
class PrivateGroupFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void privateGroupsRequireInvitationAcceptanceAndActiveRoleAuthorisationForCiphertextIngress() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        JsonNode alice = register("alice" + suffix, "Alice Android", "alice-public-key");
        JsonNode bob = register("bob" + suffix, "Bob Android", "bob-public-key");
        JsonNode charlie = register("charlie" + suffix, "Charlie Android", "charlie-public-key");

        String aliceToken = alice.path("deviceToken").asText();
        String bobToken = bob.path("deviceToken").asText();
        String charlieToken = charlie.path("deviceToken").asText();
        UUID aliceDeviceId = UUID.fromString(alice.path("deviceId").asText());
        UUID bobDeviceId = UUID.fromString(bob.path("deviceId").asText());

        UUID groupId = UUID.fromString(objectMapper.readTree(mockMvc.perform(post("/api/v1/groups")
                        .header(DeviceAuthenticator.HEADER_NAME, aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Weekend plans\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.callerRole").value("OWNER"))
                .andExpect(jsonPath("$.callerStatus").value("ACTIVE"))
                .andReturn()
                .getResponse()
                .getContentAsString()).path("conversationId").asText());

        mockMvc.perform(post("/api/v1/groups/{groupId}/invitations", groupId)
                        .header(DeviceAuthenticator.HEADER_NAME, aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"bob%s\"}".formatted(suffix)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("INVITED"));

        mockMvc.perform(post("/api/v1/messages")
                        .header(DeviceAuthenticator.HEADER_NAME, aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(envelope(groupId, bobDeviceId)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/groups/{groupId}/invitations/accept", groupId)
                        .header(DeviceAuthenticator.HEADER_NAME, bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(post("/api/v1/messages")
                        .header(DeviceAuthenticator.HEADER_NAME, aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(envelope(groupId, bobDeviceId)))
                .andExpect(status().isAccepted());

        mockMvc.perform(post("/api/v1/groups/{groupId}/invitations", groupId)
                        .header(DeviceAuthenticator.HEADER_NAME, bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"charlie%s\"}".formatted(suffix)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/groups/{groupId}/invitations", groupId)
                        .header(DeviceAuthenticator.HEADER_NAME, aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"charlie%s\"}".formatted(suffix)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/groups/{groupId}/invitations/accept", groupId)
                        .header(DeviceAuthenticator.HEADER_NAME, charlieToken))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/groups/{groupId}/members/{accountId}", groupId, charlie.path("accountId").asText())
                        .header(DeviceAuthenticator.HEADER_NAME, bobToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/groups/{groupId}/members/{accountId}", groupId, bob.path("accountId").asText())
                        .header(DeviceAuthenticator.HEADER_NAME, aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REMOVED"));

        mockMvc.perform(post("/api/v1/messages")
                        .header(DeviceAuthenticator.HEADER_NAME, bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(envelope(groupId, aliceDeviceId)))
                .andExpect(status().isForbidden());
    }

    private String envelope(UUID conversationId, UUID recipientDeviceId) {
        return """
                {
                  "messageId": "%s",
                  "conversationId": "%s",
                  "recipientDeviceId": "%s",
                  "ciphertext": "ZXhhbXBsZS1jaXBoZXJ0ZXh0"
                }
                """.formatted(UUID.randomUUID(), conversationId, recipientDeviceId);
    }

    private JsonNode register(String username, String deviceLabel, String publicIdentityKey) throws Exception {
        String response = mockMvc.perform(post("/api/v1/accounts")
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
                .getContentAsString();
        return objectMapper.readTree(response);
    }
}
