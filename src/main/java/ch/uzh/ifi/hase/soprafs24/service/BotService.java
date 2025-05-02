package ch.uzh.ifi.hase.soprafs24.service;

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Item;
import ch.uzh.ifi.hase.soprafs24.entity.Snake;
import ch.uzh.ifi.hase.soprafs24.entity.User;

@Service
@Transactional
public class BotService {

    private final GameService gameService;

    private int HEIGHT = 24;
    private int WIDTH = 29;
    private String[] MOVES = new String[] { "UP", "DOWN", "LEFT", "RIGHT" };

    BotService(@Lazy GameService gameService) {
        this.gameService = gameService;
    }

    public void updateBot(Game game, Snake snake) {
        int[][] coordinates = snake.getCoordinates();
        List<String> availableMoves = new ArrayList<>();
        List<String> cookieMoves = new ArrayList<>();
        for (String move : MOVES) {
            if (availableMoves(snake, game, coordinates, move)) {
                availableMoves.add(move);
            }
            if (availableCookieMoves(snake, game, coordinates, move)) {
                cookieMoves.add(move);
            }

        }

        if (availableMoves.size() > 0) {
            int randomIndex = (int) (Math.random() * availableMoves.size());
            String randomMove = availableMoves.get(randomIndex);
            double probabilityChangeMovement = (Math.random());
            boolean straightPossible = availableMoves.contains(snake.getDirection());

            if (cookieMoves.size() > 0) {
                int randomIndexCookie = (int) (Math.random()*cookieMoves.size());
                snake.setDirection(cookieMoves.get(randomIndexCookie));
            } else if (!straightPossible) {
                snake.setDirection(randomMove);
            } else if (probabilityChangeMovement < 0.2) {
                snake.setDirection(randomMove);
            } 

        } 
        return;
    }

    private boolean availableMoves(Snake snake, Game game, int[][] coordinates, String move) {

        int[] newHead = newHeadHelper(move, coordinates);

        if (newHead[0] < 0 || newHead[0] > WIDTH || newHead[1] < 0 || newHead[1] > HEIGHT) {
            return false;
        }



        for (Snake otherSnake : game.getSnakes()) {
            if (otherSnake.getCoordinates().length == 0) {
                continue;
            }
            for (int i = 0; i < otherSnake.getCoordinates().length; i++) {
                if (snake == otherSnake && i == 0) {
                    continue;
                }
                if (Arrays.equals(newHead, otherSnake.getCoordinates()[i])) {
                    return false;
                }
            }
        }
        return true;
        }

    private boolean availableCookieMoves(Snake snake, Game game, int[][] coordinates, String move) {
        int[] newHead = newHeadHelper(move, coordinates);

        if (newHead[0] < 0 || newHead[0] > WIDTH || newHead[1] < 0 || newHead[1] > HEIGHT) {
            return false;
        }
        for (Item item : game.getItems()) {
            if ("cookie".equals(item.getType())) {
                int[] cookiePosition = item.getPosition();

                if (newHead[0] == cookiePosition[0] && newHead[1] == cookiePosition[1]) {
                    return true; 
                }
            }
        }
        return false;

    
    }

    private int[] newHeadHelper(String move, int[][] coordinates) {
        int[] newHead;
        if (move.equals("UP")) {
            newHead = new int[]{coordinates[0][0], coordinates[0][1] - 1};
        } else if (move.equals("DOWN")) {
            newHead = new int[] {coordinates[0][0], coordinates[0][1] + 1};
        } else if (move.equals("LEFT")) {
            newHead = new int[]{coordinates[0][0] - 1, coordinates[0][1]};
        } else if (move.equals("RIGHT")) {
            newHead = new int[] {coordinates[0][0] + 1, coordinates[0][1]};
        } else {
            throw new IllegalArgumentException("Invalid direction: " + move);
        }
        return newHead;
    }
}
