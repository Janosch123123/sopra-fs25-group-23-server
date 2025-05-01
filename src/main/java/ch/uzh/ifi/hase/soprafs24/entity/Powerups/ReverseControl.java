package ch.uzh.ifi.hase.soprafs24.entity.Powerups;

import ch.uzh.ifi.hase.soprafs24.entity.Item;
import ch.uzh.ifi.hase.soprafs24.entity.Snake;

public class ReverseControl extends Item {
    private int timer;

    public ReverseControl(int[] position, String type) {
        super(position, type);
        this.timer = 4;
    }

    public void applyEffect(Snake snake) {
        snake.addEffect(this); // when powerup gets eaten... it must be stored
    }

    public int getTimer() {return timer;}

    public void revertMovement(Snake snake) {
        if (timer > 0) {
            timer--;
            switch (snake.getDirection()) {
                case "UP": snake.setDirection("DOWN"); break;
                case "DOWN": snake.setDirection("UP"); break;
                case "LEFT": snake.setDirection("RIGHT"); break;
                case "RIGHT": snake.setDirection("LEFT"); break;
            }
        }
        else {
            snake.removeEffect(this);
        }
    }


}
