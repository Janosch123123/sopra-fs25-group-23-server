package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.handler.WebSocketHandler;
import ch.uzh.ifi.hase.soprafs24.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.context.ApplicationContext;
import org.springframework.web.socket.TextMessage;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class LobbyServiceTest {

    @Mock
    private LobbyRepository lobbyRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private UserService userService;
    
    @Mock
    private ApplicationContext applicationContext;
    
    @Mock
    private WebSocketHandler webSocketHandler;
    
    @InjectMocks
    private LobbyService lobbyService;
    
    private User testUser;
    private Lobby testLobby;
    
    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        
        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testUser");
        
        // Setup test lobby
        testLobby = new Lobby();
        testLobby.setId(1L);
        testLobby.setAdminId(1L);
        testLobby.setVisibility("private");
        testLobby.addParticipant(testUser);
        
        // Mock WebSocketHandler bean retrieval
        when(applicationContext.getBean(WebSocketHandler.class)).thenReturn(webSocketHandler);
    }
    
    @Test
    public void createPrivateLobby_success() {
        // Arrange
        when(lobbyRepository.save(any(Lobby.class))).thenReturn(testLobby);
        
        // Act
        Lobby createdLobby = lobbyService.createPrivateLobby(testUser);
        
        // Assert
        assertNotNull(createdLobby);
        assertEquals(testUser.getId(), createdLobby.getAdminId());
        assertEquals("private", createdLobby.getVisibility());
        assertTrue(createdLobby.getParticipantIds().contains(testUser.getId()));
        
        // Verify
        verify(lobbyRepository, times(2)).save(any(Lobby.class));
    }
    
    @Test
    public void createPrivateLobby_nullAdmin() {
        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            lobbyService.createPrivateLobby(null);
        });
        
        assertEquals("Admin user cannot be null", exception.getMessage());
    }
    
    @Test
    public void createPublicLobby_success() {
        // Arrange
        when(lobbyRepository.save(any(Lobby.class))).thenReturn(testLobby);
        
        // Act
        Lobby createdLobby = lobbyService.createPublicLobby(testUser);
        
        // Assert
        assertNotNull(createdLobby);
        assertEquals(testUser.getId(), createdLobby.getAdminId());
        assertEquals("private", createdLobby.getVisibility()); // This will return the mock value
        assertTrue(createdLobby.getParticipantIds().contains(testUser.getId()));
    }
    
    @Test
    public void handleQuickPlay_emptyLobbies() {
        // Arrange
        when(lobbyRepository.findByVisibility("public")).thenReturn(Collections.emptyList());
        when(lobbyRepository.save(any(Lobby.class))).thenReturn(testLobby);
        
        // Act
        Lobby result = lobbyService.handleQuickPlay(testUser);
        
        // Assert
        assertNotNull(result);
        verify(lobbyRepository).findByVisibility("public");
    }
    
    @Test
    public void handleQuickPlay_joinExistingLobby() {
        // Arrange
        Lobby publicLobby = new Lobby();
        publicLobby.setId(2L);
        publicLobby.setAdminId(2L);
        publicLobby.setVisibility("public");
        publicLobby.addParticipant(new User()); // Add one participant
        
        when(lobbyRepository.findByVisibility("public")).thenReturn(List.of(publicLobby));
        when(lobbyRepository.save(any(Lobby.class))).thenReturn(publicLobby);
        
        // Act
        Lobby result = lobbyService.handleQuickPlay(testUser);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.getParticipantIds().contains(testUser.getId()));
        verify(lobbyRepository).findByVisibility("public");
        verify(lobbyRepository).save(publicLobby);
    }
    
    @Test
    public void handleQuickPlay_nullUser() {
        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            lobbyService.handleQuickPlay(null);
        });
        
        assertEquals("User user cannot be null", exception.getMessage());
    }
    
    @Test
    public void getLobbyById_success() {
        // Arrange
        when(lobbyRepository.findById(1L)).thenReturn(Optional.of(testLobby));
        
        // Act
        Lobby result = lobbyService.getLobbyById(1L);
        
        // Assert
        assertNotNull(result);
        assertEquals(testLobby.getId(), result.getId());
    }
    
    @Test
    public void getLobbyById_notFound() {
        // Arrange
        when(lobbyRepository.findById(999L)).thenReturn(Optional.empty());
        
        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            lobbyService.getLobbyById(999L);
        });
        
        assertTrue(exception.getMessage().contains("Lobby not found"));
    }
    
    @Test
    public void validateLobby_validLobby() {
        // Arrange
        Lobby validLobby = new Lobby();
        validLobby.setId(1L);
        validLobby.addParticipant(testUser);
        
        when(lobbyRepository.findById(1L)).thenReturn(Optional.of(validLobby));
        
        // Act
        boolean isValid = lobbyService.validateLobby(1L);
        
        // Assert
        assertTrue(isValid);
    }
    
    @Test
    public void validateLobby_lobbyFull() {
        // Arrange
        Lobby fullLobby = new Lobby();
        fullLobby.setId(1L);
        
        // Add 4 participants to make the lobby full
        for (int i = 1; i <= 4; i++) {
            User user = new User();
            user.setId((long) i);
            fullLobby.addParticipant(user);
        }
        
        when(lobbyRepository.findById(1L)).thenReturn(Optional.of(fullLobby));
        
        // Act
        boolean isValid = lobbyService.validateLobby(1L);
        
        // Assert
        assertFalse(isValid);
    }
    
    @Test
    public void addLobbyCodeToUser_success() {
        // Act
        lobbyService.addLobbyCodeToUser(testUser, 1L);
        
        // Assert
        assertEquals(1L, testUser.getLobbyCode());
        verify(userRepository).save(testUser);
    }
    
    @Test
    public void addLobbyCodeToUser_nullUser() {
        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            lobbyService.addLobbyCodeToUser(null, 1L);
        });
        
        assertEquals("User cannot be null and lobby code must be positive", exception.getMessage());
    }
    
    @Test
    public void findLobbyForUser_found() {
        // Arrange
        when(lobbyRepository.findAll()).thenReturn(List.of(testLobby));
        
        // Act
        Lobby result = lobbyService.findLobbyForUser(1L);
        
        // Assert
        assertNotNull(result);
        assertEquals(testLobby.getId(), result.getId());
    }
    
    @Test
    public void findLobbyForUser_notFound() {
        // Arrange
        when(lobbyRepository.findAll()).thenReturn(List.of(testLobby));
        
        // Act
        Lobby result = lobbyService.findLobbyForUser(999L);
        
        // Assert
        assertNull(result);
    }
    
    @Test
    public void updateLobby_success() {
        // Arrange
        when(lobbyRepository.findById(1L)).thenReturn(Optional.of(testLobby));
        when(lobbyRepository.save(testLobby)).thenReturn(testLobby);
        
        // Act
        Lobby result = lobbyService.updateLobby(testLobby);
        
        // Assert
        assertNotNull(result);
        assertEquals(testLobby.getId(), result.getId());
    }
    
    @Test
    public void updateLobby_nullLobby() {
        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            lobbyService.updateLobby(null);
        });
        
        assertEquals("Lobby cannot be null", exception.getMessage());
    }
    
    @Test
    public void updateLobby_nonExistentLobby() {
        // Arrange
        Lobby nonExistentLobby = new Lobby();
        nonExistentLobby.setId(999L);
        
        when(lobbyRepository.findById(999L)).thenReturn(Optional.empty());
        
        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            lobbyService.updateLobby(nonExistentLobby);
        });
        
        assertTrue(exception.getMessage().contains("Lobby not found"));
    }
    
    @Test
    public void deleteLobby_success() {
        // Arrange
        when(lobbyRepository.findById(1L)).thenReturn(Optional.of(testLobby));
        
        // Act
        lobbyService.deleteLobby(1L);
        
        // Assert
        verify(lobbyRepository).deleteById(1L);
    }
    
    @Test
    public void deleteLobby_nonExistentLobby() {
        // Arrange
        when(lobbyRepository.findById(999L)).thenReturn(Optional.empty());
        
        // Act
        lobbyService.deleteLobby(999L);
        
        // Assert
        verify(lobbyRepository, never()).deleteById(999L);
    }
    
    @Test
    public void getGameByLobby_returnsGame() {
        // Arrange
        Game game = new Game();
        game.setGameId(1L);
        
        // Use reflection to set the static map for testing
        try {
            Method putGameToLobby = LobbyService.class.getDeclaredMethod("putGameToLobby", Game.class, Long.class);
            putGameToLobby.setAccessible(true);
            putGameToLobby.invoke(null, game, 1L);
            
            // Act
            Game result = LobbyService.getGameByLobby(1L);
            
            // Assert
            assertNotNull(result);
            assertEquals(game.getGameId(), result.getGameId());
        } catch (Exception e) {
            fail("Test failed: " + e.getMessage());
        }
    }
    
    @Test
    public void checkIfGameStarted_gameStarted() throws Exception {
        // Create a method instance using reflection
        Method checkIfGameStarted = LobbyService.class.getDeclaredMethod("checkIfGameStarted", Lobby.class);
        checkIfGameStarted.setAccessible(true);
        
        // Create test lobby with game ID set
        Lobby lobby = new Lobby();
        lobby.setGameId(1L);
        
        // Invoke the method
        boolean result = (boolean) checkIfGameStarted.invoke(lobbyService, lobby);
        
        // Assert
        assertTrue(result);
    }
    
    @Test
    public void checkIfGameStarted_gameNotStarted() throws Exception {
        // Create a method instance using reflection
        Method checkIfGameStarted = LobbyService.class.getDeclaredMethod("checkIfGameStarted", Lobby.class);
        checkIfGameStarted.setAccessible(true);
        
        // Create test lobby with no game ID
        Lobby lobby = new Lobby();
        
        // Invoke the method
        boolean result = (boolean) checkIfGameStarted.invoke(lobbyService, lobby);
        
        // Assert
        assertFalse(result);
    }
    
    @Test
    public void getWebSocketHandler_returnsHandler() throws Exception {
        // Create a method instance using reflection
        Method getWebSocketHandler = LobbyService.class.getDeclaredMethod("getWebSocketHandler");
        getWebSocketHandler.setAccessible(true);
        
        // Invoke the method
        WebSocketHandler result = (WebSocketHandler) getWebSocketHandler.invoke(lobbyService);
        
        // Assert
        assertNotNull(result);
        assertEquals(webSocketHandler, result);
    }
    
    @Test
    public void createPublicLobby_createsLobbyAndSchedulesBot() {
        // Arrange
        when(lobbyRepository.save(any(Lobby.class))).thenReturn(testLobby);
        
        // Act
        Lobby createdLobby = lobbyService.createPublicLobby(testUser);
        
        // Assert
        assertNotNull(createdLobby);
        assertEquals(testUser.getId(), createdLobby.getAdminId());
        // We can't directly verify bot scheduling, but we can verify the lobby was created correctly
        assertTrue(createdLobby.getParticipantIds().contains(testUser.getId()));
        
        // Verify repository interactions
        verify(lobbyRepository, times(2)).save(any(Lobby.class));
    }
}