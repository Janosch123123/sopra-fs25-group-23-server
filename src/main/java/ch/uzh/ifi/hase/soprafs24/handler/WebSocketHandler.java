package ch.uzh.ifi.hase.soprafs24.handler;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Snake;
import ch.uzh.ifi.hase.soprafs24.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.service.GameService;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.service.LobbyService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;


import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;



import static ch.uzh.ifi.hase.soprafs24.service.LobbyService.getGameByLobby;

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

    // Add this constructor
    public WebSocketHandler() {
        logger.info("WebSocketHandler constructor called!");
    }

    public static WebSocketSession getSessionByUserId(Long userId) {
        return userSessions.get(userId);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // This method is called when a new WebSocket connection is established
        try {
            logger.info("afterConnectionEstablished called with session: " + session.getId());

            // Extract token from URL query parameters
            String token = getTokenFromSession(session);
            logger.info("Connection token: " + token);
            User user = userRepository.findByToken(token);

            userSessions.put(user.getId(), session);

            ObjectNode response = mapper.createObjectNode();
            response.put("type", "connection_success");
            response.put("message", "Connection established successfully");

            // Send as JSON string
            session.sendMessage(new TextMessage(mapper.writeValueAsString(response)));

        } catch (Exception e) {
            logger.error("Error establishing WebSocket connection", e);
            sendErrorMessage(session, "Unable to establish connection.");
            session.close();
        }

    }

    public void broadcastToLobby(long lobbyCode, ObjectNode updateMessage) throws IOException {
        // Retrieve the lobby by its code
        Lobby lobby = lobbyService.getLobbyById(lobbyCode);
        if (lobby == null) {
            logger.info("Lobby not found for code: " + lobbyCode);
            return;
        }

        // Iterate over participant IDs and send messages to their sessions
        lobby.getParticipantIds().forEach(id -> {
            WebSocketSession individualSession = getSessionByUserId(id);
            try {
                if (individualSession != null && individualSession.isOpen()) {
                    synchronized (individualSession) {
                        individualSession.sendMessage(new TextMessage(mapper.writeValueAsString(updateMessage)));
                    }
                }
            } catch (IOException e) {
                logger.error("Error sending message to user {}", id, e);
            }
        });
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // Log received message
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
                    Lobby lobby = lobbyService.createPrivateLobby(user);
                    lobbyService.addLobbyCodeToUser(user, lobby.getId());
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
                    if (!isValid){
                        if (lobbyRepository.findById(lobbyCode).isPresent()) {
                            response.put("reason", "lobby is already full or is set to solo");
                        }
                        else{
                            response.put("reason", "invalid");
                        }
                    }
                    if (isValid) {
                        lobbyService.addLobbyCodeToUser(user, lobbyCode);

                        Lobby lobby = lobbyService.getLobbyById(lobbyCode);
                        lobby.addParticipant(user);
                        lobbyRepository.save(lobby);

                        session.getAttributes().put("lobbyCode", lobbyCode);

                        sendLobbyStateToUsers(lobbyCode);


                    }
                    session.sendMessage(new TextMessage(mapper.writeValueAsString(response)));
                } catch (Exception e) {
                    logger.error("Error creating lobby", e);
                    sendErrorMessage(session, "Failed to join lobby: " + e.getMessage());
                }

            }
            else if ("lobbySettings".equals(type)) {
                String token = getTokenFromSession(session);
                try {
                    // Get user from token
                    User user = userService.getUserByToken(token);
                    if (user == null) {
                        sendErrorMessage(session, "Invalid token");
                        return;
                    }
                    long lobbyCode = user.getLobbyCode();
                    Lobby lobby = lobbyService.getLobbyById(lobbyCode);
                    if (lobby == null) {
                        sendErrorMessage(session, "Invalid lobby ID");
                        return;
                    }
                    JsonNode settingsNode = jsonNode.get("settings");
                    String cookieSpawnRateNode;
                    if (settingsNode != null && settingsNode.has("spawnRate")){
                        lobby.setSpawnRate(settingsNode.get("spawnRate").asText());
                    }
                    if (settingsNode != null && settingsNode.has("powerupsWanted")){
                        lobby.setPowerupsWanted(settingsNode.get("powerupsWanted").asBoolean());
                    }
                    if (settingsNode != null && settingsNode.has("sugarRush")){
                        lobby.setSugarRush(settingsNode.get("sugarRush").asBoolean());
                    }
                    lobbyRepository.save(lobby);
                    lobbyRepository.flush();
                    ObjectNode objectNode = mapper.createObjectNode().put("type", "lobbySettings");
                    ObjectNode settings = mapper.createObjectNode();
                    settings.put("spawnRate", lobby.getSpawnRate());
                    settings.put("powerupsWanted", lobby.getPowerupsWanted());
                    settings.put("sugarRush", lobby.getSugarRush());
                    objectNode.set("Settings", settings);
                    broadcastToLobby(lobbyCode, objectNode);


                }
                catch (Exception e) {
                    logger.error("Error changing lobby settings: ", e);
                    sendErrorMessage(session, "Failed to change lobby settings: " + e.getMessage());
                }
            }
            else if ("requestSettings".equals(type)) {
                String token = getTokenFromSession(session);
                try {
                    // Get user from token
                    User user = userService.getUserByToken(token);
                    if (user == null) {
                        sendErrorMessage(session, "Invalid token");
                        return;
                    }
                    long lobbyCode = user.getLobbyCode();
                    Lobby lobby = lobbyService.getLobbyById(lobbyCode);
                    if (lobby == null) {
                        sendErrorMessage(session, "Invalid lobby ID");
                        return;
                    }
                    JsonNode settingsNode = jsonNode.get("settings");
                    ObjectNode objectNode = mapper.createObjectNode().put("type", "lobbySettings");
                    ObjectNode settings = mapper.createObjectNode();
                    settings.put("spawnRate", lobby.getSpawnRate());
                    settings.put("powerupsWanted", lobby.getPowerupsWanted());
                    settings.put("sugarRush", lobby.getSugarRush());
                    objectNode.set("Settings", settings);
                    broadcastToLobby(lobbyCode, objectNode);


                }
                catch (Exception e) {
                    logger.error("Error sharing lobby settings: ", e);
                    sendErrorMessage(session, "Failed to share lobby settings: " + e.getMessage());
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
                    // find lobby code from session attributes
                    long lobbyCode = user.getLobbyCode();
                    Lobby lobby = lobbyService.getLobbyById(lobbyCode);
                    if (lobby == null) {
                        sendErrorMessage(session, "Invalid lobby ID");
                        return;
                    } // looking at settings in the lobby
                    JsonNode settingsNode = jsonNode.get("settings");
                    String cookieSpawnRateNode;
                    if (settingsNode != null && settingsNode.has("spawnRate")) {
                        cookieSpawnRateNode = settingsNode.get("spawnRate").asText();
                        if (settingsNode.get("sugarRush").asBoolean()){
                            cookieSpawnRateNode = "sugarRush";
                        }

                    } else {sendErrorMessage(session, "Invalid or missing cookieSpawnRate in settings");return;}
                    boolean powerupsWanted = false;
                    if (settingsNode.has("powerupsWanted")) {
                        powerupsWanted = settingsNode.get("powerupsWanted").asBoolean();
                    }
                    // Changed from static to instance method call
                    if (!lobby.isSolo()) {
                        if (lobby.getParticipantIds().size() == 1){
                            sendErrorMessage(session, "Not enough players to start the game");
                            return;
                        }
                    } 
                    Game game = gameService.createGame(lobby, cookieSpawnRateNode, powerupsWanted);
                    lobby.setGameId(game.getGameId());
                    lobbyRepository.save(lobby);
                    gameService.start(game);

                    // Spielzustand an alle Clients senden
                    ObjectNode startMessage = mapper.createObjectNode();
                    startMessage.put("type", "gameStarted");
                    startMessage.put("gameId", game.getGameId());

                    broadcastToLobby(lobbyCode, startMessage);
                } catch (Exception e) {
                    logger.error("Error starting game", e);
                    sendErrorMessage(session, "Failed to start game: " + e.getMessage());
                }
            } else if ("lobbystate".equals(type)) {
                String token = getTokenFromSession(session);
                User user = userService.getUserByToken(token);
            
                if (user == null) {
                    sendErrorMessage(session, "Invalid token or user not found");
                    return;
                }
            
                long lobbyCode = user.getLobbyCode();
                sendLobbyStateToUsers(lobbyCode);
            } else if ("playerMove".equals(type)) {
                String token = getTokenFromSession(session);
                try {
                    User user = userService.getUserByToken(token);
                    if (user == null) {
                        sendErrorMessage(session, "Invalid token or user not found");
                        return;
                    }
                    long lobbyCode = user.getLobbyCode();
                    Lobby lobby = lobbyService.getLobbyById(lobbyCode);
                    if (lobby == null) {
                        sendErrorMessage(session, "Invalid lobby ID");
                        return;
                    }
                    Game game = LobbyService.getGameByLobby(lobby.getId());

                    if (game == null) {
                        sendErrorMessage(session, "Game not found for lobby");
                        return;
                    }
                    String direction = jsonNode.get("direction").asText();
                    gameService.respondToKeyInputs(game, user, direction);

                    ObjectNode keyChange = mapper.createObjectNode();
                    keyChange.put("type", "direction changed based on keyInput to " + direction);
                    broadcastToLobby(lobbyCode, keyChange);

                } catch (Exception e) {
                    logger.error("Error processing player move", e);
                    sendErrorMessage(session, "Failed to process player move: " + e.getMessage());
                }
            } else if ("quickPlay".equals(type)) {
                // Extract token from session
                String token = getTokenFromSession(session);
                try {
                    // Get user from token
                    User user = userService.getUserByToken(token);

                    if (user == null) {
                        sendErrorMessage(session, "Invalid token or user not found");
                        return;
                    }
                    Lobby lobby = lobbyService.handleQuickPlay(user);

                    lobbyService.addLobbyCodeToUser(user, lobby.getId());
                    // Send success response with the lobby ID
                    ObjectNode response = mapper.createObjectNode();
                    response.put("type", "quickPlayResponse");
                    response.put("lobbyId", lobby.getId());

                    session.sendMessage(new TextMessage(mapper.writeValueAsString(response)));
                    logger.info("Created lobby for session: {}", session.getId());
                } catch (Exception e) {
                    logger.error("Error creating lobby", e);
                    sendErrorMessage(session, "Failed to create lobby: " + e.getMessage());
                }
            } else if ("soloLobby".equals(type)) {
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
                    Lobby lobby = lobbyService.createPrivateLobby(user);
                    lobby.setSolo(true);
                    lobby.setVisibility("private");

                    // Save the updated lobby with the new solo and visibility values
                    lobbyRepository.save(lobby);  // Or use a service method like lobbyService.updateLobby(lobby)

                    lobbyService.addLobbyCodeToUser(user, lobby.getId());
                    // Send success response with the lobby ID
                    ObjectNode response = mapper.createObjectNode();
                    response.put("type", "lobby_created");
                    response.put("lobbyId", lobby.getId());
                    response.put("solo", lobby.isSolo());

                    session.sendMessage(new TextMessage(mapper.writeValueAsString(response)));
                    logger.info("Created lobby for session: {}", session.getId());


                } catch (Exception e) {
                    logger.error("Error creating lobby", e);
                    sendErrorMessage(session, "Failed to create lobby: " + e.getMessage());
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
        // Find which user this session belongs to before removing it
        Long userId = null;
        
        // Iterate through the userSessions map to find which user has this session
        for (Map.Entry<Long, WebSocketSession> entry : userSessions.entrySet()) {
            if (session.equals(entry.getValue())) {
                userId = entry.getKey();
                break;
            }
        }
        
        if (userId != null) {
            User user = userService.getUserById(userId);
            if (!user.getIsBot()) {
                // Use the LobbyService to find the lobby
                Lobby userLobby = lobbyService.findLobbyForUser(userId);
                if (userLobby != null) {
                    Game game = LobbyService.getGameByLobby(userLobby.getId());
                    if (game != null && (!game.isGameOver() || game.getWinnerRun())&& !game.getLobby().isSolo()) {
                        int alive = 0;
                        for (Snake snake : game.getSnakes()) {
                            if (snake.getCoordinates().length > 0) {
                                alive = alive + 1;
                            }
                        }
                        if (alive == 1) {
                            gameService.endGame(game);
                            logger.info("Game ended for gameId {}", game.getGameId());
                        }
                        else {
                            // update statistic for exiting player while game continues
                            user = userService.getUserById(userId);
                            user.setPlayedGames(user.getPlayedGames() + 1);
                            System.out.println("WinnerRun in afterConnectionClosed: " + game.getWinnerRun());
                            System.out.println("Alive payers: " + game.getSnakes());
                            user.setWinRate((double) user.getWins() / user.getPlayedGames());
                            for (Snake snake : game.getSnakes()) {
                                System.out.println(snake.getUserId());
                                System.out.println(snake.getCoordinates().length);
                                if (snake.getUserId().equals(userId)) {
                                    if (user.getLengthPR() < snake.getCoordinates().length) {
                                        user.setLengthPR(snake.getCoordinates().length);
                                    }
                                }
                            }
                            int points = 1 + (user.getWins() / 2) + (user.getKills() / 4);
                            double newLevel = 5 * Math.sqrt((double) points / 4) - 1;
                            user.setLevel(newLevel);
                            userRepository.save(user);
                            userRepository.flush();
                        }
                    }

                    // Remove user from the lobby's participants
                    userLobby.removeParticipantId(userId);

                    // Check if the user was the last one in the lobby
                    if (userLobby.getParticipantIds().isEmpty()) {
                        // If the lobby is empty, delete it
                        lobbyService.deleteLobby(userLobby.getId());
                    }
                    else {
                        // If not, just update the lobby
                        lobbyService.updateLobby(userLobby);
                    }
                    // if all participants are bots, also delete the lobby + delete the bot accounts
                    List<Long> participants = userLobby.getParticipantIds();
                    boolean onlyBots = true;
                    for (Long participantId : participants) {
                        User participant = userService.getUserById(participantId);
                        if (participant.getIsBot() == false) {
                            onlyBots = false;
                        }
                    }
                    if (onlyBots) {
                        for (Long participantId : participants) {
                            User participant = userService.getUserById(participantId);
                            userRepository.delete(participant);
                        }
                        lobbyService.deleteLobby(userLobby.getId());
                    }
                    sendLobbyStateToUsers(userLobby.getId());
                    logger.info("User {} removed from lobby {}", userId, userLobby.getId());

                    // Also remove LobbyCode in User entity
                    user = userService.getUserById(userId);
                    user.setLobbyCode(0);

                    // save updated user
                    userRepository.save(user);
                }

            }

            userSessions.entrySet().removeIf(entry -> entry.getValue().equals(session));
            logger.info("WebSocket connection closed: {} with status {}", session.getId(), status);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
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


    public void sendLobbyStateToUsers(Long lobbyCode) throws IOException {
        // Get the lobby
        Lobby lobby = lobbyService.getLobbyById(lobbyCode);
        if (lobby == null) {
            logger.warn("Attempted to send state for non-existent lobby: {}", lobbyCode);
            return;
        }
    
        logger.info("Sending lobby state to users in lobby {}", lobbyCode);
    
        // Create the response object
        ObjectNode response = mapper.createObjectNode();
        response.put("type", "lobby_state");
        response.put("lobbyId", lobby.getId());
        response.put("adminId", lobby.getAdminId());
    
        // Create an array for participants
        ArrayNode participantsArray = mapper.createArrayNode();
        
        // Add participant info to the array
        for (Long userId : lobby.getParticipantIds()) {
            User participant = userService.getUserById(userId);
            if (participant != null) {
                ObjectNode participantNode = mapper.createObjectNode();
                participantNode.put("id", participant.getId());
                participantNode.put("username", participant.getUsername());
                participantNode.put("level", participant.getLevel());
                participantsArray.add(participantNode);
            }
        }
        
        response.set("participants", participantsArray);
        
        // Broadcast the message to all participants in the lobby
        broadcastToLobby(lobbyCode, response);
    }
}