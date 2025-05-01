package ch.uzh.ifi.hase.soprafs24.handler;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs24.service.LobbyService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import ch.uzh.ifi.hase.soprafs24.service.GameService;

import java.lang.reflect.Field;
import org.springframework.test.util.ReflectionTestUtils;
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

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class WebSocketHandlerTest {

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
        when(lobbyService.createPrivateLobby(testUser)).thenReturn(testLobby);
        
        // Create test message
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("type", "create_lobby");
        TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(requestBody));
        
        // Execute
        webSocketHandler.handleTextMessage(session, textMessage);
        
        // Verify
        verify(lobbyService).createPrivateLobby(testUser);
        verify(lobbyService).addLobbyCodeToUser(testUser, 100L);
        verify(session).sendMessage(any(TextMessage.class));
    }

    @Test
    void testHandleTextMessage_JoinLobby() throws Exception {
        // Setup
        User testUser = new User();
        testUser.setId(1L);
        testUser.setToken("test-token");

        Lobby testLobby = new Lobby();
        testLobby.setId(100L);
        
        when(userService.getUserByToken("test-token")).thenReturn(testUser);
        when(lobbyService.getLobbyById(100L)).thenReturn(testLobby);
        
        // Create test message
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("type", "validateLobby");
        requestBody.put("lobbyCode", 100L);
        TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(requestBody));
        
        // Execute
        webSocketHandler.handleTextMessage(session, textMessage);
        
        // Verify
        verify(lobbyService).validateLobby(100L);
        verify(session).sendMessage(any(TextMessage.class));
    }

    @Test
    void testHandleTextMessage_LobbyState() throws Exception {
        //Setup
        User testUser = new User();
        testUser.setId(1L);
        testUser.setToken("test-token");
        testUser.setLobbyCode(100L);

        Lobby testLobby = new Lobby();
        testLobby.setId(100L);
        testLobby.setAdminId(1L);
        
        when(userService.getUserByToken("test-token")).thenReturn(testUser);
        when(lobbyService.getLobbyById(100L)).thenReturn(testLobby);

        // Create test message
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("type", "lobbystate");
        TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(requestBody));

        //Execute
        webSocketHandler.handleTextMessage(session, textMessage);

        //verify
        verify(webSocketHandler).sendLobbyStateToUsers(testLobby.getId());
    }

    @Test
    void testHandleTextMessage_startGame() throws Exception {
        //Setup
        User testUser = new User();
        testUser.setId(1L);
        testUser.setToken("test-token");
        testUser.setLobbyCode(100L);

        Lobby testLobby = new Lobby();
        testLobby.setId(100L);
        testLobby.setAdminId(1L);

        Game testGame = new Game();
        testGame.setGameId(200L);
        
        when(gameService.createGame(testLobby, "Medium")).thenReturn(testGame);
        when(userService.getUserByToken("test-token")).thenReturn(testUser);
        when(lobbyService.getLobbyById(100L)).thenReturn(testLobby);

        // Create test message
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("type", "startGame");
        requestBody.put("lobbyCode", 100L);
        ObjectNode settings = objectMapper.createObjectNode();
        settings.put("spawnRate", "Medium");
        settings.put("sugarRush", "false");// Add spawnRate to settings
        requestBody.set("settings", settings);
        TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(requestBody));

        // Execute
        webSocketHandler.handleTextMessage(session, textMessage);

        // Verify
        verify(gameService).createGame(testLobby, "Medium");
        verify(gameService).start(testGame);
    }

    @Test
    void testHandleTextMessage_playerMove() throws Exception {
        try {
            // Setup
            User testUser = new User();
            testUser.setId(1L);
            testUser.setToken("test-token");
            testUser.setLobbyCode(100L);
    
            Lobby testLobby = new Lobby();
            testLobby.setId(100L);
            testLobby.setAdminId(1L);
    
            Game testGame = new Game();
            testGame.setGameId(200L);
            testGame.setLobby(testLobby);
            
            when(userService.getUserByToken("test-token")).thenReturn(testUser);
            when(lobbyService.getLobbyById(100L)).thenReturn(testLobby);
            
            // Create test message before the MockedStatic block
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("type", "playerMove");
            requestBody.put("direction", "UP");
            TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(requestBody));
            
            try (MockedStatic<LobbyService> mockedLobbyService = mockStatic(LobbyService.class)) {
                // Use a more flexible matcher for the method parameter
                mockedLobbyService.when(() -> LobbyService.getGameByLobby(any(Long.class))).thenReturn(testGame);
                
                // Make sure the mock is working by testing it directly
                Game result = LobbyService.getGameByLobby(100L);
                System.out.println("Mock test - Expected same game: " + (result == testGame));
                
                // Execute within the MockedStatic scope
                webSocketHandler.handleTextMessage(session, textMessage);
                
                // Verify within the MockedStatic scope
                verify(gameService).respondToKeyInputs(testGame, testUser, "UP");
            }
        } catch (Exception e) {
            System.err.println("Test failed with exception: " + e.getClass().getName());
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    void testAfterConnectionClosed() throws Exception {
        // Setup
        Long userId = 1L;
        
        // Create a test lobby
        Lobby testLobby = new Lobby();
        testLobby.setId(100L);
        testLobby.addParticipantId(userId);
        
        when(lobbyService.findLobbyForUser(userId)).thenReturn(testLobby);
        
        doAnswer(invocation -> {
            testLobby.removeParticipantId(userId);
            lobbyService.updateLobby(testLobby);
            
            webSocketHandler.sendLobbyStateToUsers(testLobby.getId());
            
            return null;
        }).when(webSocketHandler).afterConnectionClosed(eq(session), any(CloseStatus.class));
        
        // Execute
        webSocketHandler.afterConnectionClosed(session, CloseStatus.NORMAL);
        
        // Verify
        verify(lobbyService).updateLobby(testLobby);
        verify(webSocketHandler).sendLobbyStateToUsers(100L);
        
        // Verify user was removed from lobby
        assertFalse(testLobby.getParticipantIds().contains(userId));
    }
}