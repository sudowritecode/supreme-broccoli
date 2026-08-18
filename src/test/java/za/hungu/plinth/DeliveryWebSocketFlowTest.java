package za.hungu.plinth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import za.hungu.plinth.auth.DeviceAuthenticator;
import za.hungu.plinth.delivery.MessageDeliveryRepository;
import za.hungu.plinth.delivery.MessageDeliveryStatus;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class DeliveryWebSocketFlowTest {

    @LocalServerPort
    private int port;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MessageDeliveryRepository messageDeliveryRepository;

    @Test
    void deliversOpaqueCiphertextOverAnAuthenticatedSocketAcknowledgesItAndClosesOnRevocation() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        JsonNode alice = register("alice" + suffix, "Alice Android", "alice-public-key");
        JsonNode bob = register("bob" + suffix, "Bob Android", "bob-public-key");

        String aliceToken = alice.path("deviceToken").asText();
        String bobToken = bob.path("deviceToken").asText();
        UUID bobDeviceId = UUID.fromString(bob.path("deviceId").asText());
        String bobSessionToken = objectMapper.readTree(mockMvc.perform(post("/api/v1/devices/{deviceId}/sessions", bobDeviceId)
                        .header(DeviceAuthenticator.HEADER_NAME, bobToken))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString()).path("sessionToken").asText();
        UUID messageId = UUID.randomUUID();

        BlockingQueue<String> firstMessages = new LinkedBlockingQueue<>();
        CountDownLatch firstSocketClosed = new CountDownLatch(1);
        WebSocketSession firstSocket = connect(bobSessionToken, firstMessages, firstSocketClosed);

        UUID conversationId = establishConversation(aliceToken, bobToken, "bob" + suffix);
        mockMvc.perform(post("/api/v1/messages")
                        .header(DeviceAuthenticator.HEADER_NAME, aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "messageId": "%s",
                                  "conversationId": "%s",
                                  "recipientDeviceId": "%s",
                                  "ciphertext": "ZXhhbXBsZS1jaXBoZXJ0ZXh0"
                                }
                                """.formatted(messageId, conversationId, bobDeviceId)))
                .andExpect(status().isAccepted());

        String wireMessage = firstMessages.poll(5, TimeUnit.SECONDS);
        assertThat(wireMessage).isNotNull();
        JsonNode envelope = objectMapper.readTree(wireMessage);
        assertThat(envelope.path("type").asText()).isEqualTo("encrypted_message");
        assertThat(envelope.path("messageId").asText()).isEqualTo(messageId.toString());
        assertThat(envelope.path("ciphertext").asText()).isEqualTo("ZXhhbXBsZS1jaXBoZXJ0ZXh0");

        UUID deliveryId = UUID.fromString(envelope.path("deliveryId").asText());
        firstSocket.close();
        assertThat(firstSocketClosed.await(5, TimeUnit.SECONDS)).isTrue();

        BlockingQueue<String> replayMessages = new LinkedBlockingQueue<>();
        CountDownLatch revokedSocketClosed = new CountDownLatch(1);
        WebSocketSession replaySocket = connect(bobSessionToken, replayMessages, revokedSocketClosed);
        JsonNode replayedEnvelope = objectMapper.readTree(replayMessages.poll(5, TimeUnit.SECONDS));
        assertThat(replayedEnvelope.path("deliveryId").asText()).isEqualTo(deliveryId.toString());
        assertThat(replayedEnvelope.path("messageId").asText()).isEqualTo(messageId.toString());

        replaySocket.sendMessage(new TextMessage("""
                {
                  "type": "delivery_ack",
                  "deliveryId": "%s",
                  "messageId": "%s"
                }
                """.formatted(deliveryId, messageId)));
        awaitDelivered(deliveryId);
        replaySocket.sendMessage(new TextMessage("""
                {
                  "type": "delivery_ack",
                  "deliveryId": "%s",
                  "messageId": "%s"
                }
                """.formatted(deliveryId, messageId)));
        awaitDelivered(deliveryId);

        mockMvc.perform(delete("/api/v1/devices/{deviceId}", bobDeviceId)
                        .header(DeviceAuthenticator.HEADER_NAME, bobToken))
                .andExpect(status().isNoContent());
        assertThat(revokedSocketClosed.await(5, TimeUnit.SECONDS)).isTrue();

        mockMvc.perform(post("/api/v1/devices/{deviceId}/sessions", bobDeviceId)
                        .header(DeviceAuthenticator.HEADER_NAME, bobSessionToken))
                .andExpect(status().isUnauthorized());
    }

    private WebSocketSession connect(String token, BlockingQueue<String> messages, CountDownLatch closed) throws Exception {
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.add(DeviceAuthenticator.HEADER_NAME, token);
        return new StandardWebSocketClient().execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                messages.add(message.getPayload());
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
                closed.countDown();
            }
        }, headers, URI.create("ws://localhost:" + port + "/ws/v1/delivery")).get(5, TimeUnit.SECONDS);
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

    private JsonNode register(String username, String label, String publicKey) throws Exception {
        return objectMapper.readTree(mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "deviceLabel": "%s",
                                  "publicIdentityKey": "%s"
                                }
                                """.formatted(username, label, publicKey)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString());
    }

    private void awaitDelivered(UUID deliveryId) throws InterruptedException {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(5));
        while (Instant.now().isBefore(deadline)) {
            if (messageDeliveryRepository.findById(deliveryId)
                    .map(delivery -> delivery.getStatus() == MessageDeliveryStatus.DELIVERED)
                    .orElse(false)) {
                return;
            }
            Thread.sleep(50);
        }
        assertThat(messageDeliveryRepository.findById(deliveryId))
                .isPresent()
                .get()
                .extracting(delivery -> delivery.getStatus())
                .isEqualTo(MessageDeliveryStatus.DELIVERED);
    }
}
