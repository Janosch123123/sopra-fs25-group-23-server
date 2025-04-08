package ch.uzh.ifi.hase.soprafs24.service;

import javax.transaction.Transactional;

import org.springframework.stereotype.Service;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Snake;

@Service
@Transactional
public class SnakeSerivce {

    public void growSnake() {
        // increase size of snake
    }

    public void moveSnake(Snake snake) {
        int[][] coordinates = snake.getCoordinates();
        int[] newHead;
        if (snake.getDirection().equals("UP")) {
            newHead = new int[]{coordinates[0][0], coordinates[0][1] - 1};
        } else if (snake.getDirection().equals("DOWN")) {
            newHead = new int[] {coordinates[0][0], coordinates[0][1] + 1};
        } else if (snake.getDirection().equals("LEFT")) {
            newHead = new int[]{coordinates[0][0] - 1, coordinates[0][1]};
        } else if (snake.getDirection().equals("RIGHT")) {
            newHead = new int[] {coordinates[0][0] + 1, coordinates[0][1]};
        } else {
            throw new IllegalArgumentException("Invalid direction: " + snake.getDirection());
        }
        int[][] newCoordinates = new int[coordinates.length][];
        newCoordinates[0] = newHead;
        System.arraycopy(coordinates, 0, newCoordinates, 1, coordinates.length-1);
        snake.setCoordinates(newCoordinates);
        System.out.println("Snake moved to: " + newHead[0] + ", " + newHead[1]);
    }

    public boolean checkCollision(Snake snake, Game game) {
        int[] head = snake.getCoordinates()[0];
        //check for collision with walls
        if (head[0] < 0 || head[0] >= 30 || head[1] < 0 || head[1] >= 25) {
            return true;
        }
        // check collision with other snakes
        for (Snake otherSnake : game.getSnakes()) {
            for (int i = 0; i < snake.getLength(); i++) {
                if (snake == otherSnake && i == 0) {
                    continue;
                }
                
                if (head == otherSnake.getCoordinates()[i]) {
                    return true;
                }
            }
        }
        return false;
    }
}
