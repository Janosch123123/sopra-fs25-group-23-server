package ch.uzh.ifi.hase.soprafs24.entity.Powerups;

import ch.uzh.ifi.hase.soprafs24.entity.Item;
import ch.uzh.ifi.hase.soprafs24.entity.Snake;

public class GoldenCookie extends Item {
    private int count = 5;
    public GoldenCookie(int[] position, String type) {
        super(position, type);
    }

    public void applyEffect(Snake snake) {
        snake.addEffect(this);
    }
    public void growGolden(Snake snake){
        if (count > 0){
            snake.addGrowCount();
            count--;
        }
        else{snake.removeEffect(this);}
    }
    public int getCount() {return count;}
}
