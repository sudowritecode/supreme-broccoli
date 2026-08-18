package za.hungu.plinth.realtime;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class DeliveryWebSocketConfiguration implements WebSocketConfigurer {

    private final DeliveryWebSocketHandler deliveryWebSocketHandler;
    private final DeliveryWebSocketHandshakeInterceptor handshakeInterceptor;

    public DeliveryWebSocketConfiguration(
            DeliveryWebSocketHandler deliveryWebSocketHandler,
            DeliveryWebSocketHandshakeInterceptor handshakeInterceptor
    ) {
        this.deliveryWebSocketHandler = deliveryWebSocketHandler;
        this.handshakeInterceptor = handshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(deliveryWebSocketHandler, "/ws/v1/delivery")
                .addInterceptors(handshakeInterceptor);
    }
}
