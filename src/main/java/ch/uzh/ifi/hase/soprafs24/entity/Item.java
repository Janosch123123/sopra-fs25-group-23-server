package ch.uzh.ifi.hase.soprafs24.entity;

public class Item {
    private int[] Position;
    private String type;



    public Item(int[] position, String type) {
        Position = position;
        this.type = type;
    }
    public int[] getPosition() {return Position;}
    public String getType() {return type;}
}
