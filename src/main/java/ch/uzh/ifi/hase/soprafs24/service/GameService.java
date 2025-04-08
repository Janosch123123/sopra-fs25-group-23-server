package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.*;
import ch.uzh.ifi.hase.soprafs24.handler.WebSocketHandler;
import ch.uzh.ifi.hase.soprafs24.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.*;

import static ch.uzh.ifi.hase.soprafs24.service.LobbyService.putGameToLobby;

@Service
@Transactional
public class GameService {

    private final LobbyRepository lobbyRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final SnakeSerivce snakeService;
    private final ObjectMapper mapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(GameService.class);
    
    // Replace direct WebSocketHandler dependency with ApplicationContext
    private final ApplicationContext applicationContext;

    @Autowired
    public GameService(LobbyRepository lobbyRepository, UserRepository userRepository,
                      UserService userService, ApplicationContext applicationContext, SnakeSerivce snakeService) {
        this.lobbyRepository = lobbyRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.applicationContext = applicationContext;
        this.snakeService = snakeService;
    }
    
    // Add a method to get WebSocketHandler lazily when needed
    private WebSocketHandler getWebSocketHandler() {
        return applicationContext.getBean(WebSocketHandler.class);
    }

    public Game createGame(Lobby lobby) {
        // Ensure we are working with a managed entity within the current transaction
        Lobby managedLobby = lobbyRepository.findById(lobby.getId())
                .orElseThrow(() -> new IllegalArgumentException("Lobby not found with ID: " + lobby.getId()));
        
        Game game = new Game();
        game.setLobby(managedLobby);
        putGameToLobby(game, managedLobby);
        
        List<Long> playersId = managedLobby.getParticipantIds();
        
        // Log player IDs to debug
        logger.info("Creating game with {} players", playersId.size());
        addSnakesToBoard(game, playersId);
        
        List<Item> gameItems = new ArrayList<>();
        gameItems.add(new Item(new int[]{12, 12}, "cookie"));
        gameItems.add(new Item(new int[]{8, 13}, "cookie"));
        gameItems.add(new Item(new int[]{2, 17}, "cookie"));
        game.setItems(gameItems);

        return game;
    }

    private void addSnakesToBoard(Game game, List<Long> playersId){
        for (Long playerId : playersId) {
            logger.info("Adding snake for player: {}", playerId);
            
            int index = playersId.indexOf(playerId);
            
            int[][] coordinate;
            coordinate = switch (index % 4) {
                case 0 -> new int[][]{{4, 4}, {3, 4}, {2, 4}};
                case 1 -> new int[][]{{25, 20}, {26, 20}, {27, 20}};
                case 2 -> new int[][]{{25, 4}, {25, 3}, {25, 2}};
                case 3 -> new int[][]{{4, 20}, {4, 21}, {4, 22}};
                default -> new int[][]{{4, 4}, {3, 4}, {2, 4}}; // Default case
            };
            String direction = switch (index % 4) {
                case 0 -> "RIGHT";
                case 1 -> "LEFT";
                case 2 -> "UP";
                case 3 -> "DOWN";
                default -> "RIGHT";
            };
            
            
            Snake snake = new Snake();
            snake.setUserId(playerId);
            snake.setUsername(userService.getUserById(playerId).getUsername());
            snake.setDirection(direction);
            snake.setCoordinates(coordinate);
            snake.setLength(2);
            snake.setHead(new int[]{2, 2});
            snake.setTail(new int[]{2, 1});
            game.addSnake(snake);
        }
    }

    public void start(Game game) {
        new Thread(() -> { // Startet die Game-Loop in einem eigenen Thread
            while (!game.isGameOver()) {
                updateGameState(game);
                game.setTimestamp(game.getTimestamp()-1);// Aktualisiert den Spielzustand (Bewegungen, Kollisionsprüfung)
                try {
                    broadcastGameState(game); // Sendet Spielzustand an alle WebSocket-Clients
                }
                catch (IOException e) {
                    throw new RuntimeException(e);
                }
                try {
                    Thread.sleep(2000); // Wartezeit für den nächsten Loop (z. B. 100ms pro Frame)
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            endGame(game); // Cleanup, wenn das Spiel vorbei ist
        }).start();
    }

    private void broadcastGameState(Game game) throws IOException {

        logger.info("Broadcasting game state for game: {}", game.getGameId());
        ObjectNode message = mapper.createObjectNode();
        message.put("type", "gameState");
        message.put("timestamp", Math.round(game.getTimestamp()));
        // Map mit Username als Key und Snake-Informationen als Value erstellen
        Map<String, Object> snakesDictionary = new HashMap<>();
        for (Snake snake : game.getSnakes()) {
            String username = snake.getUsername(); // Benutzername als Key
            snakesDictionary.put(username, snake.getCoordinates());
        }
        // Füge die strukturierte Map dem JSON-Objekt hinzu
        message.set("snakes", mapper.valueToTree(snakesDictionary));
// Extrahiere die Cookies aus der Items-Liste (alle Items mit type "cookie")
        List<int[]> cookiePositions = new ArrayList<>();
        for (Item item : game.getItems()) {
            if ("cookie".equals(item.getType())) { // Prüfen, ob Item-Typ "cookie" ist
                cookiePositions.add(item.getPosition()); // Position hinzufügen
            }
        }
        // Füge die Cookie-Positionen zu den JSON-Daten hinzu
        message.set("cookies", mapper.valueToTree(cookiePositions));


        // Get WebSocketHandler lazily only when needed
        WebSocketHandler webSocketHandler = getWebSocketHandler();
        webSocketHandler.broadcastToLobby(game.getLobby().getId(), message);
//
//        for (Long playerId : game.getLobby().getParticipantIds()) {
//            WebSocketSession session = webSocketHandler.getSessionByUserId(playerId);
//            try {
//                if (session != null && session.isOpen()) {
//                    session.sendMessage(new TextMessage(message.toString()));
//                }
//            } catch (IOException e) {
//                logger.error("Error sending game update to user {}", playerId, e);
//            }
//        }
    }

    private void endGame(Game game) {
        // was soll da passieren?
    }

    private void updateGameState(Game game) {
//        List<Snake> toRemove = new ArrayList<>();
//        for (Snake snake : game.getSnakes()) {
//            if (snake.getCoordinates() == null) {
//                continue; // already dead
//            }
//            snake.moveSnake();
//            if (Snake.checkCollision(snake, game)) {
//                toRemove.add(snake);
//            }
//        }
//        for (Snake snake : toRemove) {
//            game.getSnakes().remove(snake);
//        }
        
        for (Snake snake : game.getSnakes()) {
            if (snake.getCoordinates().length == 0) {
                continue; // already dead
            }
            snakeService.moveSnake(snake);

            if (snakeService.checkCollision(snake, game)) {
                // Snake has collided with another snake
                logger.info("Collision detected for snake: {}", snake.getUserId());
                
                continue;
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


