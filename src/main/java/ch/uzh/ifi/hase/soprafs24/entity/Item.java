package ch.uzh.ifi.hase.soprafs24.entity;

public abstract class Item {
    private int[] position;
    private String type;

    public Item(int[] position, String type) {
        this.position = position;
        this.type = type;
    }
    public abstract void applyEffect(Snake snake);

    // Getters and setters
    public int[] getPosition() {
        return position;
    }

    public void setPosition(int[] position) {
        this.position = position;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
