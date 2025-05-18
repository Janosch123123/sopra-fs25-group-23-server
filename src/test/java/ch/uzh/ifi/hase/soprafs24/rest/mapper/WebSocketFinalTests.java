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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.mockStatic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class WebSocketFinalTests {

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
        when(session.isOpen()).thenReturn(true);
        
        // Mock session message sending
        doAnswer(invocation -> {
            TextMessage msg = invocation.getArgument(0);
            System.out.println("Message sent: " + msg.getPayload());
            return null;
        }).when(session).sendMessage(any(TextMessage.class));
        
        Map<String, Object> attributes = new HashMap<>();
        when(session.getAttributes()).thenReturn(attributes);
    }

    // Test case for lines 95-97: Connection error handling
    @Test
    void testAfterConnectionEstablishedWithError() throws Exception {
        // Setup to trigger exception
        when(userRepository.findByToken("test-token")).thenThrow(new RuntimeException("Database error"));
        
        // Execute
        webSocketHandler.afterConnectionEstablished(session);
        
        // Verify error message was sent and session was closed
        verify(session).sendMessage(any(TextMessage.class));
        verify(session).close();
    }

    // Test case for lines 109-110: broadcast to non-existent lobby
    @Test
    void testBroadcastToNonExistentLobby() throws IOException {
        // Setup
        long nonExistentLobbyCode = 999L;
        when(lobbyService.getLobbyById(nonExistentLobbyCode)).thenReturn(null);
        
        // Create test message
        ObjectNode updateMessage = objectMapper.createObjectNode();
        updateMessage.put("type", "test_update");
        
        // Execute
        webSocketHandler.broadcastToLobby(nonExistentLobbyCode, updateMessage);
        
        // Verify that no messages were sent since lobby doesn't exist
        verify(session, never()).sendMessage(any(TextMessage.class));
    }

    // Test case for lines 157-160: Create lobby exception handling
    @Test
    void testHandleTextMessage_CreateLobbyError() throws Exception {
        // Setup
        User testUser = new User();
        testUser.setId(1L);
        testUser.setToken("test-token");
        
        when(userService.getUserByToken("test-token")).thenReturn(testUser);
        when(lobbyService.createPrivateLobby(testUser)).thenThrow(new RuntimeException("Lobby creation failed"));
        
        // Create test message
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("type", "create_lobby");
        TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(requestBody));
        
        // Execute
        webSocketHandler.handleTextMessage(session, textMessage);
        
        // Verify error response
        verify(session).sendMessage(any(TextMessage.class));
    }

    // Test case for lines 186-195: validateLobby - lobby already full
    @Test
    void testHandleTextMessage_ValidateLobbyAlreadyFull() throws Exception {
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
        requestBody.put("lobbyCode", 100);
        TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(requestBody));
        
        // Execute
        webSocketHandler.handleTextMessage(session, textMessage);
        
        // Verify response contains correct reason
        verify(session).sendMessage(argThat(message -> {
            try {
                ObjectNode response = (ObjectNode) objectMapper.readTree(message.getPayload().toString());
                return response.has("reason") && 
                       response.get("reason").asText().equals("lobby is already full or is set to solo");
            } catch (Exception e) {
                return false;
            }
        }));
    }

    // Test case for lines 199-201: validateLobby - invalid lobby
    @Test
    void testHandleTextMessage_ValidateLobbyInvalid() throws Exception {
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
        requestBody.put("lobbyCode", 100);
        TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(requestBody));
        
        // Execute
        webSocketHandler.handleTextMessage(session, textMessage);
        
        // Verify response contains correct reason
        verify(session).sendMessage(argThat(message -> {
            try {
                ObjectNode response = (ObjectNode) objectMapper.readTree(message.getPayload().toString());
                return response.has("reason") && 
                       response.get("reason").asText().equals("invalid");
            } catch (Exception e) {
                return false;
            }
        }));
    }

    // Test case for lines 241-243: lobbySettings changes exception
    @Test
    void testHandleTextMessage_LobbySettingsException() throws Exception {
        // Setup
        User testUser = new User();
        testUser.setId(1L);
        testUser.setToken("test-token");
        testUser.setLobbyCode(100L);
        
        Lobby testLobby = new Lobby();
        testLobby.setId(100L);
        
        when(userService.getUserByToken("test-token")).thenReturn(testUser);
        when(lobbyService.getLobbyById(100L)).thenReturn(testLobby);
        doThrow(new RuntimeException("Settings update error")).when(lobbyService).getLobbyById(100L);
        
        // Create test message
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("type", "lobbySettings");
        ObjectNode settings = objectMapper.createObjectNode();
        settings.put("spawnRate", "High");
        settings.put("powerupsWanted", true);
        requestBody.set("settings", settings);
        TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(requestBody));
        
        // Execute
        webSocketHandler.handleTextMessage(session, textMessage);
        
        // Verify error message
        verify(session).sendMessage(argThat(message -> {
            try {
                ObjectNode response = (ObjectNode) objectMapper.readTree(message.getPayload().toString());
                return "error".equals(response.get("type").asText());
            } catch (Exception e) {
                return false;
            }
        }));
    }

    // Test case for lines 263-264: startGame not enough players
    @Test
    void testHandleTextMessage_StartGameNotEnoughPlayers() throws Exception {
        // Setup
        User testUser = new User();
        testUser.setId(1L);
        testUser.setToken("test-token");
        testUser.setLobbyCode(100L);
        
        Lobby testLobby = new Lobby();
        testLobby.setId(100L);
        testLobby.setAdminId(1L);
        testLobby.addParticipantId(1L);
        testLobby.setSolo(false);
        
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
        
        // Verify error message
        verify(session).sendMessage(argThat(message -> {
            try {
                ObjectNode response = (ObjectNode) objectMapper.readTree(message.getPayload().toString());
                return "error".equals(response.get("type").asText()) && 
                       response.get("message").asText().contains("Not enough players");
            } catch (Exception e) {
                return false;
            }
        }));
    }

    // Test case for lines 296-298: playerMove game not found
    @Test
    void testHandleTextMessage_PlayerMoveGameNotFound() throws Exception {
        // Setup
        User testUser = new User();
        testUser.setId(1L);
        testUser.setToken("test-token");
        testUser.setLobbyCode(100L);
        
        Lobby testLobby = new Lobby();
        testLobby.setId(100L);
        
        when(userService.getUserByToken("test-token")).thenReturn(testUser);
        when(lobbyService.getLobbyById(100L)).thenReturn(testLobby);
        
        // Create test message
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("type", "playerMove");
        requestBody.put("direction", "UP");
        TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(requestBody));
        
        try (MockedStatic<LobbyService> mockedLobbyService = mockStatic(LobbyService.class)) {
            // Return null to simulate game not found
            mockedLobbyService.when(() -> LobbyService.getGameByLobby(100L)).thenReturn(null);
            
            // Execute
            webSocketHandler.handleTextMessage(session, textMessage);
            
            // Verify error message
            verify(session).sendMessage(argThat(message -> {
                try {
                    ObjectNode response = (ObjectNode) objectMapper.readTree(message.getPayload().toString());
                    return "error".equals(response.get("type").asText()) && 
                           response.get("message").asText().contains("Game not found");
                } catch (Exception e) {
                    return false;
                }
            }));
        }
    }

    // Test case for lines 338-340: quickPlay exception
    @Test
    void testHandleTextMessage_QuickPlayException() throws Exception {
        // Setup
        User testUser = new User();
        testUser.setId(1L);
        testUser.setToken("test-token");
        
        when(userService.getUserByToken("test-token")).thenReturn(testUser);
        when(lobbyService.handleQuickPlay(testUser)).thenThrow(new RuntimeException("QuickPlay error"));
        
        // Create test message
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("type", "quickPlay");
        TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(requestBody));
        
        // Execute
        webSocketHandler.handleTextMessage(session, textMessage);
        
        // Verify error message
        verify(session).sendMessage(argThat(message -> {
            try {
                ObjectNode response = (ObjectNode) objectMapper.readTree(message.getPayload().toString());
                return "error".equals(response.get("type").asText()) && 
                       response.get("message").asText().contains("Failed to create lobby");
            } catch (Exception e) {
                return false;
            }
        }));
    }

    // Test case for lines 363-365: soloLobby exception
    @Test
    void testHandleTextMessage_SoloLobbyException() throws Exception {
        // Setup
        User testUser = new User();
        testUser.setId(1L);
        testUser.setToken("test-token");
        
        when(userService.getUserByToken("test-token")).thenReturn(testUser);
        when(lobbyService.createPrivateLobby(testUser)).thenThrow(new RuntimeException("Solo lobby error"));
        
        // Create test message
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("type", "soloLobby");
        TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(requestBody));
        
        // Execute
        webSocketHandler.handleTextMessage(session, textMessage);
        
        // Verify error message
        verify(session).sendMessage(argThat(message -> {
            try {
                ObjectNode response = (ObjectNode) objectMapper.readTree(message.getPayload().toString());
                return "error".equals(response.get("type").asText()) && 
                       response.get("message").asText().contains("Failed to create lobby");
            } catch (Exception e) {
                return false;
            }
        }));
    }

    // Test case for lines 398-400: Unknown message type
    @Test
    void testHandleTextMessage_UnknownMessageType() throws Exception {
        // Create test message with unknown type
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("type", "nonexistent_command");
        TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(requestBody));
        
        // Execute
        webSocketHandler.handleTextMessage(session, textMessage);
        
        // Verify error message
        verify(session).sendMessage(argThat(message -> {
            try {
                ObjectNode response = (ObjectNode) objectMapper.readTree(message.getPayload().toString());
                return "error".equals(response.get("type").asText()) && 
                       response.get("message").asText().contains("Unknown message type");
            } catch (Exception e) {
                return false;
            }
        }));
    }
}