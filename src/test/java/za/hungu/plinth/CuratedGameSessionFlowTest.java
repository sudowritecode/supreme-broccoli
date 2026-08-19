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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CuratedGameSessionFlowTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void curatedSessionsStayBoundToCurrentPrivateContextMembership() throws Exception {
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

        UUID conversationId = establishContact(aliceToken, bobToken, "bob" + suffix);

        mockMvc.perform(get("/api/v1/games/catalog")
                        .header(DeviceAuthenticator.HEADER_NAME, aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("WORD_CHAIN"))
                .andExpect(jsonPath("$[0].minPlayers").value(2));

        UUID roomId = UUID.fromString(objectMapper.readTree(mockMvc.perform(post("/api/v1/rooms")
                        .header(DeviceAuthenticator.HEADER_NAME, aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topic": "Private word games",
                                  "capacity": 4,
                                  "interestTags": ["word-games"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).path("id").asText());

        mockMvc.perform(post("/api/v1/rooms/{roomId}/invitations", roomId)
                        .header(DeviceAuthenticator.HEADER_NAME, aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\":\"%s\"}".formatted(bobAccountId)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/rooms/{roomId}/participants/{accountId}/admit", roomId, bobAccountId)
                        .header(DeviceAuthenticator.HEADER_NAME, aliceToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/games/rooms/{roomId}/sessions", roomId)
                        .header(DeviceAuthenticator.HEADER_NAME, malloryToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gameId\":\"WORD_CHAIN\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/games/rooms/{roomId}/sessions", roomId)
                        .header(DeviceAuthenticator.HEADER_NAME, bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gameId\":\"WORD_CHAIN\"}"))
                .andExpect(status().isForbidden());

        UUID roomGameSessionId = UUID.fromString(objectMapper.readTree(mockMvc.perform(post("/api/v1/games/rooms/{roomId}/sessions", roomId)
                        .header(DeviceAuthenticator.HEADER_NAME, aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gameId\":\"WORD_CHAIN\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sourceType").value("ROOM"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.playableClientConfigured").value(false))
                .andReturn().getResponse().getContentAsString()).path("id").asText());

        mockMvc.perform(post("/api/v1/games/sessions/{gameSessionId}/join", roomGameSessionId)
                        .header(DeviceAuthenticator.HEADER_NAME, malloryToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/games/sessions/{gameSessionId}/join", roomGameSessionId)
                        .header(DeviceAuthenticator.HEADER_NAME, bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("JOINED"));
        mockMvc.perform(post("/api/v1/games/sessions/{gameSessionId}/end", roomGameSessionId)
                        .header(DeviceAuthenticator.HEADER_NAME, bobToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/rooms/{roomId}/end", roomId)
                        .header(DeviceAuthenticator.HEADER_NAME, aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENDED"));
        mockMvc.perform(post("/api/v1/games/sessions/{gameSessionId}/join", roomGameSessionId)
                        .header(DeviceAuthenticator.HEADER_NAME, charlieToken))
                .andExpect(status().isConflict());

        UUID conversationGameSessionId = UUID.fromString(objectMapper.readTree(mockMvc.perform(post("/api/v1/games/conversations/{conversationId}/sessions", conversationId)
                        .header(DeviceAuthenticator.HEADER_NAME, aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gameId\":\"WORD_CHAIN\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sourceType").value("CONVERSATION"))
                .andReturn().getResponse().getContentAsString()).path("id").asText());
        mockMvc.perform(post("/api/v1/games/sessions/{gameSessionId}/join", conversationGameSessionId)
                        .header(DeviceAuthenticator.HEADER_NAME, bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("JOINED"));
        mockMvc.perform(post("/api/v1/games/sessions/{gameSessionId}/end", conversationGameSessionId)
                        .header(DeviceAuthenticator.HEADER_NAME, aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENDED"));
        mockMvc.perform(get("/api/v1/games")
                        .header(DeviceAuthenticator.HEADER_NAME, malloryToken))
                .andExpect(status().isNotFound());
    }

    private UUID establishContact(String senderToken, String recipientToken, String recipientUsername) throws Exception {
        String requestId = objectMapper.readTree(mockMvc.perform(post("/api/v1/contact-requests")
                        .header(DeviceAuthenticator.HEADER_NAME, senderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recipientUsername\":\"%s\"}".formatted(recipientUsername)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).path("requestId").asText();
        return UUID.fromString(objectMapper.readTree(mockMvc.perform(post("/api/v1/contact-requests/{requestId}/accept", requestId)
                        .header(DeviceAuthenticator.HEADER_NAME, recipientToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).path("conversationId").asText());
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
