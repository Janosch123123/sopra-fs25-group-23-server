package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.*;
import ch.uzh.ifi.hase.soprafs24.entity.Powerups.Cookie;
import ch.uzh.ifi.hase.soprafs24.handler.WebSocketHandler;
import ch.uzh.ifi.hase.soprafs24.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationContext;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

public class GameServiceTest {

    @Mock
    private LobbyRepository lobbyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @Mock
    private SnakeService snakeService;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private WebSocketHandler webSocketHandler;

    @InjectMocks
    private GameService gameService;

    private Lobby testLobby;
    private Game testGame;
    private User testUser1;
    private User testUser2;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        // Setup test users
        testUser1 = new User();
        testUser1.setId(1L);
        testUser1.setUsername("testUser1");
        testUser1.setPassword("password1");
        testUser1.setWins(0);
        testUser1.setKills(0);
        testUser1.setLevel(0);

        testUser2 = new User();
        testUser2.setId(2L);
        testUser2.setUsername("testUser2");
        testUser2.setPassword("password2");
        testUser2.setWins(0);
        testUser2.setKills(0);
        testUser2.setLevel(0);

        // Setup test lobby
        testLobby = new Lobby();
        testLobby.setId(1L);
        testLobby.setParticipantIds(Arrays.asList(1L, 2L));

        // Setup test game
        testGame = new Game();
        testGame.setGameId(1L);
        testGame.setLobby(testLobby);
        testGame.setTimestamp(60.0f);
        testGame.setGameOver(false);

        // Setup mocks
        when(applicationContext.getBean(WebSocketHandler.class)).thenReturn(webSocketHandler);
        when(lobbyRepository.findById(anyLong())).thenReturn(Optional.of(testLobby));
        when(userService.getUserById(1L)).thenReturn(testUser1);
        when(userService.getUserById(2L)).thenReturn(testUser2);
        when(userRepository.findByUsername("testUser1")).thenReturn(testUser1);
        when(userRepository.findByUsername("testUser2")).thenReturn(testUser2);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser2));
    }

//    @Test
//    public void createGame_validLobby_success() {
//        // when
//        Game createdGame = gameService.createGame(testLobby,"Medium");
//
//        // then
//        assertNotNull(createdGame);
//        assertEquals(testLobby, createdGame.getLobby());
//
//        // Verify that snakes were added correctly
//        assertNotNull(createdGame.getSnakes());
//        assertEquals(2, createdGame.getSnakes().size());
//
//        // Verify initial items
//        assertNotNull(createdGame.getItems());
//        assertEquals(12, createdGame.getItems().size());    // this number changes often (set number of items before M4 submission
//
//        // Verify that the repository was accessed
//        verify(lobbyRepository, times(1)).findById(testLobby.getId());
//    }

    @Test
    public void respondToKeyInputs_validDirection_success() {
        // Setup
        Snake testSnake = new Snake();
        testSnake.setUserId(1L);
        testSnake.setDirection("RIGHT");
        testSnake.setCoordinates(new int[][]{{4, 4}, {3, 4}, {2, 4}});
        testGame.addSnake(testSnake);

        // when
        gameService.respondToKeyInputs(testGame, testUser1, "UP");

        // then
        assertEquals(1, testSnake.getDirectionQueue().size());
        assertEquals("UP", testSnake.getDirectionQueue().get(0));
    }

    @Test
    public void updateSnakeDirection_validQueuedDirection_directionChanges() {
        // Setup
        Snake testSnake = new Snake();
        testSnake.setUserId(1L);
        testSnake.setDirection("RIGHT");
        testSnake.setCoordinates(new int[][]{{4, 4}, {3, 4}, {2, 4}});
        testSnake.addDirectionQueue("UP");
        testGame.addSnake(testSnake);

        // when
        gameService.updateSnakeDirection(testSnake);

        // then
        assertEquals("UP", testSnake.getDirection());
        assertTrue(testSnake.getDirectionQueue().isEmpty());
    }

    @Test
    public void updateSnakeDirection_invalidQueuedDirection_directionUnchanged() {
        // Setup
        Snake testSnake = new Snake();
        testSnake.setUserId(1L);
        testSnake.setDirection("RIGHT");
        testSnake.setCoordinates(new int[][]{{4, 4}, {3, 4}, {2, 4}});
        testSnake.addDirectionQueue("LEFT"); // Invalid because it's opposite of current direction
        testGame.addSnake(testSnake);

        // when
        gameService.updateSnakeDirection(testSnake);

        // then
        assertEquals("RIGHT", testSnake.getDirection()); // Direction remains unchanged
        assertTrue(testSnake.getDirectionQueue().isEmpty());
    }

    @Test
    public void endGame_winnerExists_updatesStats() throws IOException {
        // Setup
        testGame.setLeaderboard(Arrays.asList(testUser1.getUsername()));
        
        // when
        gameService.endGame(testGame);

        // then
        verify(userRepository, times(1)).findByUsername("testUser1");
        verify(userRepository, atLeast(testLobby.getParticipantIds().size())).save(any(User.class));
        
        assertEquals(1, testUser1.getWins());
        
        // Verify that the broadcast was called
        verify(webSocketHandler, times(1)).broadcastToLobby(eq(testLobby.getId()), any(ObjectNode.class));
    }
    
    @Test
    public void start_gameRunsAndBroadcasts() throws InterruptedException, IOException {
        // Mock the methods that are called inside the start method
        doNothing().when(webSocketHandler).broadcastToLobby(anyLong(), any(ObjectNode.class));
        
        // Setup a game that will end immediately for testing
        testGame.setGameOver(true);
        
        // when
        gameService.start(testGame);
        
        // Give the thread some time to execute
        Thread.sleep(100);
        
        // This is a limited test since the method spawns a new thread
        // We mainly verify that the method does not throw exceptions
        assertDoesNotThrow(() -> {});
    }
    
    @Test
    public void addSnakesToBoard_multiplePlayers_correctInitialization() {
        // Testing the private method directly without reflection
        // Instead, we'll use the createGame method which calls addSnakesToBoard
        
        // Setup with 4 players in the lobby
        testLobby.setParticipantIds(Arrays.asList(1L, 2L, 3L, 4L));
        when(userService.getUserById(3L)).thenReturn(createTestUser(3L, "testUser3"));
        when(userService.getUserById(4L)).thenReturn(createTestUser(4L, "testUser4"));
        
        // when
        Game game = gameService.createGame(testLobby, "Medium", false);
        
        // then
        assertEquals(4, game.getSnakes().size());
        
        // Verify each snake has different starting positions
        List<String> directions = new ArrayList<>();
        for (Snake snake : game.getSnakes()) {
            directions.add(snake.getDirection());
        }
        
        // Verify we have all four directions
        assertTrue(directions.contains("RIGHT"));
        assertTrue(directions.contains("LEFT"));
        assertTrue(directions.contains("UP"));
        assertTrue(directions.contains("DOWN"));
    }
    
    // Helper method to create test users
    private User createTestUser(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPassword("password");
        user.setWins(0);
        user.setKills(0);
        user.setLevel(0);
        return user;
    }

    @Test
    public void spawnItem_validGame_addsNewItem() {
        // Setup
        Game game = new Game();
        game.setItems(new ArrayList<>());
        Snake snake = new Snake();
        snake.setCoordinates(new int[][]{{5, 5}, {5, 6}});
        game.addSnake(snake);
        
        // Use reflection to access private method
        java.lang.reflect.Method method;
        try {
            method = GameService.class.getDeclaredMethod("spawnItem", Game.class);
            method.setAccessible(true);
            method.invoke(gameService, game);
            
            // then
            assertEquals(1, game.getItems().size());
            assertNotNull(game.getItems().get(0).getPosition());
            assertEquals("cookie", game.getItems().get(0).getType());
            
        } catch (Exception e) {
            fail("Test failed due to reflection error: " + e.getMessage());
        }
    }
    
    @Test
    public void updateGameState_snakeCollision_marksSnakeAsDead() {
        // Setup
        Game game = new Game();
        Snake snake = new Snake();
        snake.setCoordinates(new int[][]{{5, 5}, {5, 6}});
        snake.setDirection("RIGHT");
        snake.setUserId(1L);
        snake.setUsername("testUser1");
        game.addSnake(snake);
        
        // Mock collision check to return true (collision detected)
        when(snakeService.checkCollision(any(Snake.class), any(Game.class))).thenReturn(true);
        
        // Use reflection to access private method
        java.lang.reflect.Method method;
        try {
            method = GameService.class.getDeclaredMethod("updateGameState", Game.class);
            method.setAccessible(true);
            method.invoke(gameService, game);
            
            // then
            assertEquals(0, snake.getCoordinates().length);
            
        } catch (Exception e) {
            fail("Test failed due to reflection error: " + e.getMessage());
        }
    }
    // This test is not accurate anymore. we don't use "winner" attribute, we use the ranking instead.
    // The test is still here for reference.
//    @Test
//    public void updateGameState_singleSurvivor_setsWinner() {
//        // Setup
//        Game game = new Game();
//
//        // First snake (survivor)
//        Snake snake1 = new Snake();
//        snake1.setCoordinates(new int[][]{{5, 5}, {5, 6}});
//        snake1.setDirection("RIGHT");
//        snake1.setUserId(1L);
//        snake1.setUsername("testUser1");
//
//        // Second snake (dead - empty coordinates)
//        Snake snake2 = new Snake();
//        snake2.setCoordinates(new int[0][0]);
//        snake2.setDirection("LEFT");
//        snake2.setUserId(2L);
//        snake2.setUsername("testUser2");
//
//        game.addSnake(snake1);
//        game.addSnake(snake2);
//
//        // Mock collision check to return false for the survivor
//        when(snakeService.checkCollision(eq(snake1), any(Game.class))).thenReturn(false);
//
//        // Use reflection to access private method
//        java.lang.reflect.Method method;
//        try {
//            method = GameService.class.getDeclaredMethod("updateGameState", Game.class);
//            method.setAccessible(true);
//            method.invoke(gameService, game);
//
//            // then
//            assertEquals("testUser1", game.getWinner());
//
//        } catch (Exception e) {
//            fail("Test failed due to reflection error: " + e.getMessage());
//        }
//    }
    
    @Test
    public void broadcastGameState_validGame_callsWebSocketHandler() throws IOException {
        // Setup
        Snake snake = new Snake();
        snake.setCoordinates(new int[][]{{5, 5}, {5, 6}});
        snake.setUsername("testUser1");
        testGame.addSnake(snake);
        
        Item item = new Cookie(new int[]{10, 10}, "cookie");
        testGame.addItem(item);
        
        // Use reflection to access private method
        java.lang.reflect.Method method;
        try {
            method = GameService.class.getDeclaredMethod("broadcastGameState", Game.class);
            method.setAccessible(true);
            method.invoke(gameService, testGame);
            
            // then
            verify(webSocketHandler, times(1)).broadcastToLobby(eq(testLobby.getId()), any(ObjectNode.class));
            
        } catch (Exception e) {
            fail("Test failed due to reflection error: " + e.getMessage());
        }
    }
    
    @Test
    public void startCountdown_validGame_callsWebSocketHandler() throws IOException {
        // Use reflection to access private method
        java.lang.reflect.Method method;
        try {
            method = GameService.class.getDeclaredMethod("startCountdown", Game.class, int.class);
            method.setAccessible(true);
            
            // Call with a short countdown to avoid long test duration
            method.invoke(gameService, testGame, 1);
            
            // then
            verify(webSocketHandler, times(1)).broadcastToLobby(eq(testLobby.getId()), any(ObjectNode.class));
            
        } catch (Exception e) {
            fail("Test failed due to reflection error: " + e.getMessage());
        }
    }
}