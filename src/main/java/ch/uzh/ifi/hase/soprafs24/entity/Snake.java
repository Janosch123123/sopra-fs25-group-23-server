package ch.uzh.ifi.hase.soprafs24.entity;

import java.util.ArrayList;
import java.util.List;

public class Snake {
    private int length;
    private int[][] coordinates;
    private String direction;
    private int[] head;
    private int[] tail;
    private Long userId;
    private String username;
    private List<String> directionQueue = new ArrayList<>();

    public void addDirectionQueue(String direction){
        if (directionQueue.size() < 2){
            directionQueue.add(direction);
        }
    }

    public List<String> getDirectionQueue(){
            return directionQueue; 
    }
    
    public void setDirectionQueue(List<String> directionQueue){
        this.directionQueue = directionQueue;
    }

    public String popDirectionQueue(){
        if (directionQueue.size() > 0){
            String direction = directionQueue.get(0);
            directionQueue.remove(0);
            return direction;
        }
        return null;
    }

    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
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
