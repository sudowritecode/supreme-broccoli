package za.hungu.plinth.realtime;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class DeliveryWebSocketRegistry {

    private final Map<UUID, Set<WebSocketSession>> sessionsByDevice = new ConcurrentHashMap<>();

    public void register(UUID deviceId, WebSocketSession session) {
        sessionsByDevice.computeIfAbsent(deviceId, ignored -> new CopyOnWriteArraySet<>()).add(session);
    }

    public void unregister(UUID deviceId, WebSocketSession session) {
        Set<WebSocketSession> sessions = sessionsByDevice.get(deviceId);
        if (sessions == null) {
            return;
        }
        sessions.remove(session);
        if (sessions.isEmpty()) {
            sessionsByDevice.remove(deviceId, sessions);
        }
    }

    public boolean send(UUID deviceId, String payload) {
        Set<WebSocketSession> sessions = sessionsByDevice.get(deviceId);
        if (sessions == null || sessions.isEmpty()) {
            return false;
        }
        boolean deliveredToAtLeastOneSession = false;
        for (WebSocketSession session : sessions) {
            try {
                if (session.isOpen()) {
                    synchronized (session) {
                        session.sendMessage(new TextMessage(payload));
                    }
                    deliveredToAtLeastOneSession = true;
                } else {
                    unregister(deviceId, session);
                }
            } catch (IOException exception) {
                unregister(deviceId, session);
            }
        }
        return deliveredToAtLeastOneSession;
    }

    public void closeDevice(UUID deviceId) {
        Set<WebSocketSession> sessions = sessionsByDevice.remove(deviceId);
        if (sessions == null) {
            return;
        }
        for (WebSocketSession session : sessions) {
            try {
                session.close();
            } catch (IOException ignored) {
                // The session is already unusable and has been removed from the registry.
            }
        }
    }

    public boolean hasActiveSession(UUID deviceId) {
        Set<WebSocketSession> sessions = sessionsByDevice.get(deviceId);
        return sessions != null && sessions.stream().anyMatch(WebSocketSession::isOpen);
    }
}
