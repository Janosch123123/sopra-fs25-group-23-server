package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.controller.WebSocketHandler;
import ch.uzh.ifi.hase.soprafs24.entity.*;
import ch.uzh.ifi.hase.soprafs24.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.List;
import java.util.Random;

@Service
@Transactional
public class GameService {

    private final LobbyRepository lobbyRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final ObjectMapper mapper = new ObjectMapper(); // ObjectMapper instanziiert
    private static final Logger logger = LoggerFactory.getLogger(GameService.class); // Logger initialisiert
    private final WebSocketHandler webSocketHandler;

    @Autowired
    public GameService(LobbyRepository lobbyRepository, UserRepository userRepository, UserService userService, WebSocketHandler webSocketHandler) {
        this.lobbyRepository = lobbyRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.webSocketHandler = webSocketHandler;
    }

    public Game createGame(Lobby lobby) {
        // Ensure we are working with a managed entity within the current transaction
        Lobby managedLobby = lobbyRepository.findById(lobby.getId())
                .orElseThrow(() -> new IllegalArgumentException("Lobby not found with ID: " + lobby.getId()));
        
        Game game = new Game();
        game.setLobby(managedLobby);
        
        List<Long> playersId = managedLobby.getParticipantIds();
        for (Long playerId : playersId) {
            Snake snake = new Snake();
            snake.setUserId(playerId);
            snake.setDirection("DOWN");
            snake.setCoordinates(new int[][]{{2, 2}, {2, 1}});
            snake.setLength(2);
            snake.setHead(new int[]{2, 2});
            snake.setTail(new int[]{2, 1});
            game.addSnake(snake);
        }
        
        Item item1 = new Item(new int[]{12, 12}, "cookie");
        Item item2 = new Item(new int[]{8, 13}, "cookie");
        Item item3 = new Item(new int[]{2, 17}, "cookie");
        game.setItems(List.of(item1, item2, item3));

        return game;
    }

    public void start(Game game) {
        new Thread(() -> { // Startet die Game-Loop in einem eigenen Thread
            while (!game.isGameOver()) {
                updateGameState(game); // Aktualisiert den Spielzustand (Bewegungen, Kollisionsprüfung)
                broadcastGameState(game); // Sendet Spielzustand an alle WebSocket-Clients
                try {
                    Thread.sleep(100); // Wartezeit für den nächsten Loop (z. B. 100ms pro Frame)
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            endGame(game); // Cleanup, wenn das Spiel vorbei ist
        }).start();
    }

    private void broadcastGameState(Game game) {
        ObjectNode message = mapper.createObjectNode();
        message.put("type", "gameState");
        message.put("gameId", game.getGameId());
        message.set("snakes", mapper.valueToTree(game.getSnakes()));
        message.set("items", mapper.valueToTree(game.getItems()));

        for (Long playerId : game.getLobby().getParticipantIds()) {
            WebSocketSession session = webSocketHandler.getSessionByUserId(playerId);
            try {
                session.sendMessage(new TextMessage(message.toString()));
            } catch (IOException e) {
                logger.error("Error sending game update to user {}", playerId, e);
            }
        }
    }


    private void endGame(Game game) {
        // was soll da passieren?
    }

    private void updateGameState(Game game) {
        for (Snake snake : game.getSnakes()) {
            // Bewege die Schlange gemäß ihrer Richtung
            snake.moveSnake();
            // Prüfe, ob die Schlange mit etwas kollidiert
            if (Snake.checkCollision(snake, game)) {
                snake.setCoordinates(null); // Snake stirbt
            }
        }
        // Spawne ggf. neue Items (mit 25% Chance)
        Random random = new Random();
        double chance = random.nextDouble();
        if (chance < 0.2) { // 20 % Chance
            spawnItem(game);
        }
    }

    private void spawnItem(Game game) {
        boolean occupied = true;
        Random random = new Random();
        int x = 0;
        int y = 0;
        while (occupied) {
            occupied = false;
            x = random.nextInt(20) + 1; // 1 bis 20
            y = random.nextInt(20) + 1; // 1 bis 20
            for (Snake snake : game.getSnakes()){
                for (int[] coordinate : snake.getCoordinates())
                    if (coordinate[0] == x && coordinate[1] == y) {
                        occupied = true;
                        break;
                }
            }

        }
        Item item = new Item(new int[]{x, y}, "cookie");
        game.addItem(item);
    }
}


