package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Item;
import ch.uzh.ifi.hase.soprafs24.entity.Powerdowns.Divider;
import ch.uzh.ifi.hase.soprafs24.entity.Powerups.Cookie;
import ch.uzh.ifi.hase.soprafs24.entity.Snake;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class BotServiceTest {

    @Mock
    private GameService gameService;

    @InjectMocks
    private BotService botService;

    private Game testGame;
    private Snake testSnake;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        // Setup test snake
        testSnake = new Snake();
        testSnake.setUserId(1L);
        testSnake.setDirection("RIGHT");
        testSnake.setCoordinates(new int[][]{{10, 10}, {9, 10}, {8, 10}});
        testSnake.setPreviousCurve("RIGHTCURVE");

        // Setup test game
        testGame = new Game();
        testGame.setGameId(1L);
        testGame.setCookieSpawnRate(1); // Setting non-zero for tests
        testGame.addSnake(testSnake);
    }

    @Test
    public void updateBot_basicFunctionality() {
        // Test the main method
        botService.updateBot(testGame, testSnake);
        
        // Verify the snake has a valid direction
        assertTrue(Arrays.asList("UP", "DOWN", "LEFT", "RIGHT").contains(testSnake.getDirection()));
    }
    
    @Test
    public void newHeadHelper_allDirections() {
        try {
            Method method = BotService.class.getDeclaredMethod("newHeadHelper", String.class, int[][].class);
            method.setAccessible(true);
            
            int[][] coords = {{10, 10}, {9, 10}, {8, 10}};
            
            // Test UP
            int[] upResult = (int[]) method.invoke(botService, "UP", coords);
            assertArrayEquals(new int[]{10, 9}, upResult);
            
            // Test DOWN
            int[] downResult = (int[]) method.invoke(botService, "DOWN", coords);
            assertArrayEquals(new int[]{10, 11}, downResult);
            
            // Test LEFT
            int[] leftResult = (int[]) method.invoke(botService, "LEFT", coords);
            assertArrayEquals(new int[]{9, 10}, leftResult);
            
            // Test RIGHT
            int[] rightResult = (int[]) method.invoke(botService, "RIGHT", coords);
            assertArrayEquals(new int[]{11, 10}, rightResult);
            
        } catch (Exception e) {
            fail("Test failed: " + e.getMessage());
        }
    }
    
    @Test
    public void newHeadHelper_invalidDirection() {
        try {
            Method method = BotService.class.getDeclaredMethod("newHeadHelper", String.class, int[][].class);
            method.setAccessible(true);
            
            int[][] coords = {{10, 10}, {9, 10}, {8, 10}};
            
            // Test invalid direction
            Exception exception = assertThrows(InvocationTargetException.class, () -> {
                method.invoke(botService, "INVALID", coords);
            });
            
            // Verify that the underlying exception is IllegalArgumentException
            assertTrue(exception.getCause() instanceof IllegalArgumentException);
            
        } catch (Exception e) {
            fail("Test setup failed: " + e.getMessage());
        }
    }

    @Test
    public void mapTwoDirectionsToCurve_basicCombinations() {
        try {
            Method method = BotService.class.getDeclaredMethod("mapTwoDirectionsToCurve", Snake.class, String.class, String.class);
            method.setAccessible(true);
            
            assertEquals("LEFTCURVE", method.invoke(botService, testSnake, "UP", "LEFT"));
            assertEquals("RIGHTCURVE", method.invoke(botService, testSnake, "UP", "RIGHT"));
            assertEquals("RIGHTCURVE", method.invoke(botService, testSnake, "DOWN", "LEFT"));
            assertEquals("LEFTCURVE", method.invoke(botService, testSnake, "DOWN", "RIGHT"));
            
        } catch (Exception e) {
            fail("Test failed: " + e.getMessage());
        }
    }
    
    @Test
    public void mapCurveToDirection_basicCombinations() {
        try {
            Method method = BotService.class.getDeclaredMethod("mapCurveToDirection", String.class, String.class);
            method.setAccessible(true);
            
            assertEquals("LEFT", method.invoke(botService, "LEFTCURVE", "UP"));
            assertEquals("RIGHT", method.invoke(botService, "LEFTCURVE", "DOWN"));
            assertEquals("RIGHT", method.invoke(botService, "RIGHTCURVE", "UP"));
            assertEquals("LEFT", method.invoke(botService, "RIGHTCURVE", "DOWN"));
            
        } catch (Exception e) {
            fail("Test failed: " + e.getMessage());
        }
    }
    
    @Test
    public void availableMoves_basicScenario() {
        try {
            Method method = BotService.class.getDeclaredMethod("availableMoves", Snake.class, Game.class, int[][].class, String.class);
            method.setAccessible(true);
            
            // Test if UP is available
            boolean upAvailable = (boolean) method.invoke(botService, testSnake, testGame, testSnake.getCoordinates(), "UP");
            assertTrue(upAvailable);
            
            // Test if wall collision is detected
            testSnake.setCoordinates(new int[][]{{0, 10}, {1, 10}, {2, 10}});
            boolean leftAvailable = (boolean) method.invoke(botService, testSnake, testGame, testSnake.getCoordinates(), "LEFT");
            assertFalse(leftAvailable);
            
        } catch (Exception e) {
            fail("Test failed: " + e.getMessage());
        }
    }
    
    @Test
    public void availablePowerUpOrCookieMoves_withCookie() {
        try {
            Method method = BotService.class.getDeclaredMethod("availablePowerUpOrCookieMoves", Snake.class, Game.class, int[][].class, String.class);
            method.setAccessible(true);
            
            // Add a cookie
            int[] cookiePosition = {10, 9};
            Cookie cookie = new Cookie(cookiePosition, "COOKIE");
            testGame.addItem(cookie);
            
            // Test if UP leads to a cookie
            boolean cookieUpward = (boolean) method.invoke(botService, testSnake, testGame, testSnake.getCoordinates(), "UP");
            assertTrue(cookieUpward);
            
            // Test if RIGHT doesn't lead to a cookie
            boolean cookieRightward = (boolean) method.invoke(botService, testSnake, testGame, testSnake.getCoordinates(), "RIGHT");
            assertFalse(cookieRightward);
            
        } catch (Exception e) {
            fail("Test failed: " + e.getMessage());
        }
    }
    
    @Test
    public void isGoingToPickUpPowerDown_basicTest() {
        try {
            Method method = BotService.class.getDeclaredMethod("isGoingToPickUpPowerDown", Game.class, int[][].class, String.class);
            method.setAccessible(true);
            
            // Test with no power-downs in game
            boolean result = (boolean) method.invoke(botService, testGame, testSnake.getCoordinates(), "RIGHT");
            assertFalse(result);
            
            // We can't easily test the positive case without knowing the exact structure
            // of your Divider and ReverseControl classes, but at least the negative case
            // is covered
            
        } catch (Exception e) {
            fail("Test failed: " + e.getMessage());
        }
    }
}