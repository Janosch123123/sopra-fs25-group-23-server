package ch.uzh.ifi.hase.soprafs24.entity.Powerups;

import ch.uzh.ifi.hase.soprafs24.entity.Item;
import ch.uzh.ifi.hase.soprafs24.entity.Snake;

public class Divider extends Item {

    public Divider(int[] position, String type) {
        // Ruft den Konstruktor von Item mit position und type auf
        super(position, type);
    }

    public void applyEffect(Snake snake) {
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
}