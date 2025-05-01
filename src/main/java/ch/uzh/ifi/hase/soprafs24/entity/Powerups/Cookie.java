package ch.uzh.ifi.hase.soprafs24.entity.Powerups;

import ch.uzh.ifi.hase.soprafs24.entity.Item;
import ch.uzh.ifi.hase.soprafs24.entity.Snake;


public class Cookie extends Item {

    public Cookie(int[] position, String type) {
        // Ruft den Konstruktor von Item mit position und type auf
        super(position, type);
    }

    public void applyEffect(Snake snake) {
    }
}
