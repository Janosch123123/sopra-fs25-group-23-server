package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.service.GameService;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.List;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.service.LobbyService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;



public class WebSocketHandler extends TextWebSocketHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(WebSocketHandler.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private static final Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();


    @Autowired
    private LobbyService lobbyService;

    @Autowired
    private UserService userService;

    @Autowired
    private GameService gameService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LobbyRepository lobbyRepository;

    private final SessionRegistry sessionRegistry = new SessionRegistry();

    // Add this constructor
    public WebSocketHandler() {
        System.out.println("WebSocketHandler constructor called!");
    }

    public static WebSocketSession getSessionByUserId(Long userId) {
        return userSessions.get(userId);
    }

        @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // This method is called when a new WebSocket connection is established
        try {
            System.out.println("afterConnectionEstablished called with session: " + session.getId());
            logger.info("New WebSocket connection established: {}", session.getId());

            // Extract token from URL query parameters
            String token = getTokenFromSession(session);
            System.out.println("Connection token: " + token);
            User user = userRepository.findByToken(token);

            userSessions.put(user.getId(), session);


            ObjectNode response = mapper.createObjectNode();
            response.put("type", "connection_success");
            response.put("message", "Connection established successfully");

            // Send as JSON string
            session.sendMessage(new TextMessage(mapper.writeValueAsString(response)));

        }catch (Exception e) {
            logger.error("Error establishing WebSocket connection", e);
            sendErrorMessage(session, "Unable to establish connection.");
            session.close();
        }

    }

    private void broadcastToLobby(long lobbyCode, ObjectNode updateMessage) throws IOException {
        // Retrieve the lobby by its code
        Lobby lobby = lobbyService.getLobbyById(lobbyCode);
        if (lobby == null) {
            System.out.println("Lobby not found for code: " + lobbyCode);
            return;
        }

        // Iterate over participant IDs and send messages to their sessions
        lobby.getParticipantIds().forEach(id -> {
            WebSocketSession individualSession = getSessionByUserId(id);
            try {
                if (individualSession != null && individualSession.isOpen()) {
                    individualSession.sendMessage(new TextMessage(mapper.writeValueAsString(updateMessage)));
                }
            } catch (IOException e) {
                logger.error("Error sending message to user {}", id, e);
            }
        });
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
            // long lobbyCode = jsonNode.has("lobbyCode") ? jsonNode.get("lobbyCode").asInt() : -1;   WE DO NOT EXPECT  IT ALL THE TIME; ONLY WHEN JOINING

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
                    long lobbyCode = lobby.getId();
                    lobbyService.addLobbyCodeToUser(user, lobby.getId());
                    sessionRegistry.addSession(lobbyCode, session);
                    // Send success response with the lobby ID
                    ObjectNode response = mapper.createObjectNode();
                    response.put("type", "lobby_created");
                    response.put("lobbyId", lobby.getId());

                    session.sendMessage(new TextMessage(mapper.writeValueAsString(response)));
                    logger.info("Created lobby for session: {}", session.getId());
                }
                catch (Exception e) {
                    logger.error("Error creating lobby", e);
                    sendErrorMessage(session, "Failed to create lobby: " + e.getMessage());
                }
            }
            else if ("validateLobby".equals(type)) {
                String token = getTokenFromSession(session);

                try {
                    User user = userService.getUserByToken(token);
                    long lobbyCode = jsonNode.get("lobbyCode").asInt(); //Marc defined this variable above (new)

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

                        Lobby lobby = lobbyService.getLobbyById(lobbyCode);
                        lobby.addParticipant(user);
                        lobbyRepository.save(lobby);

                    sessionRegistry.addSession(lobbyCode, session);

                    session.getAttributes().put("lobbyCode", lobbyCode);
                    // Broadcast the user's name to all users in the lobby
                        String userName = user.getUsername();
                        int userlvl = user.getLevel();
                    // broadcastToLobby(lobbyCode, );
                    }
                    session.sendMessage(new TextMessage(mapper.writeValueAsString(response)));

                }
                catch (Exception e) {
                    logger.error("Error creating lobby", e);
                    sendErrorMessage(session, "Failed to join lobby: " + e.getMessage());
                }

            }
            else if ("startGame".equals(type)) {
                // Extract token from session
                String token = getTokenFromSession(session);

                try {
                    // Get user from token
                    User user = userService.getUserByToken(token);

                    if (user == null) {
                        sendErrorMessage(session, "Invalid token or Admin not found");
                        return;
                    }

                    long lobbyCode = user.getLobbyCode();
                    // Direct call to GameService's createGame method
                    //Get lobby by user
                    
                    Lobby lobby = jsonNode.has("lobbyId") ? lobbyService.getLobbyById(jsonNode.get("lobbyId").asLong()) : null;
                    if (lobby == null) {
                        sendErrorMessage(session, "Invalid lobby ID");
                        return;
                    }
                    
                    // Changed from static to instance method call
                    Game game = gameService.createGame(lobby);
                    lobby.setGameId(game.getGameId());
                    lobbyRepository.save(lobby);
                    gameService.start(game);

                    // Spielzustand an alle Clients senden
                    ObjectNode startMessage = mapper.createObjectNode();
                    startMessage.put("type", "gameStarted");
                    startMessage.put("gameId", game.getGameId());

                    broadcastToLobby(lobbyCode, startMessage);

                    // lobby.getParticipantIds().forEach(id -> {
                    //     WebSocketSession individualSession = getSessionByUserId(id); // Fixed: use id instead of user.getId()
                    //     try {
                    //         if (individualSession != null && individualSession.isOpen()) {
                    //             individualSession.sendMessage(new TextMessage(mapper.writeValueAsString(startMessage)));
                    //         }
                    //     } catch (IOException e) {
                    //         logger.error("Error sending start message to user {}", id, e);
                    //     }
                    // });
                }
                catch (Exception e) {
                    logger.error("Error starting game", e);
                    sendErrorMessage(session, "Failed to start game: " + e.getMessage());
                }
            }
        
            else{
                    sendErrorMessage(session, "Unknown message type: " + type);
                }
            } catch(IOException e){
                logger.error("Error parsing message", e);
                sendErrorMessage(session, "Failed to parse message: " + e.getMessage());
            }
        }

        @Override
        public void afterConnectionClosed (WebSocketSession session, CloseStatus status) throws Exception {
            System.out.println("afterConnectionClosed called for session: " + session.getId());

            userSessions.entrySet().removeIf(entry -> entry.getValue().equals(session));
            // This method is called when the WebSocket connection is closed
            logger.info("WebSocket connection closed: {} with status {}", session.getId(), status);

            Object lobbyCodeObj = session.getAttributes().get("lobbyCode");

        if (lobbyCodeObj instanceof Long lobbyCode) {
            sessionRegistry.removeSession(lobbyCode, session);
        }
    }
        @Override
        public void handleTransportError (WebSocketSession session, Throwable exception) throws Exception {
            System.out.println("handleTransportError called for session: " + session.getId());

            // This method is called when a transport error occurs
            logger.error("Error in WebSocket transport for session: {}", session.getId(), exception);
        }

        /**
         * Extracts the authentication token from the WebSocket session
         */
        private String getTokenFromSession (WebSocketSession session){
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
        private void sendErrorMessage (WebSocketSession session, String errorMessage) throws IOException {
            ObjectNode response = mapper.createObjectNode();
            response.put("type", "error");
            response.put("message", errorMessage);
            session.sendMessage(new TextMessage(mapper.writeValueAsString(response)));
            logger.warn("Sent error to client: {}", errorMessage);
        }
    /**
     * Send the current state of the lobby to all users in the lobby
     * @param lobbyCode the lobby code
     * @param lobby the lobby object
     * @throws IOException if an error occurs while sending the message
     */
    private void sendLobbyStateToUsers(Long lobbyCode, Lobby lobby) throws IOException {

        logger.info("Sending lobby state to users in lobby {}: {}", lobbyCode, lobby);

        Set<WebSocketSession> sessions = sessionRegistry.getSessions(lobbyCode);
        for (WebSocketSession sess : sessions) {
            if (sess.isOpen()) {
                ObjectNode response = mapper.createObjectNode();
                response.put("type", "lobby_state");
                response.put("lobbyId", lobby.getId());
                response.put("adminId", lobby.getAdminId());

                // Create an empty list to hold the users
                List<User> participants = new ArrayList<>();

                // Loop through participant IDs and fetch each user
                for (Long userId : lobby.getParticipantIds()) {
                    User user = userService.getUserById(userId);
                    if (user != null) {
                        participants.add(user);
                    }
                }
                ArrayNode participantsArray = mapper.createArrayNode();
                for (User participant : participants) {
                    ObjectNode participantNode = mapper.createObjectNode();
                    participantNode.put("id", participant.getId());
                    participantNode.put("username", participant.getUsername());
                    participantNode.put("level", participant.getLevel());
                    participantsArray.add(participantNode);
                }
                response.set("participants", participantsArray);
                // Add other lobby details as needed
                sess.sendMessage(new TextMessage(mapper.writeValueAsString(response)));
            }
        }
    }}