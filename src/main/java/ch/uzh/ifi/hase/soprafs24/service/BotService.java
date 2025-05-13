package ch.uzh.ifi.hase.soprafs24.service;

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Item;
import ch.uzh.ifi.hase.soprafs24.entity.Powerdowns.Divider;
import ch.uzh.ifi.hase.soprafs24.entity.Powerdowns.ReverseControl;
import ch.uzh.ifi.hase.soprafs24.entity.Powerups.Cookie;
import ch.uzh.ifi.hase.soprafs24.entity.Powerups.GoldenCookie;
import ch.uzh.ifi.hase.soprafs24.entity.Powerups.Multiplier;
import ch.uzh.ifi.hase.soprafs24.entity.Snake;

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
        List<String> cookieOrPowerUpMoves = new ArrayList<>();
        for (String move : MOVES) {
            if (availableMoves(snake, game, coordinates, move)) {
                availableMoves.add(move);
            }
            if (availablePowerUpOrCookieMoves(snake, game, coordinates, move)) {
                cookieOrPowerUpMoves.add(move);
            }

        }

        if (availableMoves.size() > 0) {
            int randomIndex = (int) (Math.random() * availableMoves.size());
            String randomMove = availableMoves.get(randomIndex);
            double probabilityChangeMovement = (Math.random());
            boolean straightPossible = availableMoves.contains(snake.getDirection()) && !isGoingToPickUpPowerDown(game, coordinates, snake.getDirection());

            if (!straightPossible) {
                String oppositeCurve = ("RIGHTCURVE".equals(snake.getPreviousCurve())) ? "LEFTCURVE" : "RIGHTCURVE";

                String nextMove = mapCurveToDirection(oppositeCurve, snake.getDirection());
                if (availableMoves.contains(nextMove)) {
                    snake.setPreviousCurve(oppositeCurve);
                    snake.setDirection(nextMove);
                } else {
                    snake.setDirection(randomMove);
                }
            } else if (cookieOrPowerUpMoves.size() > 0) {
                int randomIndexCookie = (int) (Math.random() * cookieOrPowerUpMoves.size());
                snake.setPreviousCurve(
                        mapTwoDirectionsToCurve(
                                snake,
                                snake.getDirection(),
                                cookieOrPowerUpMoves.get(randomIndexCookie)));
                snake.setDirection(cookieOrPowerUpMoves.get(randomIndexCookie));
            } else if (probabilityChangeMovement < 0.15 && !isGoingToPickUpPowerDown(game, coordinates, randomMove)) {
                snake.setPreviousCurve(
                        mapTwoDirectionsToCurve(
                            snake,
                            snake.getDirection(), 
                            randomMove));
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

    private boolean availablePowerUpOrCookieMoves(Snake snake, Game game, int[][] coordinates, String move) {
        int[] newHead = newHeadHelper(move, coordinates);

        if (newHead[0] < 0 || newHead[0] > WIDTH || newHead[1] < 0 || newHead[1] > HEIGHT) {
            return false;
        }
        for (Item item : game.getItems()) {
            if (item instanceof Cookie || item instanceof GoldenCookie || item instanceof Multiplier) { 
                int[] cookiePosition = item.getPosition();

                if (newHead[0] == cookiePosition[0] && newHead[1] == cookiePosition[1]) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isGoingToPickUpPowerDown(Game game, int[][] coordinates, String move) {
        int[] newHead = newHeadHelper(move, coordinates);

        for (Item item : game.getItems()) {
            if ((item instanceof Divider) || (item instanceof ReverseControl)) {
                int[][] powerupFourPositions;
                if (item instanceof Divider) {
                    powerupFourPositions = ((Divider) item).getFourPositions();
                    for (int[] coordinate : powerupFourPositions) {
                        if (newHead[0] == coordinate[0] && newHead[1] == coordinate[1]) {
                            return true;
                        }
                    }

                } else if (item instanceof ReverseControl) {
                    powerupFourPositions = ((ReverseControl) item).getFourPositions();
                    for (int[] coordinate : powerupFourPositions) {
                        if (newHead[0] == coordinate[0] && newHead[1] == coordinate[1]) {
                            return true;
                        }
                    }
                }

            }
        }
        return false;
    }

    private int[] newHeadHelper(String move, int[][] coordinates) {
        int[] newHead;
        if (move.equals("UP")) {
            newHead = new int[] { coordinates[0][0], coordinates[0][1] - 1 };
        } else if (move.equals("DOWN")) {
            newHead = new int[] { coordinates[0][0], coordinates[0][1] + 1 };
        } else if (move.equals("LEFT")) {
            newHead = new int[] { coordinates[0][0] - 1, coordinates[0][1] };
        } else if (move.equals("RIGHT")) {
            newHead = new int[] { coordinates[0][0] + 1, coordinates[0][1] };
        } else {
            throw new IllegalArgumentException("Invalid direction: " + move);
        }
        return newHead;
    }

    private String mapTwoDirectionsToCurve(Snake snake, String direction1, String direction2) {
        if (direction1.equals("UP") && direction2.equals("LEFT")) {
            return "LEFTCURVE";
        } else if (direction1.equals("UP") && direction2.equals("RIGHT")) {
            return "RIGHTCURVE";
        } else if (direction1.equals("DOWN") && direction2.equals("LEFT")) {
            return "RIGHTCURVE";
        } else if (direction1.equals("DOWN") && direction2.equals("RIGHT")) {
            return "LEFTCURVE";
        } else if (direction1.equals("LEFT") && direction2.equals("UP")) {
            return "RIGHTCURVE";
        } else if (direction1.equals("LEFT") && direction2.equals("DOWN")) {
            return "LEFTCURVE";
        } else if (direction1.equals("RIGHT") && direction2.equals("UP")) {
            return "LEFTCURVE";
        } else if (direction1.equals("RIGHT") && direction2.equals("DOWN")) {
            return "RIGHTCURVE";
        }
        return snake.getPreviousCurve();
    }

    private String mapCurveToDirection(String curve, String currentDirection) {
        if (curve.equals("LEFTCURVE")) {
            if (currentDirection.equals("UP")) {
                return "LEFT";
            } else if (currentDirection.equals("DOWN")) {
                return "RIGHT";
            } else if (currentDirection.equals("LEFT")) {
                return "DOWN";
            } else if (currentDirection.equals("RIGHT")) {
                return "UP";
            }
        } else if (curve.equals("RIGHTCURVE")) {
            if (currentDirection.equals("UP")) {
                return "RIGHT";
            } else if (currentDirection.equals("DOWN")) {
                return "LEFT";
            } else if (currentDirection.equals("LEFT")) {
                return "UP";
            } else if (currentDirection.equals("RIGHT")) {
                return "DOWN";
            }
        }
        return null;
    }
}
