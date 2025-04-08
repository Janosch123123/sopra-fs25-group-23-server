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
}
