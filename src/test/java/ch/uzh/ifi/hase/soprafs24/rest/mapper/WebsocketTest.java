package ch.uzh.ifi.hase.soprafs24.handler;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
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
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class WebSocketHandlerTest {

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
    }
    
    @Test
    void testAfterConnectionEstablished() throws Exception {
        // Setup
        User testUser = new User();
        testUser.setId(1L);
        testUser.setToken("test-token");
        
        when(userRepository.findByToken("test-token")).thenReturn(testUser);
        
        // Execute
        webSocketHandler.afterConnectionEstablished(session);
        
        // Verify
        verify(session).sendMessage(any(TextMessage.class));
        // Additional assertions could check exact message content
    }
    
    @Test
    void testHandleTextMessage_CreateLobby() throws Exception {
        // Setup
        User testUser = new User();
        testUser.setId(1L);
        testUser.setToken("test-token");
        
        Lobby testLobby = new Lobby();
        testLobby.setId(100L);
        testLobby.setAdminId(1L);
        
        when(userService.getUserByToken("test-token")).thenReturn(testUser);
        when(lobbyService.createLobby(testUser)).thenReturn(testLobby);
        
        // Create test message
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("type", "create_lobby");
        TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(requestBody));
        
        // Execute
        webSocketHandler.handleTextMessage(session, textMessage);
        
        // Verify
        verify(lobbyService).createLobby(testUser);
        verify(lobbyService).addLobbyCodeToUser(testUser, 100L);
        verify(session).sendMessage(any(TextMessage.class));
    }
    
    @Test
    void testAfterConnectionClosed() throws Exception {
        // This function does nothing yet, but once it does, we can test it :D
        
        webSocketHandler.afterConnectionClosed(session, CloseStatus.NORMAL); 
    }
}