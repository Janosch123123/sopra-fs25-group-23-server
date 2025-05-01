package ch.uzh.ifi.hase.soprafs24.entity.Powerups;

import ch.uzh.ifi.hase.soprafs24.entity.Item;
import ch.uzh.ifi.hase.soprafs24.entity.Snake;

public class Multiplier extends Item {
    private float start;

    public Multiplier(int[] position, String type) {
        super(position, type);
    }

    public void applyEffect(Snake snake) {
        snake.addEffect(this);
        this.start = snake.getGame().getTimestamp();
    }

    public void multiplyCookie(Snake snake){  // function gets called when cookie gets eaten and this effect is active
        if (this.start - snake.getGame().getTimestamp() < 15){
            snake.addGrowCount();
        }else{snake.removeEffect(this);}
    }
}
