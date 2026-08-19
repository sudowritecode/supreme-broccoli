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
import za.hungu.plinth.payments.PaymentIntentController;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SandboxPaymentIntentFlowTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void sandboxPaymentIntentsAreOwnerBoundIdempotentAndNeverLive() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        JsonNode alice = register("alice" + suffix, "Alice Android", "alice-public-key");
        JsonNode bob = register("bob" + suffix, "Bob iOS", "bob-public-key");
        String aliceToken = alice.path("deviceToken").asText();
        String bobToken = bob.path("deviceToken").asText();

        mockMvc.perform(post("/api/v1/payments/intents")
                        .header(DeviceAuthenticator.HEADER_NAME, aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amountMinor\":1299,\"currency\":\"ZAR\"}"))
                .andExpect(status().isBadRequest());

        JsonNode firstIntent = objectMapper.readTree(mockMvc.perform(post("/api/v1/payments/intents")
                        .header(DeviceAuthenticator.HEADER_NAME, aliceToken)
                        .header(PaymentIntentController.IDEMPOTENCY_KEY_HEADER, "order-" + suffix)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amountMinor\":1299,\"currency\":\"ZAR\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REQUIRES_AUTHORIZATION"))
                .andExpect(jsonPath("$.sandbox").value(true))
                .andExpect(jsonPath("$.providerReference").value(org.hamcrest.Matchers.startsWith("sandbox_pi_")))
                .andReturn().getResponse().getContentAsString());
        UUID paymentIntentId = UUID.fromString(firstIntent.path("id").asText());

        mockMvc.perform(post("/api/v1/payments/intents")
                        .header(DeviceAuthenticator.HEADER_NAME, aliceToken)
                        .header(PaymentIntentController.IDEMPOTENCY_KEY_HEADER, "order-" + suffix)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amountMinor\":1299,\"currency\":\"ZAR\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(paymentIntentId.toString()));
        mockMvc.perform(post("/api/v1/payments/intents")
                        .header(DeviceAuthenticator.HEADER_NAME, aliceToken)
                        .header(PaymentIntentController.IDEMPOTENCY_KEY_HEADER, "order-" + suffix)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amountMinor\":1300,\"currency\":\"ZAR\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/v1/payments/intents/{paymentIntentId}", paymentIntentId)
                        .header(DeviceAuthenticator.HEADER_NAME, bobToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/payments/intents/{paymentIntentId}/authorize", paymentIntentId)
                        .header(DeviceAuthenticator.HEADER_NAME, bobToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/payments/intents/{paymentIntentId}/authorize", paymentIntentId)
                        .header(DeviceAuthenticator.HEADER_NAME, aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AUTHORIZED"));
        mockMvc.perform(post("/api/v1/payments/intents/{paymentIntentId}/cancel", paymentIntentId)
                        .header(DeviceAuthenticator.HEADER_NAME, aliceToken))
                .andExpect(status().isConflict());

        UUID cancellableIntentId = UUID.fromString(objectMapper.readTree(mockMvc.perform(post("/api/v1/payments/intents")
                        .header(DeviceAuthenticator.HEADER_NAME, aliceToken)
                        .header(PaymentIntentController.IDEMPOTENCY_KEY_HEADER, "cancel-" + suffix)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amountMinor\":500,\"currency\":\"USD\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).path("id").asText());
        mockMvc.perform(post("/api/v1/payments/intents/{paymentIntentId}/cancel", cancellableIntentId)
                        .header(DeviceAuthenticator.HEADER_NAME, aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        mockMvc.perform(post("/api/v1/payments/intents/{paymentIntentId}/authorize", cancellableIntentId)
                        .header(DeviceAuthenticator.HEADER_NAME, aliceToken))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/v1/payments/live/activation")
                        .header(DeviceAuthenticator.HEADER_NAME, aliceToken))
                .andExpect(status().isConflict());
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
