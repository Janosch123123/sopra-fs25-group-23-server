package ch.uzh.ifi.hase.soprafs24.entity;

public class Snake {
    private int length;
    private int[][] coordinates;
    private String direction;
    private int[] head;
    private int[] tail;
    private Long userId;
    private String username;

    public static boolean checkCollision(Snake snake, Game game) {
        for (Snake otherSnake : game.getSnakes()) {
            if (snake != otherSnake) {
                for (int i = 0; i < snake.getLength(); i++) {
                    if (snake.getCoordinates()[i][0] == otherSnake.getCoordinates()[i][0]
                            && snake.getCoordinates()[i][1] == otherSnake.getCoordinates()[i][1]) {
                        return true;
                    }

                }
            }
        }
        return false;
    }
    public String getUsername() {return username;}
    public void setUsername(String username) {this.username = username;}

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

    public void growSnake() {
        // increase size of snake
    }

    public void moveSnake() {
        // move snake one field to in direction of head
    }
}
