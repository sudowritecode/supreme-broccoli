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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SignalProtocolIntegrationFlowTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void publishesAndAtomicallyClaimsPqxdhBundlesWhilePreservingOpaqueSignalTransport() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        JsonNode alice = register("signalalice" + suffix, "Alice Device", "alice-registration-key");
        JsonNode bob = register("signalbob" + suffix, "Bob Device", "bob-registration-key");
        JsonNode mallory = register("signalmallory" + suffix, "Mallory Device", "mallory-registration-key");
        String aliceToken = alice.path("deviceToken").asText();
        String bobToken = bob.path("deviceToken").asText();
        String malloryToken = mallory.path("deviceToken").asText();
        UUID bobDeviceId = UUID.fromString(bob.path("deviceId").asText());

        String conversationId = connectContacts(aliceToken, bobToken, "signalbob" + suffix);
        registerPqxdhBundle(bobToken, "bob-identity-v1", 1, 701, 801, 901, 1001);

        mockMvc.perform(get("/api/v1/signal/accounts/{username}/devices", "signalbob" + suffix)
                        .header(DeviceAuthenticator.HEADER_NAME, malloryToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/signal/accounts/{username}/devices", "signalbob" + suffix)
                        .header(DeviceAuthenticator.HEADER_NAME, aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].deviceId").value(bobDeviceId.toString()))
                .andExpect(jsonPath("$[0].protocolProfile").value("SIGNAL_PQXDH_DOUBLE_RATCHET_V1"))
                .andExpect(jsonPath("$[0].protocolDeviceId").value(1))
                .andExpect(jsonPath("$[0].registrationId").value(701))
                .andExpect(jsonPath("$[0].availableOneTimePrekeys").value(2));

        mockMvc.perform(post("/api/v1/signal/devices/{deviceId}/prekey-bundle:claim", bobDeviceId)
                        .header(DeviceAuthenticator.HEADER_NAME, aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identityKey").value("bob-identity-v1"))
                .andExpect(jsonPath("$.oneTimePrekey.prekeyId").value(801))
                .andExpect(jsonPath("$.kyberPrekey.prekeyId").value(901))
                .andExpect(jsonPath("$.kyberPrekey.lastResort").value(false));
        mockMvc.perform(post("/api/v1/signal/devices/{deviceId}/prekey-bundle:claim", bobDeviceId)
                        .header(DeviceAuthenticator.HEADER_NAME, aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.oneTimePrekey.prekeyId").value(802))
                .andExpect(jsonPath("$.kyberPrekey.prekeyId").value(902))
                .andExpect(jsonPath("$.kyberPrekey.lastResort").value(false));
        mockMvc.perform(post("/api/v1/signal/devices/{deviceId}/prekey-bundle:claim", bobDeviceId)
                        .header(DeviceAuthenticator.HEADER_NAME, aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.oneTimePrekey").doesNotExist())
                .andExpect(jsonPath("$.kyberPrekey.prekeyId").value(1001))
                .andExpect(jsonPath("$.kyberPrekey.lastResort").value(true));

        mockMvc.perform(post("/api/v1/signal/devices/{deviceId}/identity-verification", bobDeviceId)
                        .header(DeviceAuthenticator.HEADER_NAME, aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"safetyNumberFingerprint\":\"12345 67890 12345 67890\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VERIFIED"));

        registerPqxdhBundle(bobToken, "bob-identity-v2", 1, 702, 803, 903, 1002);
        mockMvc.perform(get("/api/v1/signal/devices/{deviceId}/identity-verification", bobDeviceId)
                        .header(DeviceAuthenticator.HEADER_NAME, aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CHANGED"));

        mockMvc.perform(post("/api/v1/signal/messages")
                        .header(DeviceAuthenticator.HEADER_NAME, aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "messageId": "%s",
                                  "conversationId": "%s",
                                  "recipientDeviceId": "%s",
                                  "ciphertext": "SIGNAL_PQXDH_DOUBLE_RATCHET_V1.PREKEY.b3BhcXVlLXNpZ25hbC1wcmVrZXk="
                                }
                                """.formatted(UUID.randomUUID(), conversationId, bobDeviceId)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.protocolProfile").value("SIGNAL_PQXDH_DOUBLE_RATCHET_V1"))
                .andExpect(jsonPath("$.envelopeKind").value("PREKEY"));
        mockMvc.perform(post("/api/v1/signal/messages")
                        .header(DeviceAuthenticator.HEADER_NAME, aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "messageId": "%s",
                                  "conversationId": "%s",
                                  "recipientDeviceId": "%s",
                                  "ciphertext": "SIGNAL_PQXDH_DOUBLE_RATCHET_V1.GROUP.b3BhcXVlLWdyb3Vw"
                                }
                                """.formatted(UUID.randomUUID(), conversationId, bobDeviceId)))
                .andExpect(status().isConflict());
    }

    private void registerPqxdhBundle(
            String deviceToken,
            String identityKey,
            int protocolDeviceId,
            int registrationId,
            int firstEcPrekeyId,
            int firstKyberPrekeyId,
            int lastResortKyberPrekeyId
    ) throws Exception {
        String expiry = Instant.now().plus(7, ChronoUnit.DAYS).toString();
        mockMvc.perform(post("/api/v1/signal/device-bundle")
                        .header(DeviceAuthenticator.HEADER_NAME, deviceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "protocolProfile": "SIGNAL_PQXDH_DOUBLE_RATCHET_V1",
                                  "protocolDeviceId": %d,
                                  "registrationId": %d,
                                  "identityKey": "%s",
                                  "signedPrekeyId": %d,
                                  "signedPrekeyPublic": "signed-ec-%d",
                                  "signedPrekeySignature": "signature-ec-%d",
                                  "signedPrekeyExpiresAt": "%s",
                                  "kyberLastResortPrekey": {
                                    "prekeyId": %d,
                                    "publicKey": "kyber-last-resort-%d",
                                    "signature": "kyber-last-signature-%d"
                                  },
                                  "kyberOneTimePrekeys": [
                                    {"prekeyId": %d, "publicKey": "kyber-one-%d", "signature": "kyber-signature-%d"},
                                    {"prekeyId": %d, "publicKey": "kyber-two-%d", "signature": "kyber-signature-%d"}
                                  ],
                                  "oneTimePrekeys": [
                                    {"prekeyId": %d, "publicKey": "ec-one-%d"},
                                    {"prekeyId": %d, "publicKey": "ec-two-%d"}
                                  ]
                                }
                                """.formatted(
                                protocolDeviceId, registrationId, identityKey, registrationId, registrationId, registrationId, expiry,
                                lastResortKyberPrekeyId, lastResortKyberPrekeyId, lastResortKyberPrekeyId,
                                firstKyberPrekeyId, firstKyberPrekeyId, firstKyberPrekeyId,
                                firstKyberPrekeyId + 1, firstKyberPrekeyId + 1, firstKyberPrekeyId + 1,
                                firstEcPrekeyId, firstEcPrekeyId, firstEcPrekeyId + 1, firstEcPrekeyId + 1)))
                .andExpect(status().isCreated());
    }

    private String connectContacts(String senderToken, String recipientToken, String recipientUsername) throws Exception {
        String requestId = objectMapper.readTree(mockMvc.perform(post("/api/v1/contact-requests")
                        .header(DeviceAuthenticator.HEADER_NAME, senderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recipientUsername\":\"%s\"}".formatted(recipientUsername)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).path("requestId").asText();
        return objectMapper.readTree(mockMvc.perform(post("/api/v1/contact-requests/{requestId}/accept", requestId)
                        .header(DeviceAuthenticator.HEADER_NAME, recipientToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).path("conversationId").asText();
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
