package za.hungu.plinth.realtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import za.hungu.plinth.delivery.MessageDeliveryService;

import java.io.IOException;
import java.util.UUID;

@Component
public class DeliveryWebSocketHandler extends TextWebSocketHandler {

    private static final String ACKNOWLEDGEMENT_TYPE = "delivery_ack";

    private final DeliveryWebSocketRegistry registry;
    private final MessageDeliveryService messageDeliveryService;
    private final ObjectMapper objectMapper;

    public DeliveryWebSocketHandler(
            DeliveryWebSocketRegistry registry,
            MessageDeliveryService messageDeliveryService,
            ObjectMapper objectMapper
    ) {
        this.registry = registry;
        this.messageDeliveryService = messageDeliveryService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        UUID deviceId = deviceId(session);
        registry.register(deviceId, session);
        messageDeliveryService.replayPendingForDevice(deviceId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        try {
            DeliveryAcknowledgement acknowledgement = objectMapper.readValue(
                    message.getPayload(),
                    DeliveryAcknowledgement.class
            );
            if (!ACKNOWLEDGEMENT_TYPE.equals(acknowledgement.type())
                    || acknowledgement.deliveryId() == null
                    || acknowledgement.messageId() == null) {
                session.close(CloseStatus.BAD_DATA);
                return;
            }
            messageDeliveryService.acknowledge(deviceId(session), acknowledgement.deliveryId(), acknowledgement.messageId());
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            session.close(CloseStatus.BAD_DATA);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        registry.unregister(deviceId(session), session);
    }

    private UUID deviceId(WebSocketSession session) {
        Object value = session.getAttributes().get(DeliveryWebSocketHandshakeInterceptor.DEVICE_ID_ATTRIBUTE);
        if (!(value instanceof UUID deviceId)) {
            throw new IllegalStateException("Authenticated device identifier is missing from WebSocket session.");
        }
        return deviceId;
    }

    private record DeliveryAcknowledgement(String type, UUID deliveryId, UUID messageId) {
    }
}
