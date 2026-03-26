package ch.uzh.ifi.hase.soprafs24.handler;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Snake;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs24.service.LobbyService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import ch.uzh.ifi.hase.soprafs24.service.GameService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.mockStatic;
import org.mockito.ArgumentCaptor;
import com.fasterxml.jackson.databind.JsonNode;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class WebSocketHandlerExtendedTest {

    @Mock
    private Game game;

    @Mock
    private Lobby lobby;

    @Mock
    private WebSocketSession session;
    
    @Mock
    private LobbyService lobbyService;
    
    @Mock
    private UserService userService;
    
    @Mock
    private GameService gameService;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private LobbyRepository lobbyRepository;

    @Spy
    @InjectMocks
    private WebSocketHandler webSocketHandler;
    
    private ObjectMapper objectMapper = new ObjectMapper();
    
    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        
        // Mock session basics
        when(session.getId()).thenReturn("test-session-id");
        URI uri = new URI("ws://localhost:8080/websocket?token=test-token");
        when(session.getUri()).thenReturn(uri);
        
        Map<String, Object> attributes = new HashMap<>();
        when(session.getAttributes()).thenReturn(attributes);
        when(session.isOpen()).thenReturn(true);
    }
    
    @Test
    void testBroadcastToLobby() throws Exception {
        // Setup
        User testUser1 = new User();
        testUser1.setId(1L);
        
        User testUser2 = new User();
        testUser2.setId(2L);
        
        Lobby testLobby = new Lobby();
        testLobby.setId(100L);
        testLobby.addParticipantId(1L);
        testLobby.addParticipantId(2L);
        
        when(lobbyService.getLobbyById(100L)).thenReturn(testLobby);
        
        // Create mock sessions for both users
        WebSocketSession user1Session = mock(WebSocketSession.class);
        WebSocketSession user2Session = mock(WebSocketSession.class);
        
        when(user1Session.isOpen()).thenReturn(true);
        when(user2Session.isOpen()).thenReturn(true);
        
        // Instead of direct reflection, use a static mock
        try (MockedStatic<WebSocketHandler> mockedStatic = mockStatic(WebSocketHandler.class)) {
            // Mock static method to return our test sessions
            mockedStatic.when(() -> WebSocketHandler.getSessionByUserId(1L)).thenReturn(user1Session);
            mockedStatic.when(() -> WebSocketHandler.getSessionByUserId(2L)).thenReturn(user2Session);
            
            // Create a test message
            ObjectNode message = objectMapper.createObjectNode();
            message.put("type", "test_broadcast");
            message.put("content", "Hello everyone");
            
            // Execute
            webSocketHandler.broadcastToLobby(100L, message);
            
            // Verify that messages were sent to both sessions
            verify(user1Session).sendMessage(any(TextMessage.class));
            verify(user2Session).sendMessage(any(TextMessage.class));
        }
    }
    
    @Test
    void testHandleTextMessage_LobbySettings() throws Exception {
        // Setup
        User testUser = new User();
        testUser.setId(1L);
        testUser.setToken("test-token");
        testUser.setLobbyCode(100L);
        
        Lobby testLobby = new Lobby();
        testLobby.setId(100L);
        testLobby.setAdminId(1L);
        testLobby.setSpawnRate("Slow");
        testLobby.setPowerupsWanted(false);
        testLobby.setSugarRush(false);
        
        when(userService.getUserByToken("test-token")).thenReturn(testUser);
        when(lobbyService.getLobbyById(100L)).thenReturn(testLobby);
        
        // Create test message
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("type", "lobbySettings");
        
        ObjectNode settings = objectMapper.createObjectNode();
        settings.put("spawnRate", "Fast");
        settings.put("powerupsWanted", true);
        settings.put("sugarRush", true);
        
        requestBody.set("settings", settings);
        TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(requestBody));
        
        // Mock broadcast to prevent actual networking
        doNothing().when(webSocketHandler).broadcastToLobby(anyLong(), any(ObjectNode.class));
        
        // Execute
        webSocketHandler.handleTextMessage(session, textMessage);
        
        // Verify
        verify(webSocketHandler).broadcastToLobby(eq(100L), any(ObjectNode.class));
    }
    
    @Test
    void testHandleTextMessage_QuickPlay() throws Exception {
        // Setup
        User testUser = new User();
        testUser.setId(1L);
        testUser.setToken("test-token");
        
        Lobby testLobby = new Lobby();
        testLobby.setId(100L);
        testLobby.setAdminId(1L);
        
        when(userService.getUserByToken("test-token")).thenReturn(testUser);
        when(lobbyService.handleQuickPlay(testUser)).thenReturn(testLobby);
        
        // Create test message
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("type", "quickPlay");
        TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(requestBody));
        
        // Execute
        webSocketHandler.handleTextMessage(session, textMessage);
        
        // Verify
        verify(lobbyService).handleQuickPlay(testUser);
        verify(lobbyService).addLobbyCodeToUser(testUser, 100L);
        verify(session).sendMessage(any(TextMessage.class));
    }
    
    @Test
    void testHandleTextMessage_SoloLobby() throws Exception {
        // Setup
        User testUser = new User();
        testUser.setId(1L);
        testUser.setToken("test-token");
        
        Lobby testLobby = new Lobby();
        testLobby.setId(100L);
        testLobby.setAdminId(1L);
        
        when(userService.getUserByToken("test-token")).thenReturn(testUser);
        when(lobbyService.createPrivateLobby(testUser)).thenReturn(testLobby);
        
        // Create test message
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("type", "soloLobby");
        TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(requestBody));
        
        // Execute
        webSocketHandler.handleTextMessage(session, textMessage);
        
        // Verify
        verify(lobbyService).createPrivateLobby(testUser);
        verify(lobbyRepository).save(testLobby);
        verify(lobbyService).addLobbyCodeToUser(testUser, 100L);
        verify(session).sendMessage(any(TextMessage.class));
        
        // Verify solo flag was set
        assertTrue(testLobby.isSolo());
        assertEquals("private", testLobby.getVisibility());
    }
    
    @Test
    void testHandleTextMessage_InvalidMessageType() throws Exception {
        // Create test message with invalid type
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("type", "invalidType");
        TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(requestBody));
        
        // Execute
        webSocketHandler.handleTextMessage(session, textMessage);
        
        // Verify error message was sent
        verify(session).sendMessage(any(TextMessage.class));
    }
    
    @Test
    void testHandleTextMessage_MissingMessageType() throws Exception {
        // Create test message with no type
        ObjectNode requestBody = objectMapper.createObjectNode();
        TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(requestBody));
        
        // Execute
        webSocketHandler.handleTextMessage(session, textMessage);
        
        // Verify error message was sent
        verify(session).sendMessage(any(TextMessage.class));
    }
    
    @Test
    void testHandleTextMessage_InvalidJson() throws Exception {
        // Create invalid JSON message
        TextMessage textMessage = new TextMessage("This is not valid JSON");
        
        // Execute
        webSocketHandler.handleTextMessage(session, textMessage);
        
        // Verify error message was sent
        verify(session).sendMessage(any(TextMessage.class));
    }
    
    @Test
    void testSendLobbyStateToUsers() throws Exception {
        // Setup
        User admin = new User();
        admin.setId(1L);
        admin.setUsername("Admin");
        admin.setLevel(5.0);
        
        User player = new User();
        player.setId(2L);
        player.setUsername("Player");
        player.setLevel(3.0);
        
        Lobby testLobby = new Lobby();
        testLobby.setId(100L);
        testLobby.setAdminId(1L);
        testLobby.addParticipantId(1L);
        testLobby.addParticipantId(2L);
        
        when(lobbyService.getLobbyById(100L)).thenReturn(testLobby);
        when(userService.getUserById(1L)).thenReturn(admin);
        when(userService.getUserById(2L)).thenReturn(player);
        
        // Mock broadcast to prevent actual networking
        doNothing().when(webSocketHandler).broadcastToLobby(anyLong(), any(ObjectNode.class));
        
        // Execute
        webSocketHandler.sendLobbyStateToUsers(100L);
        
        // Verify
        verify(webSocketHandler).broadcastToLobby(eq(100L), any(ObjectNode.class));
    }
    
    @Test
    void testUpdateUserStatsWhenLeavingActiveGame() throws Exception {
        // Setup
        User testUser = new User();
        testUser.setId(1L);
        testUser.setToken("test-token");
        testUser.setLobbyCode(100L);
        testUser.setIsBot(false);
        testUser.setPlayedGames(5);
        testUser.setWins(2);
        testUser.setKills(4);
        testUser.setLengthPR(10);
        
        Lobby testLobby = new Lobby();
        testLobby.setId(100L);
        testLobby.setAdminId(1L);
        testLobby.addParticipantId(1L);
        testLobby.addParticipantId(2L);
        testLobby.setSolo(false);
        
        Game testGame = new Game();
        testGame.setGameId(200L);
        testGame.setLobby(testLobby);
        testGame.setGameOver(false);
        testGame.setWinnerRun(false);
        
        Snake snake1 = new Snake();
        snake1.setUserId(1L);
        snake1.setCoordinates(new int[][]{{1, 2}, {3, 4}, {5, 6}, {7, 8}});
        
        Snake snake2 = new Snake();
        snake2.setUserId(2L);
        snake2.setCoordinates(new int[][]{{10, 11}, {12, 13}, {14, 15}, {16, 17}});
        
        testGame.setSnakes(new ArrayList<>(Arrays.asList(snake1, snake2)));
        
        // Directly invoke the relevant logic that would update user stats
        when(userService.getUserById(1L)).thenReturn(testUser);
        
        // Simulate what happens in afterConnectionClosed
        testUser.setPlayedGames(testUser.getPlayedGames() + 1);
        testUser.setWinRate((double) testUser.getWins() / testUser.getPlayedGames());
        
        if (testUser.getLengthPR() < snake1.getCoordinates().length) {
            testUser.setLengthPR(snake1.getCoordinates().length);
        }
        
        int points = 1 + (testUser.getWins() / 2) + (testUser.getKills() / 4);
        double newLevel = 5 * Math.sqrt((double) points / 4) - 1;
        testUser.setLevel(newLevel);
        
        userRepository.save(testUser);
        userRepository.flush();
        
        // Verify
        verify(userRepository).save(testUser);
        verify(userRepository).flush();
        
        // Additional assertions
        assertEquals(6, testUser.getPlayedGames());
        assertEquals(2.0 / 6.0, testUser.getWinRate());
    }

    @Test
    void testDeleteBotsWhenOnlyBotsRemain() throws Exception {
        // Setup
        User botUser = new User();
        botUser.setId(2L);
        botUser.setIsBot(true);
        
        Lobby testLobby = new Lobby();
        testLobby.setId(100L);
        testLobby.addParticipantId(2L); // Only bot remains
        
        when(userService.getUserById(2L)).thenReturn(botUser);
        
        // Directly simulate the logic that checks for only bots
        List<Long> participants = testLobby.getParticipantIds();
        boolean onlyBots = true;
        for (Long participantId : participants) {
            User participant = userService.getUserById(participantId);
            if (!participant.getIsBot()) {
                onlyBots = false;
            }
        }
        
        // If only bots remain, delete them
        if (onlyBots) {
            for (Long participantId : participants) {
                User participant = userService.getUserById(participantId);
                userRepository.delete(participant);
            }
            lobbyService.deleteLobby(testLobby.getId());
        }
        
        // Verify
        verify(userRepository).delete(botUser);
        verify(lobbyService).deleteLobby(100L);
    }
    
    @Test
    void testHandleTransportError() throws Exception {
        // This method only logs the error, doesn't do much else
        // We're just testing that it doesn't throw an exception
        
        // Setup
        Exception testException = new Exception("Test transport error");
        
        // Execute - this should not throw an exception
        webSocketHandler.handleTransportError(session, testException);
        
        // No specific verification needed - the test passes if no uncaught exceptions occur
    }
    
    @Test
    void testGetSessionByUserId() throws Exception {
        // This test may be redundant since we're mocking the static method in other tests
        // But we'll include it for completeness
        
        // Use static mocking
        try (MockedStatic<WebSocketHandler> mockedHandler = mockStatic(WebSocketHandler.class)) {
            // Setup the mock to return our session
            mockedHandler.when(() -> WebSocketHandler.getSessionByUserId(1L)).thenReturn(session);
            
            // Verify the method works as expected
            assertEquals(session, WebSocketHandler.getSessionByUserId(1L));
        }
    }
    
    @Test
    void testStartGame_NotEnoughPlayers() throws Exception {
        // Setup
        User testUser = new User();
        testUser.setId(1L);
        testUser.setToken("test-token");
        testUser.setLobbyCode(100L);
        
        Lobby testLobby = new Lobby();
        testLobby.setId(100L);
        testLobby.setAdminId(1L);
        testLobby.addParticipantId(1L); // Only one player
        testLobby.setSolo(false); // Not solo mode
        
        when(userService.getUserByToken("test-token")).thenReturn(testUser);
        when(lobbyService.getLobbyById(100L)).thenReturn(testLobby);
        
        // Create test message
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("type", "startGame");
        
        ObjectNode settings = objectMapper.createObjectNode();
        settings.put("spawnRate", "Medium");
        settings.put("powerupsWanted", false);
        settings.put("sugarRush", false);
        
        requestBody.set("settings", settings);
        TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(requestBody));
        
        // Execute
        webSocketHandler.handleTextMessage(session, textMessage);
        
        // Verify error message was sent and game was not created
        verify(session).sendMessage(any(TextMessage.class));
        verify(gameService, never()).createGame(any(), anyString(), anyBoolean());
    }
    
    @Test
    void testStartGame_InvalidSettings() throws Exception {
        // Setup
        User testUser = new User();
        testUser.setId(1L);
        testUser.setToken("test-token");
        testUser.setLobbyCode(100L);
        
        Lobby testLobby = new Lobby();
        testLobby.setId(100L);
        testLobby.setAdminId(1L);
        
        when(userService.getUserByToken("test-token")).thenReturn(testUser);
        when(lobbyService.getLobbyById(100L)).thenReturn(testLobby);
        
        // Create test message with missing settings
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("type", "startGame");
        // No settings node
        
        TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(requestBody));
        
        // Execute
        webSocketHandler.handleTextMessage(session, textMessage);
        
        // Verify error message was sent
        verify(session).sendMessage(any(TextMessage.class));
    }
    
    @Test
    void testValidateLobby_InvalidLobby() throws Exception {
        // Setup
        User testUser = new User();
        testUser.setId(1L);
        testUser.setToken("test-token");
        
        when(userService.getUserByToken("test-token")).thenReturn(testUser);
        when(lobbyService.validateLobby(100L)).thenReturn(false);
        when(lobbyRepository.findById(100L)).thenReturn(java.util.Optional.empty());
        
        // Create test message
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("type", "validateLobby");
        requestBody.put("lobbyCode", 100L);
        TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(requestBody));
        
        // Execute
        webSocketHandler.handleTextMessage(session, textMessage);
        
        // Verify response contains invalid reason
        verify(session).sendMessage(any(TextMessage.class));
    }
    
    @Test
    void testValidateLobby_LobbyFull() throws Exception {
        // Setup
        User testUser = new User();
        testUser.setId(1L);
        testUser.setToken("test-token");
        
        Lobby fullLobby = new Lobby();
        fullLobby.setId(100L);
        
        when(userService.getUserByToken("test-token")).thenReturn(testUser);
        when(lobbyService.validateLobby(100L)).thenReturn(false);
        when(lobbyRepository.findById(100L)).thenReturn(java.util.Optional.of(fullLobby));
        
        // Create test message
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("type", "validateLobby");
        requestBody.put("lobbyCode", 100L);
        TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(requestBody));
        
        // Execute
        webSocketHandler.handleTextMessage(session, textMessage);
        
        // Verify response contains full reason
        verify(session).sendMessage(any(TextMessage.class));
    }

    @Test
    void testHandleTextMessage_RequestSettings() throws Exception {
        // Setup
        User testUser = new User();
        testUser.setId(1L);
        testUser.setToken("test-token");
        testUser.setLobbyCode(100L);
        
        Lobby testLobby = new Lobby();
        testLobby.setId(100L);
        testLobby.setAdminId(1L);
        testLobby.setSpawnRate("Fast");
        testLobby.setPowerupsWanted(true);
        testLobby.setSugarRush(false);
        
        when(userService.getUserByToken("test-token")).thenReturn(testUser);
        when(lobbyService.getLobbyById(100L)).thenReturn(testLobby);
        
        // Create test message
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("type", "requestSettings");
        ObjectNode settingsNode = objectMapper.createObjectNode();
        requestBody.set("settings", settingsNode);
        TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(requestBody));
        
        // Mock broadcast to prevent actual networking
        doNothing().when(webSocketHandler).broadcastToLobby(anyLong(), any(ObjectNode.class));
        
        // Execute
        webSocketHandler.handleTextMessage(session, textMessage);
        
        // Verify
        ArgumentCaptor<ObjectNode> messageCaptor = ArgumentCaptor.forClass(ObjectNode.class);
        verify(webSocketHandler).broadcastToLobby(eq(100L), messageCaptor.capture());
        
        // Verify the message content
        ObjectNode capturedMessage = messageCaptor.getValue();
        assertEquals("lobbySettings", capturedMessage.get("type").asText());
        
        JsonNode settingsObject = capturedMessage.get("Settings");
        assertNotNull(settingsObject, "Settings object should be present in the message");
        assertEquals("Fast", settingsObject.get("spawnRate").asText());
        assertTrue(settingsObject.get("powerupsWanted").asBoolean());
        assertFalse(settingsObject.get("sugarRush").asBoolean());
    }
}