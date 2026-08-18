package za.hungu.plinth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import za.hungu.plinth.auth.DeviceAuthenticator;
import za.hungu.plinth.rooms.RoomReportRepository;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class InviteOnlyRoomFlowTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoomReportRepository roomReportRepository;

    @Test
    void inviteOnlyRoomsRequireHostApprovalAndEnforceMatchingSafetyControls() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        JsonNode alice = register("alice" + suffix, "Alice Android", "alice-public-key");
        JsonNode bob = register("bob" + suffix, "Bob iOS", "bob-public-key");
        JsonNode charlie = register("charlie" + suffix, "Charlie Android", "charlie-public-key");
        JsonNode mallory = register("mallory" + suffix, "Mallory Android", "mallory-public-key");

        String aliceToken = alice.path("deviceToken").asText();
        String bobToken = bob.path("deviceToken").asText();
        String charlieToken = charlie.path("deviceToken").asText();
        String malloryToken = mallory.path("deviceToken").asText();
        UUID bobAccountId = UUID.fromString(bob.path("accountId").asText());
        UUID charlieAccountId = UUID.fromString(charlie.path("accountId").asText());
        UUID malloryAccountId = UUID.fromString(mallory.path("accountId").asText());

        establishContact(aliceToken, bobToken, "bob" + suffix);
        establishContact(aliceToken, charlieToken, "charlie" + suffix);

        UUID roomId = UUID.fromString(objectMapper.readTree(mockMvc.perform(post("/api/v1/rooms")
                        .header(DeviceAuthenticator.HEADER_NAME, aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topic": "Board games after work",
                                  "capacity": 4,
                                  "interestTags": ["board-games", "music"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.hostAccountId").value(alice.path("accountId").asText()))
                .andReturn().getResponse().getContentAsString()).path("id").asText());

        mockMvc.perform(post("/api/v1/rooms/{roomId}/invitations", roomId)
                        .header(DeviceAuthenticator.HEADER_NAME, aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\":\"%s\"}".formatted(bobAccountId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("LOBBY"));

        mockMvc.perform(get("/api/v1/rooms/suggestions")
                        .header(DeviceAuthenticator.HEADER_NAME, bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].roomId").value(roomId.toString()))
                .andExpect(jsonPath("$[0].reason").value("DIRECT_INVITE"));

        mockMvc.perform(post("/api/v1/rooms/{roomId}/participants/{accountId}/admit", roomId, bobAccountId)
                        .header(DeviceAuthenticator.HEADER_NAME, malloryToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/rooms/{roomId}/participants/{accountId}/admit", roomId, bobAccountId)
                        .header(DeviceAuthenticator.HEADER_NAME, aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ADMITTED"));

        mockMvc.perform(post("/api/v1/rooms/{roomId}/participants/{accountId}/co-host", roomId, bobAccountId)
                        .header(DeviceAuthenticator.HEADER_NAME, aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CO_HOST"));

        mockMvc.perform(post("/api/v1/rooms/{roomId}/reports", roomId)
                        .header(DeviceAuthenticator.HEADER_NAME, bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reportedAccountId\":\"%s\",\"reason\":\"HARASSMENT\"}".formatted(malloryAccountId)))
                .andExpect(status().isCreated());
        assertThat(roomReportRepository.count()).isEqualTo(1L);

        mockMvc.perform(put("/api/v1/rooms/interest-preferences")
                        .header(DeviceAuthenticator.HEADER_NAME, charlieToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interestTags\":[\"board-games\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("board-games"));

        mockMvc.perform(get("/api/v1/rooms/suggestions")
                        .header(DeviceAuthenticator.HEADER_NAME, charlieToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].roomId").value(roomId.toString()))
                .andExpect(jsonPath("$[0].reason").value("SHARED_INTEREST"));

        mockMvc.perform(post("/api/v1/rooms/{roomId}/entry-requests", roomId)
                        .header(DeviceAuthenticator.HEADER_NAME, charlieToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("LOBBY"));

        mockMvc.perform(post("/api/v1/rooms/{roomId}/participants/{accountId}/admit", roomId, charlieAccountId)
                        .header(DeviceAuthenticator.HEADER_NAME, bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ADMITTED"));

        mockMvc.perform(post("/api/v1/rooms/{roomId}/blocks", roomId)
                        .header(DeviceAuthenticator.HEADER_NAME, aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\":\"%s\"}".formatted(charlieAccountId)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/rooms/{roomId}/invitations", roomId)
                        .header(DeviceAuthenticator.HEADER_NAME, aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\":\"%s\"}".formatted(charlieAccountId)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/rooms/{roomId}/entry-requests", roomId)
                        .header(DeviceAuthenticator.HEADER_NAME, charlieToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/rooms/{roomId}/participants/{accountId}", roomId, bobAccountId)
                        .header(DeviceAuthenticator.HEADER_NAME, aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REMOVED"));

        mockMvc.perform(post("/api/v1/rooms/{roomId}/entry-requests", roomId)
                        .header(DeviceAuthenticator.HEADER_NAME, bobToken))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/v1/rooms")
                        .header(DeviceAuthenticator.HEADER_NAME, malloryToken))
                .andExpect(status().isMethodNotAllowed());
    }

    private void establishContact(String senderToken, String recipientToken, String recipientUsername) throws Exception {
        String requestId = objectMapper.readTree(mockMvc.perform(post("/api/v1/contact-requests")
                        .header(DeviceAuthenticator.HEADER_NAME, senderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recipientUsername\":\"%s\"}".formatted(recipientUsername)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).path("requestId").asText();
        mockMvc.perform(post("/api/v1/contact-requests/{requestId}/accept", requestId)
                        .header(DeviceAuthenticator.HEADER_NAME, recipientToken))
                .andExpect(status().isOk());
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
                .andReturn().getResponse().getContentAsString());
    }
}
