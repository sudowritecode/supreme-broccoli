package za.hungu.plinth.realtime;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import za.hungu.plinth.auth.AuthenticatedDevice;
import za.hungu.plinth.auth.AuthenticationRequiredException;
import za.hungu.plinth.auth.DeviceAuthenticator;

import java.util.Map;

@Component
public class DeliveryWebSocketHandshakeInterceptor implements HandshakeInterceptor {

    public static final String DEVICE_ID_ATTRIBUTE = "authenticatedDeviceId";
    public static final String ACCOUNT_ID_ATTRIBUTE = "authenticatedAccountId";

    private final DeviceAuthenticator deviceAuthenticator;

    public DeliveryWebSocketHandshakeInterceptor(DeviceAuthenticator deviceAuthenticator) {
        this.deviceAuthenticator = deviceAuthenticator;
    }

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler webSocketHandler,
            Map<String, Object> attributes
    ) {
        try {
            AuthenticatedDevice authenticatedDevice = deviceAuthenticator.require(
                    request.getHeaders().getFirst(DeviceAuthenticator.HEADER_NAME)
            );
            attributes.put(DEVICE_ID_ATTRIBUTE, authenticatedDevice.deviceId());
            attributes.put(ACCOUNT_ID_ATTRIBUTE, authenticatedDevice.accountId());
            return true;
        } catch (AuthenticationRequiredException exception) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler webSocketHandler,
            Exception exception
    ) {
        // No request credentials are retained after the authenticated identifiers are attached.
    }
}
