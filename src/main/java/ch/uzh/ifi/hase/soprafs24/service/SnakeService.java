package ch.uzh.ifi.hase.soprafs24.service;

import javax.transaction.Transactional;

import ch.uzh.ifi.hase.soprafs24.entity.Item;
import ch.uzh.ifi.hase.soprafs24.entity.Powerdowns.Divider;
import ch.uzh.ifi.hase.soprafs24.entity.Powerups.GoldenCookie;
import ch.uzh.ifi.hase.soprafs24.entity.Powerups.Multiplier;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.handler.WebSocketHandler;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Snake;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@Transactional
public class SnakeService {

    private final UserRepository userRepository;

/////
    private final ObjectMapper mapper = new ObjectMapper();

    private final ApplicationContext applicationContext;

/////
    public SnakeService(UserService userService, UserRepository userRepository, ApplicationContext applicationContext) {
        this.userRepository = userRepository;
        //
        this.applicationContext = applicationContext;
        //
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
        // check if goldenCookie active
        List<Item> effectsCopy = new ArrayList<>(snake.getEffects());
        for (Item effect : effectsCopy) {
            if (effect instanceof GoldenCookie){((GoldenCookie) effect).growGolden(snake);}
            if (effect instanceof Divider){((Divider) effect).checkIfActive(snake);}
        }

        // Überprüfe auf Cookie-Kollision
        boolean ateCookie = checkCookieCollision(snake);
        if (ateCookie) {
            snake.addGrowCount();
            for (Item effect : effectsCopy) {
                if (effect instanceof Multiplier){((Multiplier) effect).multiplyCookie(snake);}
            }
        }
        // Neues Koordinaten-Array erstellen
        int[][] newCoordinates;
        if (snake.getGrowCount()>=1) {
            // Wenn ein Cookie gegessen wurde, behalten wir den Schwanz (Schlange wächst)
            newCoordinates = new int[coordinates.length + 1][];
        } else {
            // Normalfall: Der Schwanz wird gelöscht (gleiche Länge)
            newCoordinates = new int[coordinates.length][];
        }
        newCoordinates[0] = newHead;
        if (snake.getGrowCount()>=1) {
            System.arraycopy(coordinates, 0, newCoordinates, 1, coordinates.length);
            snake.removeGrowCount();
        }else{
        System.arraycopy(coordinates, 0, newCoordinates, 1, coordinates.length-1);
        }
        snake.setCoordinates(newCoordinates);

    }

    private boolean checkCookieCollision(Snake snake) {
        // Position des Kopfes der Schlange abrufen
        int[] head = snake.getCoordinates()[0]; // Der Kopf ist der erste Punkt im Koordinatenarray

        // Durchlaufen der Items im Spiel (Game)
        for (Item item : snake.getGame().getItems()) {
            // Prüfen, ob das aktuelle Item vom Typ "cookie" ist
            if ("cookie".equals(item.getType())) {
                int[] cookiePosition = item.getPosition(); // Cookie-Position abrufen

                // Prüfen, ob die Kopfposition mit der Cookie-Position übereinstimmt
                if (head[0] == cookiePosition[0] && head[1] == cookiePosition[1]) {
                    // Kollision -> Entfernt den Cookie aus dem Spiel
                    snake.getGame().getItems().remove(item);
                    return true; // Eine Kollision wurde festgestellt
                }
            }
        }

        // Keine Kollision gefunden
        return false;
    }

    public boolean checkCollision(Snake snake, Game game) {
        if (snake.getCoordinates().length == 0) {
            return false;
        }

        Lobby lobby = game.getLobby();
        boolean isSoloLobby = lobby.isSolo();
        
        int[] head = snake.getCoordinates()[0];
        //check for collision with walls
        if (head[0] < 0 || head[0] >= 30 || head[1] < 0 || head[1] >= 25) {
            // updating length-PR
            String username = snake.getUsername();
            User victim = userRepository.findByUsername(username);
            if (!isSoloLobby) {
                if (victim.getLengthPR() < snake.getCoordinates().length) {
                    victim.setLengthPR(snake.getCoordinates().length);
                    userRepository.save(victim);
                    userRepository.flush();
                }
            }
            return true;
        }
        // check collision with other snakes
        for (Snake otherSnake : game.getSnakes()) {
            //other snake is already dead
            if (otherSnake.getCoordinates().length == 0) {
                continue;
            }
            // check if the snake is colliding with itself or someone else
            for (int i = 0; i < otherSnake.getCoordinates().length; i++) {
                // dont check your own head with your own head
                if (snake == otherSnake && i == 0) {
                    continue;
                }
                if (Arrays.equals(head, otherSnake.getCoordinates()[i])) {
                    // updating kill count for the snake who killed this snake
                    if (snake != otherSnake) { // if it did not collide in itself
                        String snakeName = otherSnake.getUsername();
                        User killer = userRepository.findByUsername(snakeName);
                        killer.setKills(killer.getKills()+1);
                    }
                    // updating length-PR
                    String username = snake.getUsername();
                    User victim = userRepository.findByUsername(username);
                    if (!isSoloLobby) {
                        if (victim.getLengthPR() < snake.getCoordinates().length) {
                            victim.setLengthPR(snake.getCoordinates().length);
                            userRepository.save(victim);
                            userRepository.flush();
                        }
                    }

                    sendDeathMsg(snake, game);

                    return true;
                }
            }
        }
        return false;
    }

    //JUST FOR DEBUGGING

    private void sendDeathMsg(Snake snake, Game game) {
        ///// TO REMOVE
        ObjectNode message = mapper.createObjectNode();
        message.put("type", "playerDied");
        message.put("coordinates",mapper.valueToTree(snake.getCoordinates()));
        WebSocketHandler webSocketHandler = getWebSocketHandler();
        try {
            webSocketHandler.broadcastToLobby(game.getLobby().getId(), message);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        /////
    }
    private WebSocketHandler getWebSocketHandler() {
        return applicationContext.getBean(WebSocketHandler.class);
    }
    //////

}
