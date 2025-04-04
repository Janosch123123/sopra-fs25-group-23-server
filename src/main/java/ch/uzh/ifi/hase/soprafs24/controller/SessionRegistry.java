package ch.uzh.ifi.hase.soprafs24.controller;

import org.springframework.web.socket.WebSocketSession;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SessionRegistry {

    // Map lobbyId -> set of active sessions
    private final Map<Long, Set<WebSocketSession>> lobbySessions = new ConcurrentHashMap<>();

    public void addSession(long lobbyId, WebSocketSession session) {
        lobbySessions.computeIfAbsent(lobbyId, key ->
            Collections.newSetFromMap(new ConcurrentHashMap<>())
        ).add(session);
        System.out.println("Added session " + session.getId() + " to lobby " + lobbyId +
        ". Current sessions in lobby: " + lobbySessions.get(lobbyId).size());
    }

    public void removeSession(Long lobbyId, WebSocketSession session) {
        Set<WebSocketSession> sessions = lobbySessions.get(lobbyId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                lobbySessions.remove(lobbyId);
            }
        }
        System.out.println("REmoved session " + session.getId() + " to lobby " + lobbyId +
        ". Current sessions in lobby: " + lobbySessions.get(lobbyId).size());
    }

    public Set<WebSocketSession> getSessions(Long lobbyId) {
        return lobbySessions.getOrDefault(lobbyId, Collections.emptySet());
    }
}