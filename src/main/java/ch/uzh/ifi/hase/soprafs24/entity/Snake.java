package ch.uzh.ifi.hase.soprafs24.entity;

public class Snake {
    private int length;
    private int[][] coordinates;
    private String direction;
    private int[] head;
    private int[] tail;
    private Long userId;

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int[][] getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(int[][] coordinates) {
        this.coordinates = coordinates;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public int[] getHead() {
        return head;
    }

    public void setHead(int[] head) {
        this.head = head;
    }

    public int[] getTail() {
        return tail;
    }

    public void setTail(int[] tail) {
        this.tail = tail;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
