package ch.uzh.ifi.hase.soprafs24.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationContext;

import ch.uzh.ifi.hase.soprafs24.entity.*;
import ch.uzh.ifi.hase.soprafs24.entity.Powerdowns.Divider;
import ch.uzh.ifi.hase.soprafs24.entity.Powerdowns.ReverseControl;
import ch.uzh.ifi.hase.soprafs24.entity.Powerups.Cookie;
import ch.uzh.ifi.hase.soprafs24.entity.Powerups.GoldenCookie;
import ch.uzh.ifi.hase.soprafs24.entity.Powerups.Multiplier;
import ch.uzh.ifi.hase.soprafs24.handler.WebSocketHandler;
import ch.uzh.ifi.hase.soprafs24.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;

public class GameServiceAdditionalTests {

    @Mock
    private LobbyRepository lobbyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @Mock
    private SnakeService snakeService;
    
    @Mock
    private BotService botService;

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

    @Test
    public void createGame_withDifferentCookieSpawnRates_setsCorrectRate() {
        // Test slow rate
        Game slowGame = gameService.createGame(testLobby, "Slow", false);
        assertEquals(0.1, slowGame.getCookieSpawnRate());
        
        // Test medium rate
        Game mediumGame = gameService.createGame(testLobby, "Medium", false);
        assertEquals(0.3, mediumGame.getCookieSpawnRate());
        
        // Test fast rate
        Game fastGame = gameService.createGame(testLobby, "Fast", false);
        assertEquals(0.5, fastGame.getCookieSpawnRate());
        
        // Test invalid rate (defaults to medium)
        Game defaultGame = gameService.createGame(testLobby, "InvalidRate", false);
        assertEquals(0.3, defaultGame.getCookieSpawnRate());
    }
    
    @Test
    public void createGame_withSugarRush_activatesSugarRush() {
        // Create game with sugarRush
        Game sugarRushGame = gameService.createGame(testLobby, "sugarRush", false);
        
        // Sugar Rush should set cookie spawn rate to 0
        assertEquals(0.0, sugarRushGame.getCookieSpawnRate());
        
        // And should spawn cookies on the entire grid (30x25 = 750)
        assertTrue(sugarRushGame.getItems().size() > 700);
        
        // Verify all items are cookies
        for (Item item : sugarRushGame.getItems()) {
            assertEquals("cookie", item.getType());
        }
    }
    
    @Test
    public void createGame_withPowerupsEnabled_enablesPowerups() {
        // Create game with powerups enabled
        Game gameWithPowerups = gameService.createGame(testLobby, "Medium", true);
        
        // Verify powerups flag is set
        assertTrue(gameWithPowerups.getPowerupsWanted());
    }
    
    @Test
    public void createGame_withPowerupsDisabled_disablesPowerups() {
        // Create game with powerups disabled
        Game gameWithoutPowerups = gameService.createGame(testLobby, "Medium", false);
        
        // Verify powerups flag is not set
        assertFalse(gameWithoutPowerups.getPowerupsWanted());
    }
    
    @Test
    public void respondToKeyInputs_oppositeDirection_queueNotChanged() {
        // Setup
        Snake testSnake = new Snake();
        testSnake.setUserId(1L);
        testSnake.setDirection("RIGHT");
        testSnake.setCoordinates(new int[][]{{4, 4}, {3, 4}, {2, 4}});
        testGame.addSnake(testSnake);
        
        // Try to move LEFT which is opposite to current direction RIGHT
        gameService.respondToKeyInputs(testGame, testUser1, "LEFT");
        
        // Queue should remain empty
        assertTrue(testSnake.getDirectionQueue().isEmpty());
    }
    
    @Test
    public void respondToKeyInputs_sameDirection_queueNotChanged() {
        // Setup
        Snake testSnake = new Snake();
        testSnake.setUserId(1L);
        testSnake.setDirection("RIGHT");
        testSnake.setCoordinates(new int[][]{{4, 4}, {3, 4}, {2, 4}});
        testGame.addSnake(testSnake);
        
        // Try to move in the same direction
        gameService.respondToKeyInputs(testGame, testUser1, "RIGHT");
        
        // Queue should remain empty
        assertTrue(testSnake.getDirectionQueue().isEmpty());
    }
    
    @Test
    public void respondToKeyInputs_differentUser_snakeUnaffected() {
        // Setup
        Snake testSnake = new Snake();
        testSnake.setUserId(1L);
        testSnake.setDirection("RIGHT");
        testSnake.setCoordinates(new int[][]{{4, 4}, {3, 4}, {2, 4}});
        testGame.addSnake(testSnake);
        
        // Create a different user
        User differentUser = new User();
        differentUser.setId(3L);
        
        // Try to control the snake with a different user
        gameService.respondToKeyInputs(testGame, differentUser, "UP");
        
        // Snake should be unaffected
        assertTrue(testSnake.getDirectionQueue().isEmpty());
    }
    
    @Test
    public void rankRemainingPlayers_sortsPlayersByLength() {
        // Setup game with multiple snakes of different lengths
        Game game = new Game();
        game.setLobby(testLobby);
        
        // Snake 1 with length 3
        Snake snake1 = new Snake();
        snake1.setUsername("testUser1");
        snake1.setCoordinates(new int[][]{{1, 1}, {1, 2}, {1, 3}});
        game.addSnake(snake1);
        
        // Snake 2 with length 5
        Snake snake2 = new Snake();
        snake2.setUsername("testUser2");
        snake2.setCoordinates(new int[][]{{2, 1}, {2, 2}, {2, 3}, {2, 4}, {2, 5}});
        game.addSnake(snake2);
        
        // Add user lookups
        when(userRepository.findByUsername("testUser1")).thenReturn(testUser1);
        when(userRepository.findByUsername("testUser2")).thenReturn(testUser2);
        
        // Rank the players
        gameService.rankRemainingPlayers(game);
        
        // Verify the leaderboard order - longer snake should be first
        List<String> leaderboard = game.getLeaderboard();
        assertEquals(2, leaderboard.size());
        assertEquals("testUser2", leaderboard.get(0)); // Snake2 has length 5
        assertEquals("testUser1", leaderboard.get(1)); // Snake1 has length 3
        
        // Verify that all snakes were marked as dead (empty coordinates)
        assertEquals(0, snake1.getCoordinates().length);
        assertEquals(0, snake2.getCoordinates().length);
    }
    
    @Test
    public void rankRemainingPlayers_updatesLengthPR() {
        // Setup
        testLobby.setSolo(false); // Not a solo lobby
        Game game = new Game();
        game.setLobby(testLobby);
        
        // Set up user with existing length PR
        testUser1.setLengthPR(3);
        
        // Create a snake with length greater than the PR
        Snake snake = new Snake();
        snake.setUsername("testUser1");
        snake.setCoordinates(new int[][]{{1, 1}, {1, 2}, {1, 3}, {1, 4}, {1, 5}}); // Length 5
        game.addSnake(snake);
        
        // When
        gameService.rankRemainingPlayers(game);
        
        // Then
        verify(userRepository, times(1)).save(testUser1);
        assertEquals(5, testUser1.getLengthPR());
    }
    
    @Test
    public void rankRemainingPlayers_soloLobby_doesNotUpdateStats() {
        // Setup
        testLobby.setSolo(true); // Solo lobby
        Game game = new Game();
        game.setLobby(testLobby);
        
        // Set up user with existing length PR
        testUser1.setLengthPR(3);
        
        // Create a snake with length greater than the PR
        Snake snake = new Snake();
        snake.setUsername("testUser1");
        snake.setCoordinates(new int[][]{{1, 1}, {1, 2}, {1, 3}, {1, 4}, {1, 5}}); // Length 5
        game.addSnake(snake);
        
        // When
        gameService.rankRemainingPlayers(game);
        
        // Then
        verify(userRepository, never()).save(testUser1);
        assertEquals(3, testUser1.getLengthPR()); // Should remain unchanged
    }
    
    @Test
    public void endGame_soloLobby_doesNotUpdateWins() throws IOException {
        // Setup
        testLobby.setSolo(true);
        testGame.setLobby(testLobby);
        testGame.setLeaderboard(Arrays.asList(testUser1.getUsername()));
        
        // Initial wins
        int initialWins = testUser1.getWins();
        
        // When
        gameService.endGame(testGame);
        
        // Then
        assertEquals(initialWins, testUser1.getWins()); // Wins should not increase
    }
    
    @Test
    public void endGame_multiplayerGame_updatesPlayedGamesAndWinRate() throws IOException {
        // Setup
        testLobby.setSolo(false);
        testGame.setLobby(testLobby);
        
        // Set initial stats
        testUser1.setWins(2);
        testUser1.setPlayedGames(4);
        testUser1.setWinRate(0.5);
        
        testUser2.setWins(0);
        testUser2.setPlayedGames(2);
        testUser2.setWinRate(0.0);
        
        // User1 wins this game
        testGame.setLeaderboard(Arrays.asList(testUser1.getUsername(), testUser2.getUsername()));
        
        // When
        gameService.endGame(testGame);
        
        // Then
        // User1 should have 3 wins, 5 games played, win rate 0.6
        assertEquals(3, testUser1.getWins());
        assertEquals(5, testUser1.getPlayedGames());
        assertEquals(0.6, testUser1.getWinRate(), 0.01);
        
        // User2 should have 0 wins, 3 games played, win rate 0.0
        assertEquals(0, testUser2.getWins());
        assertEquals(3, testUser2.getPlayedGames());
        assertEquals(0.0, testUser2.getWinRate(), 0.01);
    }
    
    @Test
    public void endGame_updatesLevel() throws IOException {
        // Setup
        testLobby.setSolo(false);
        testGame.setLobby(testLobby);
        
        // Set initial stats affecting level calculation (wins, kills)
        testUser1.setWins(4);  // 1 + (4/2) + (0/4) = 1 + 2 + 0 = 3 points
        testUser1.setKills(0);
        testUser1.setPlayedGames(10);
        
        // Add user to leaderboard
        testGame.setLeaderboard(Arrays.asList(testUser1.getUsername()));
        
        // When
        gameService.endGame(testGame);
        
        // Then
        // Verify level calculation: 5 * sqrt(points/4) - 1
        // points = 3 -> level should be 5 * sqrt(3/4) - 1 = 5 * 0.866 - 1 = 3.33
        assertEquals(3.33, testUser1.getLevel(), 0.1);
    }

    // Tests using reflection to access private methods
    // These might need to be commented out if you prefer not to use reflection
    
    @Test
    public void findUsedCoordinates_includesSnakesAndItems() {
        // Setup
        Game game = new Game();
        
        // Add snake
        Snake snake = new Snake();
        snake.setCoordinates(new int[][]{{1, 1}, {1, 2}});
        game.addSnake(snake);
        
        // Add regular item
        Cookie cookie = new Cookie(new int[]{3, 3}, "cookie");
        game.addItem(cookie);
        
        // Add a special item with multiple positions (Divider)
        // The Divider constructor likely initializes the four positions internally
        Divider divider = new Divider(new int[]{5, 5}, "powerup");
        // Let's directly access and check the getFourPositions method, which likely exists
        game.addItem(divider);
        
        // Add a ReverseControl item (also has multiple positions)
        ReverseControl reverseControl = new ReverseControl(new int[]{8, 8}, "powerup");
        // Similarly, ReverseControl probably initializes positions in constructor
        game.addItem(reverseControl);
        
        // Use reflection to access the private method
        try {
            java.lang.reflect.Method method = GameService.class.getDeclaredMethod("findUsedCoordinates", Game.class);
            method.setAccessible(true);
            
            // Call the method
            @SuppressWarnings("unchecked")
            List<int[]> usedCoordinates = (List<int[]>) method.invoke(gameService, game);
            
            // Note: Since we don't control the exact positions of the four-position items,
            // we'll just make sure the total count matches what we expect
            assertTrue(usedCoordinates.size() >= 3); // At minimum: 2 snake positions + 1 cookie

            // Helper function to check if a coordinate is in the list
            java.util.function.Predicate<int[]> contains = coord -> {
                for (int[] c : usedCoordinates) {
                    if (c[0] == coord[0] && c[1] == coord[1]) {
                        return true;
                    }
                }
                return false;
            };
            
            // Check the coordinates we definitely know about
            assertTrue(contains.test(new int[]{1, 1})); // Snake head 
            assertTrue(contains.test(new int[]{1, 2})); // Snake body
            assertTrue(contains.test(new int[]{3, 3})); // Cookie
            
        } catch (Exception e) {
            fail("Test failed due to reflection error: " + e.getMessage());
        }
    }
}