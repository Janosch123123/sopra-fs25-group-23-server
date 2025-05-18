package ch.uzh.ifi.hase.soprafs24.handler;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs24.service.LobbyService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import ch.uzh.ifi.hase.soprafs24.service.GameService;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class WebSocketHandlerExceptionTest {

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
        
        Map<String, Object> attributes = new HashMap<>();
        when(session.getAttributes()).thenReturn(attributes);
    }
    
    @Test
    void testAfterConnectionEstablished_InvalidToken() throws Exception {
        // Setup - Token returns null user
        when(userRepository.findByToken("test-token")).thenReturn(null);
        
        // Execute
        webSocketHandler.afterConnectionEstablished(session);
        
        // Verify error message was sent and session closed
        verify(session).sendMessage(any(TextMessage.class));
        verify(session).close();
    }
    
    @Test
    void testAfterConnectionEstablished_Exception() throws Exception {
        // Setup - Throw exception when trying to find user
        when(userRepository.findByToken("test-token")).thenThrow(new RuntimeException("Database error"));
        
        // Execute
        webSocketHandler.afterConnectionEstablished(session);
        
        // Verify error handling
        verify(session).sendMessage(any(TextMessage.class));
        verify(session).close();
    }
    
    @Test
    void testHandleTextMessage_MissingType() throws Exception {
        // Create test message with no type
        ObjectNode requestBody = objectMapper.createObjectNode();
        TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(requestBody));
        
        // Execute
        webSocketHandler.handleTextMessage(session, textMessage);
        
        // Verify
        verify(session).sendMessage(any(TextMessage.class));
    }
    
    @Test
    void testHandleTextMessage_CreateLobby_NullUser() throws Exception {
        // Setup
        when(userService.getUserByToken("test-token")).thenReturn(null);
        
        // Create test message
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("type", "create_lobby");
        TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(requestBody));
        
        // Execute
        webSocketHandler.handleTextMessage(session, textMessage);
        
        // Verify error handling
        verify(session).sendMessage(any(TextMessage.class));
    }
    
    @Test
    void testHandleTextMessage_ValidateLobby_NullUser() throws Exception {
        // Setup
        when(userService.getUserByToken("test-token")).thenReturn(null);
        
        // Create test message
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("type", "validateLobby");
        requestBody.put("lobbyCode", 100);
        TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(requestBody));
        
        // Execute
        webSocketHandler.handleTextMessage(session, textMessage);
        
        // Verify error handling
        verify(session).sendMessage(any(TextMessage.class));
    }
    
    @Test
    void testHandleTextMessage_LobbySettings_NullUser() throws Exception {
        // Setup
        when(userService.getUserByToken("test-token")).thenReturn(null);
        
        // Create test message
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("type", "lobbySettings");
        ObjectNode settingsNode = objectMapper.createObjectNode();
        settingsNode.put("spawnRate", "Fast");
        requestBody.set("settings", settingsNode);
        TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(requestBody));
        
        // Execute
        webSocketHandler.handleTextMessage(session, textMessage);
        
        // Verify error handling
        verify(session).sendMessage(any(TextMessage.class));
    }
    
    @Test
    void testHandleTextMessage_StartGame_NullUser() throws Exception {
        // Setup
        when(userService.getUserByToken("test-token")).thenReturn(null);
        
        // Create test message
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("type", "startGame");
        ObjectNode settingsNode = objectMapper.createObjectNode();
        settingsNode.put("spawnRate", "Medium");
        settingsNode.put("sugarRush", false);
        settingsNode.put("powerupsWanted", false);
        requestBody.set("settings", settingsNode);
        TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(requestBody));
        
        // Execute
        webSocketHandler.handleTextMessage(session, textMessage);
        
        // Verify error handling
        verify(session).sendMessage(any(TextMessage.class));
    }
    
    @Test
    void testHandleTextMessage_LobbyState_NullUser() throws Exception {
        // Setup
        when(userService.getUserByToken("test-token")).thenReturn(null);
        
        // Create test message
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("type", "lobbystate");
        TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(requestBody));
        
        // Execute
        webSocketHandler.handleTextMessage(session, textMessage);
        
        // Verify error handling
        verify(session).sendMessage(any(TextMessage.class));
    }
    
    @Test
    void testHandleTextMessage_PlayerMove_NullUser() throws Exception {
        // Setup
        when(userService.getUserByToken("test-token")).thenReturn(null);
        
        // Create test message
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("type", "playerMove");
        requestBody.put("direction", "UP");
        TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(requestBody));
        
        // Execute
        webSocketHandler.handleTextMessage(session, textMessage);
        
        // Verify error handling
        verify(session).sendMessage(any(TextMessage.class));
    }
    
    @Test
    void testHandleTextMessage_QuickPlay_NullUser() throws Exception {
        // Setup
        when(userService.getUserByToken("test-token")).thenReturn(null);
        
        // Create test message
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("type", "quickPlay");
        TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(requestBody));
        
        // Execute
        webSocketHandler.handleTextMessage(session, textMessage);
        
        // Verify error handling
        verify(session).sendMessage(any(TextMessage.class));
    }
    
    @Test
    void testHandleTextMessage_SoloLobby_NullUser() throws Exception {
        // Setup
        when(userService.getUserByToken("test-token")).thenReturn(null);
        
        // Create test message
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("type", "soloLobby");
        TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(requestBody));
        
        // Execute
        webSocketHandler.handleTextMessage(session, textMessage);
        
        // Verify error handling
        verify(session).sendMessage(any(TextMessage.class));
    }
    
    @Test
    void testHandleTextMessage_InvalidJson() throws Exception {
        // Create invalid JSON message
        TextMessage textMessage = new TextMessage("{invalid json}");
        
        // Execute
        webSocketHandler.handleTextMessage(session, textMessage);
        
        // Verify error handling
        verify(session).sendMessage(any(TextMessage.class));
    }
}