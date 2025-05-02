package ch.uzh.ifi.hase.soprafs24.entity.Powerups;

import ch.uzh.ifi.hase.soprafs24.entity.Item;
import ch.uzh.ifi.hase.soprafs24.entity.Snake;

public class GoldenCookie extends Item {

    public GoldenCookie(int[] position, String type) {
        super(position, type);
    }

    public void applyEffect(Snake snake) {
        snake.addGrowCount();
        snake.addGrowCount();
        snake.addGrowCount();
        snake.addGrowCount();
        snake.addGrowCount();
    }
}
