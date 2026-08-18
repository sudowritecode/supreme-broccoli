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
import za.hungu.plinth.outbox.MessageOutboxRepository;
import za.hungu.plinth.outbox.OutboxStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MessagingMvpFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MessageOutboxRepository messageOutboxRepository;

    @Test
    void registersContactsAndQueuesAnAuthorizedEncryptedMessageIdempotently() throws Exception {
        JsonNode alice = register("alice", "Alice Android", "alice-public-key");
        JsonNode bob = register("bob", "Bob Android", "bob-public-key");

        String aliceToken = alice.path("deviceToken").asText();
        String bobToken = bob.path("deviceToken").asText();
        String bobDeviceId = bob.path("deviceId").asText();

        String contactRequestResponse = mockMvc.perform(post("/api/v1/contact-requests")
                        .header(DeviceAuthenticator.HEADER_NAME, aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recipientUsername\":\"bob\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode contactRequest = objectMapper.readTree(contactRequestResponse);
        String requestId = contactRequest.path("requestId").asText();

        String acceptedContactResponse = mockMvc.perform(post("/api/v1/contact-requests/{requestId}/accept", requestId)
                        .header(DeviceAuthenticator.HEADER_NAME, bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.conversationId").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String conversationId = objectMapper.readTree(acceptedContactResponse).path("conversationId").asText();
        UUID messageId = UUID.randomUUID();
        String encryptedEnvelope = """
                {
                  "messageId": "%s",
                  "conversationId": "%s",
                  "recipientDeviceId": "%s",
                  "ciphertext": "ZXhhbXBsZS1jaXBoZXJ0ZXh0"
                }
                """.formatted(messageId, conversationId, bobDeviceId);

        mockMvc.perform(post("/api/v1/messages")
                        .header(DeviceAuthenticator.HEADER_NAME, aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(encryptedEnvelope))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("queued"));

        mockMvc.perform(post("/api/v1/messages")
                        .header(DeviceAuthenticator.HEADER_NAME, aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(encryptedEnvelope))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("duplicate"));

        assertThat(messageOutboxRepository.findByMessageId(messageId))
                .isPresent()
                .get()
                .extracting(outbox -> outbox.getStatus())
                .isEqualTo(OutboxStatus.PENDING);
        assertThat(messageOutboxRepository.count()).isEqualTo(1);
    }

    @Test
    void forbidsAnUnrelatedAccountFromSendingIntoAnExistingDirectConversation() throws Exception {
        JsonNode carol = register("carol", "Carol Android", "carol-public-key");
        JsonNode dave = register("dave", "Dave Android", "dave-public-key");
        JsonNode mallory = register("mallory", "Mallory Android", "mallory-public-key");

        String carolToken = carol.path("deviceToken").asText();
        String daveToken = dave.path("deviceToken").asText();
        String requestId = objectMapper.readTree(mockMvc.perform(post("/api/v1/contact-requests")
                        .header(DeviceAuthenticator.HEADER_NAME, carolToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recipientUsername\":\"dave\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString()).path("requestId").asText();
        String conversationId = objectMapper.readTree(mockMvc.perform(post("/api/v1/contact-requests/{requestId}/accept", requestId)
                        .header(DeviceAuthenticator.HEADER_NAME, daveToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString()).path("conversationId").asText();

        mockMvc.perform(post("/api/v1/messages")
                        .header(DeviceAuthenticator.HEADER_NAME, mallory.path("deviceToken").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "messageId": "%s",
                                  "conversationId": "%s",
                                  "recipientDeviceId": "%s",
                                  "ciphertext": "ZXhhbXBsZS1jaXBoZXJ0ZXh0"
                                }
                                """.formatted(UUID.randomUUID(), conversationId, dave.path("deviceId").asText())))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsMessageIngressWithoutAValidDeviceToken() throws Exception {
        mockMvc.perform(post("/api/v1/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "messageId": "11111111-1111-1111-1111-111111111111",
                                  "conversationId": "22222222-2222-2222-2222-222222222222",
                                  "recipientDeviceId": "33333333-3333-3333-3333-333333333333",
                                  "ciphertext": "ZXhhbXBsZS1jaXBoZXJ0ZXh0"
                                }
                                """))
                .andExpect(status().isUnauthorized());
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
                .andExpect(jsonPath("$.deviceToken").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response);
    }
}
