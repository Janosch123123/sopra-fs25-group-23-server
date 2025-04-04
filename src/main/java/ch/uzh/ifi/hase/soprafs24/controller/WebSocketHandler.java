package ch.uzh.ifi.hase.soprafs24.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.Set;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.service.LobbyService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;

public class WebSocketHandler extends TextWebSocketHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(WebSocketHandler.class);
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private LobbyService lobbyService;

    @Autowired
    private UserService userService;

    private final SessionRegistry sessionRegistry = new SessionRegistry();

    // Add this constructor
    public WebSocketHandler() {
        System.out.println("WebSocketHandler constructor called!");
    }
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // This method is called when a new WebSocket connection is established
        System.out.println("afterConnectionEstablished called with session: " + session.getId());
        logger.info("New WebSocket connection established: {}", session.getId());
        
        // Extract token from URL query parameters
        String token = getTokenFromSession(session);
        System.out.println("Connection token: " + token);

        ObjectNode response = mapper.createObjectNode();
        response.put("type", "connection_success");
        response.put("message", "Connection established successfully");
        
        // Send as JSON string
        session.sendMessage(new TextMessage(mapper.writeValueAsString(response)));
    }

    private void broadcastToLobby(Long lobbyCode, String updateMessage) throws IOException {
        // Retrieve all sessions registered for the given lobby code
        Set<WebSocketSession> sessions = sessionRegistry.getSessions(lobbyCode);
        for (WebSocketSession sess : sessions) {
            if (sess.isOpen()) {
                ObjectNode response = mapper.createObjectNode();
                response.put("type", "lobby_update");
                response.put("message", updateMessage);
                sess.sendMessage(new TextMessage(mapper.writeValueAsString(response)));
            }
        }
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // Log received message
        System.out.println("handleTextMessage called with message: " + message.getPayload());
        logger.info("Received message from {}: {}", session.getId(), message.getPayload());
        
        try {
            // Parse the JSON message
            JsonNode jsonNode = mapper.readTree(message.getPayload());
            
            // Extract the message type
            String type = jsonNode.has("type") ? jsonNode.get("type").asText() : null;
            
            if (type == null) {
                sendErrorMessage(session, "Missing message type");
                return;
            }
            
        // Check if it's a create_lobby message
        if ("create_lobby".equals(type)) {
            // Extract token from session
            String token = getTokenFromSession(session);

            try {
                // Get user from token
                User user = userService.getUserByToken(token);
                
                if (user == null) {
                    sendErrorMessage(session, "Invalid token or user not found");
                    return;
                }
                
                // Direct call to LobbyService's createLobby method
                Lobby lobby = lobbyService.createLobby(user);
                sessionRegistry.addSession(lobby.getId(), session);
                
                // Send success response with the lobby ID
                ObjectNode response = mapper.createObjectNode();
                response.put("type", "lobby_created");
                response.put("lobbyId", lobby.getId());
                
                session.sendMessage(new TextMessage(mapper.writeValueAsString(response)));
                logger.info("Created lobby for session: {}", session.getId());
            } catch (Exception e) {
                logger.error("Error creating lobby", e);
                sendErrorMessage(session, "Failed to create lobby: " + e.getMessage());
            }
        } else if ("validateLobby".equals(type)) {
            String token = getTokenFromSession(session);

            try {
                User user = userService.getUserByToken(token);
                long lobbyCode = jsonNode.get("lobbyCode").asInt();
                
                if (user == null) {
                    sendErrorMessage(session, "Invalid token or user not found");
                    return;
                }

                boolean isValid = lobbyService.validateLobby(lobbyCode);

                ObjectNode response = mapper.createObjectNode();
                response.put("type", "validateLobbyResponse");
                response.put("valid", isValid);
                if (isValid) {
                    lobbyService.addLobbyCodeToUser(user, lobbyCode);

                    sessionRegistry.addSession(lobbyCode, session);

                    session.getAttributes().put("lobbyCode", lobbyCode);

                    // Broadcast the user's name to all users in the lobby
                    String userName = user.getUsername();
                    int userlvl = user.getLevel();
                    broadcastToLobby(lobbyCode, userName + "," + userlvl + " has joined the lobby.");
                }
                session.sendMessage(new TextMessage(mapper.writeValueAsString(response)));

            } catch (Exception e) {
                logger.error("Error creating lobby", e);
                sendErrorMessage(session, "Failed to join lobby: " + e.getMessage());
            }

        }
        
        else {
            sendErrorMessage(session, "Unknown message type: " + type);
        }
        } catch (IOException e) {
            logger.error("Error parsing message", e);
            sendErrorMessage(session, "Failed to parse message: " + e.getMessage());
        }
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        System.out.println("afterConnectionClosed called for session: " + session.getId());

        // This method is called when the WebSocket connection is closed
        logger.info("WebSocket connection closed: {} with status {}", session.getId(), status);

        Object lobbyCodeObj = session.getAttributes().get("lobbyCode");

        if (lobbyCodeObj instanceof Long) {
            Long lobbyCode = (Long) lobbyCodeObj;
            sessionRegistry.removeSession(lobbyCode, session);
        }
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.out.println("handleTransportError called for session: " + session.getId());

        // This method is called when a transport error occurs
        logger.error("Error in WebSocket transport for session: {}", session.getId(), exception);
    }
    
    /**
     * Extracts the authentication token from the WebSocket session
     */
    private String getTokenFromSession(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null && query.contains("token=")) {
            String token = query.substring(query.indexOf("token=") + 6);
            if (token.contains("&")) {
                token = token.substring(0, token.indexOf("&"));
            }
            return token;
        }
        return null;
    }
    
    /**
     * Sends an error message to the client
     */
    private void sendErrorMessage(WebSocketSession session, String errorMessage) throws IOException {
        ObjectNode response = mapper.createObjectNode();
        response.put("type", "error");
        response.put("message", errorMessage);
        session.sendMessage(new TextMessage(mapper.writeValueAsString(response)));
        logger.warn("Sent error to client: {}", errorMessage);
    }
}