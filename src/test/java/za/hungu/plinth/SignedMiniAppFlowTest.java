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
import za.hungu.plinth.miniapps.MiniAppController;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SignedMiniAppFlowTest {
    private static final String OPERATOR_KEY = "test-mini-app-operator-key";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void signedManifestsAndTicketsRequireRegistrationConsentAndOneTimeUse() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String appId = "word-tool-" + suffix;
        JsonNode alice = register("alice" + suffix, "Alice Android", "alice-public-key");
        String aliceToken = alice.path("deviceToken").asText();

        String publicKey = publicKey();
        String validSignature = signManifest(appId, "1.0.0", "Plinth Test Issuer", "https://apps.example.test/word-tool", "CONTEXT_LAUNCH,PROFILE_BASIC");

        mockMvc.perform(post("/api/v1/mini-apps/manifests")
                        .header(MiniAppController.REGISTRATION_KEY_HEADER, "incorrect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(manifestJson(appId, publicKey, validSignature)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/mini-apps/manifests")
                        .header(MiniAppController.REGISTRATION_KEY_HEADER, OPERATOR_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(manifestJson(appId + "-bad", publicKey, validSignature)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/mini-apps/manifests")
                        .header(MiniAppController.REGISTRATION_KEY_HEADER, OPERATOR_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(manifestJson(appId, publicKey, validSignature)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.appId").value(appId))
                .andExpect(jsonPath("$.permissions[0]").exists());

        mockMvc.perform(get("/api/v1/mini-apps/{appId}/versions/{appVersion}/manifest", appId, "1.0.0")
                        .header(DeviceAuthenticator.HEADER_NAME, aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.signatureBase64").value(validSignature));

        mockMvc.perform(post("/api/v1/mini-apps/{appId}/versions/{appVersion}/launch-tickets", appId, "1.0.0")
                        .header(DeviceAuthenticator.HEADER_NAME, aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"acceptedPermissions\":[\"PROFILE_BASIC\"]}"))
                .andExpect(status().isForbidden());

        JsonNode ticket = objectMapper.readTree(mockMvc.perform(post("/api/v1/mini-apps/{appId}/versions/{appVersion}/launch-tickets", appId, "1.0.0")
                        .header(DeviceAuthenticator.HEADER_NAME, aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"acceptedPermissions\":[\"PROFILE_BASIC\",\"CONTEXT_LAUNCH\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.consumed").value(false))
                .andExpect(jsonPath("$.ticketSignatureBase64").isNotEmpty())
                .andReturn().getResponse().getContentAsString());
        UUID ticketId = UUID.fromString(ticket.path("ticketId").asText());
        org.junit.jupiter.api.Assertions.assertTrue(verifyTicketSignature(ticket));

        mockMvc.perform(post("/api/v1/mini-apps/launch-tickets/{ticketId}/consume", ticketId)
                        .header(DeviceAuthenticator.HEADER_NAME, aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consumed").value(true));
        mockMvc.perform(post("/api/v1/mini-apps/launch-tickets/{ticketId}/consume", ticketId)
                        .header(DeviceAuthenticator.HEADER_NAME, aliceToken))
                .andExpect(status().isConflict());
    }

    private String manifestJson(String appId, String publicKey, String signature) {
        return """
                {
                  "appId": "%s",
                  "appVersion": "1.0.0",
                  "issuer": "Plinth Test Issuer",
                  "origin": "https://apps.example.test/word-tool",
                  "publicKeyBase64": "%s",
                  "signatureBase64": "%s",
                  "permissions": ["PROFILE_BASIC", "CONTEXT_LAUNCH"]
                }
                """.formatted(appId, publicKey, signature);
    }

    private KeyPair signingKeyPair;

    private String publicKey() throws Exception {
        return Base64.getEncoder().encodeToString(signingKeyPair().getPublic().getEncoded());
    }

    private String signManifest(String appId, String appVersion, String issuer, String origin, String canonicalPermissions) throws Exception {
        Signature signer = Signature.getInstance("Ed25519");
        signer.initSign(signingKeyPair().getPrivate());
        signer.update((
                "appId=" + appId + "\n" +
                "appVersion=" + appVersion + "\n" +
                "issuer=" + issuer + "\n" +
                "origin=" + origin + "\n" +
                "permissions=" + canonicalPermissions + "\n"
        ).getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signer.sign());
    }

    private boolean verifyTicketSignature(JsonNode ticket) throws Exception {
        Signature verifier = Signature.getInstance("Ed25519");
        verifier.initVerify(KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(
                Base64.getDecoder().decode(ticket.path("platformPublicKeyBase64").asText())
        )));
        String permissions = java.util.stream.StreamSupport.stream(ticket.path("permissions").spliterator(), false)
                .map(JsonNode::asText)
                .sorted()
                .reduce((left, right) -> left + "," + right)
                .orElse("");
        verifier.update((
                "ticketId=" + ticket.path("ticketId").asText() + "\n" +
                "appId=" + ticket.path("appId").asText() + "\n" +
                "appVersion=" + ticket.path("appVersion").asText() + "\n" +
                "accountId=" + ticket.path("accountId").asText() + "\n" +
                "deviceId=" + ticket.path("deviceId").asText() + "\n" +
                "nonce=" + ticket.path("nonce").asText() + "\n" +
                "expiresAt=" + Instant.parse(ticket.path("expiresAt").asText()).getEpochSecond() + "\n" +
                "permissions=" + permissions + "\n"
        ).getBytes(StandardCharsets.UTF_8));
        return verifier.verify(Base64.getDecoder().decode(ticket.path("ticketSignatureBase64").asText()));
    }

    private KeyPair signingKeyPair() throws Exception {
        if (signingKeyPair == null) {
            signingKeyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        }
        return signingKeyPair;
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
