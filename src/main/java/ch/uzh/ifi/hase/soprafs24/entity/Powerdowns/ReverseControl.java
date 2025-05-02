package ch.uzh.ifi.hase.soprafs24.entity.Powerdowns;

import ch.uzh.ifi.hase.soprafs24.entity.Item;
import ch.uzh.ifi.hase.soprafs24.entity.Snake;

public class ReverseControl extends Item {
    private int timer;
    private int[][] FourPositions;

    public ReverseControl(int[] position, String type) {
        super(position, type);
        this.FourPositions = new int[4][2];
        // Obere linke Koordinate (wird übergeben)
        this.FourPositions[0] = position.clone();
        // Obere rechte Koordinate (x-1, y)
        this.FourPositions[1] = new int[]{position[0] + 1, position[1]};
        // Untere linke Koordinate (x-1, y+1)
        this.FourPositions[2] = new int[]{position[0], position[1] + 1};
        // Untere rechte Koordinate (x, y+1)
        this.FourPositions[3] = new int[]{position[0]+1, position[1] + 1};
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
    public int[][] getFourPositions() {return FourPositions;}


}
