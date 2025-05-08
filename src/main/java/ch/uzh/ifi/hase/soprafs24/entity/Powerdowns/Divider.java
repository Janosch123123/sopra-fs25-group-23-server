package ch.uzh.ifi.hase.soprafs24.entity.Powerdowns;

import ch.uzh.ifi.hase.soprafs24.entity.Item;
import ch.uzh.ifi.hase.soprafs24.entity.Snake;

public class Divider extends Item {
    private int[][] FourPositions;

    public Divider(int[] position, String type) {
        // Ruft den Konstruktor von Item mit position und type auf
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
    }

    public void applyEffect(Snake snake) {
        snake.addEffect(this);
        int[][] oldCoordinates = snake.getCoordinates();
        if (oldCoordinates.length <= 2) {
            return;}

        int newLength = (int) Math.ceil(oldCoordinates.length / 2.0);
        int[][] newCoordinates = new int[newLength][2];
        // Kopiere den Kopf und die erste Hälfte der Koordinaten
        for (int i = 0; i < newLength; i++) {
            newCoordinates[i] = oldCoordinates[i].clone();
        }

        snake.setCoordinates(newCoordinates);
    }
    public void checkIfActive(Snake snake){
        snake.removeEffect(this);
    }

    public int[][] getFourPositions() {return FourPositions;}
}