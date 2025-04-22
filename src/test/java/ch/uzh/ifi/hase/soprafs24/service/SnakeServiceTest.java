package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Item;
import ch.uzh.ifi.hase.soprafs24.entity.Snake;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SnakeServiceTest {

    @InjectMocks
    private SnakeService snakeService;

    private Snake testSnake;
    private Game testGame;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        // Setup test game
        testGame = new Game();
        testGame.setItems(new ArrayList<>());
        testGame.setSnakes(new ArrayList<>());

        // Setup test snake
        testSnake = new Snake();
        testSnake.setDirection("RIGHT");
        testSnake.setCoordinates(new int[][]{{5, 5}, {4, 5}, {3, 5}});
        testSnake.setGame(testGame);

        // Add snake to game
        testGame.getSnakes().add(testSnake);
    }

    @Test
    public void moveSnake_rightDirection_success() {
        // Call the method
        snakeService.moveSnake(testSnake);

        // Check snake moved correctly
        int[][] newCoordinates = testSnake.getCoordinates();
        assertEquals(3, newCoordinates.length);
        assertEquals(6, newCoordinates[0][0]); // new head x is +1
        assertEquals(5, newCoordinates[0][1]); // new head y is same
        assertEquals(5, newCoordinates[1][0]); // old head is now second segment
        assertEquals(5, newCoordinates[1][1]);
        assertEquals(4, newCoordinates[2][0]); // old second segment is now third
        assertEquals(5, newCoordinates[2][1]);
    }

    @Test
    public void moveSnake_leftDirection_success() {
        // Setup
        testSnake.setDirection("LEFT");
        
        // Call the method
        snakeService.moveSnake(testSnake);

        // Check snake moved correctly
        int[][] newCoordinates = testSnake.getCoordinates();
        assertEquals(3, newCoordinates.length);
        assertEquals(4, newCoordinates[0][0]); // new head x is -1
        assertEquals(5, newCoordinates[0][1]); // new head y is same
    }

    @Test
    public void moveSnake_upDirection_success() {
        // Setup
        testSnake.setDirection("UP");
        
        // Call the method
        snakeService.moveSnake(testSnake);

        // Check snake moved correctly
        int[][] newCoordinates = testSnake.getCoordinates();
        assertEquals(3, newCoordinates.length);
        assertEquals(5, newCoordinates[0][0]); // new head x is same
        assertEquals(4, newCoordinates[0][1]); // new head y is -1
    }

    @Test
    public void moveSnake_downDirection_success() {
        // Setup
        testSnake.setDirection("DOWN");
        
        // Call the method
        snakeService.moveSnake(testSnake);

        // Check snake moved correctly
        int[][] newCoordinates = testSnake.getCoordinates();
        assertEquals(3, newCoordinates.length);
        assertEquals(5, newCoordinates[0][0]); // new head x is same
        assertEquals(6, newCoordinates[0][1]); // new head y is +1
    }

    @Test
    public void moveSnake_invalidDirection_throwsException() {
        // Setup
        testSnake.setDirection("INVALID");
        
        // Check exception
        assertThrows(IllegalArgumentException.class, () -> {
            snakeService.moveSnake(testSnake);
        });
    }

    @Test
    public void moveSnake_eatsCookie_growsSnake() {
        // Setup - add a cookie to the game at the position where the snake will move
        Item cookie = new Item();
        cookie.setType("cookie");
        cookie.setPosition(new int[]{6, 5}); // Position right in front of snake
        testGame.getItems().add(cookie);

        // Initial length
        int initialLength = testSnake.getCoordinates().length;
        
        // Call the method
        snakeService.moveSnake(testSnake);

        // Check snake ate cookie and grew
        int[][] newCoordinates = testSnake.getCoordinates();
        assertEquals(6, newCoordinates[0][0]); // Head moved to cookie position
        assertEquals(5, newCoordinates[0][1]);
        
        // Check the body segments are preserved correctly 
        // The old head (5,5) should now be the second segment
        assertEquals(5, newCoordinates[1][0]);
        assertEquals(5, newCoordinates[1][1]);
        
        // The old second segment (4,5) should now be the third segment
        assertEquals(4, newCoordinates[2][0]);
        assertEquals(5, newCoordinates[2][1]);
        
        // Check cookie was removed from items
    }

    @Test
    public void checkCollision_hitsWall_returnsTrue() {
        // Setup - position snake head next to wall
        testSnake.setCoordinates(new int[][]{{29, 5}, {28, 5}, {27, 5}});
        testSnake.setDirection("RIGHT");
        
        // Move snake into wall
        snakeService.moveSnake(testSnake);
        
        // Check collision detected
        assertTrue(snakeService.checkCollision(testSnake, testGame));
    }

    @Test
    public void checkCollision_hitsItself_returnsTrue() {
        // Setup - position snake in a way it will hit itself after moving
        testSnake.setCoordinates(new int[][]{{5, 5}, {5, 6}, {6, 6}, {6, 5}, {5, 5}});
        
        // Check collision with itself
        assertTrue(snakeService.checkCollision(testSnake, testGame));
    }

    @Test
    public void checkCollision_noCollision_returnsFalse() {
        // Snake is in a valid position, not hitting anything
        boolean result = snakeService.checkCollision(testSnake, testGame);
        
        // Check no collision
        assertFalse(result);
    }

    @Test
    public void checkCollision_emptySnake_returnsFalse() {
        // Setup - empty snake coordinates
        testSnake.setCoordinates(new int[][]{});
        
        // Check no collision for empty snake
        assertFalse(snakeService.checkCollision(testSnake, testGame));
    }

    @Test
    public void checkCollision_deadSnake_ignoresCollision() {
        // Setup - add a "dead" snake with empty coordinates
        Snake deadSnake = new Snake();
        deadSnake.setCoordinates(new int[][]{});
        testGame.getSnakes().add(deadSnake);
        
        // Normal snake shouldn't collide with dead snake
        assertFalse(snakeService.checkCollision(testSnake, testGame));
    }
}