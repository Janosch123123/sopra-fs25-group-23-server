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
import java.math.BigDecimal;
import java.util.*;

import static ch.uzh.ifi.hase.soprafs24.service.LobbyService.putGameToLobby;

@Service
@Transactional
public class GameService {

    private final LobbyRepository lobbyRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final SnakeService snakeService;
    private final ObjectMapper mapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(GameService.class);
    
    // Replace direct WebSocketHandler dependency with ApplicationContext
    private final ApplicationContext applicationContext;

    @Autowired
    public GameService(LobbyRepository lobbyRepository, UserRepository userRepository,
                      UserService userService, ApplicationContext applicationContext, SnakeService snakeService) {
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

    public Game createGame(Lobby lobby, String cookieSpawnRate) {
        // Ensure we are working with a managed entity within the current transaction
        Lobby managedLobby = lobbyRepository.findById(lobby.getId())
                .orElseThrow(() -> new IllegalArgumentException("Lobby not found with ID: " + lobby.getId()));
        
        Game game = new Game();
        switch (cookieSpawnRate) {
            case "Slow" -> game.setCookieSpawnRate(0.1);
            case "Medium" -> game.setCookieSpawnRate(0.3);
            case "Fast" -> game.setCookieSpawnRate(0.5);
            default -> game.setCookieSpawnRate(0.3); // Default to medium if invalid input
        }
        game.setLobby(managedLobby);
        putGameToLobby(game, managedLobby.getId());
        
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
                case 2 -> "DOWN";
                case 3 -> "UP";
                default -> "RIGHT";
            };
            
            
            Snake snake = new Snake();
            snake.setGame(game);
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
            try {startCountdown(game, 5);}
            catch (IOException e) {throw new RuntimeException(e);}
            while (!game.isGameOver()) {
                updateGameState(game);
                game.setTimestamp(game.getTimestamp()-0.20f);// Aktualisiert den Spielzustand (Bewegungen, Kollisionsprüfung)
                try {
                    broadcastGameState(game); // Sendet Spielzustand an alle WebSocket-Clients
                }
                catch (IOException e) {
                    throw new RuntimeException(e);}
                try {
                    Thread.sleep(200); // Wartezeit für den nächsten Loop (z. B. 100ms pro Frame)
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;}
            }
            try {endGame(game); } // send winner to FE etc//
            catch (IOException e) {throw new RuntimeException(e);}
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

    public void endGame(Game game) throws IOException {
        rankRemainingPlayers(game);

        // update winning stats
        String winnerName = game.getLeaderboard().get(0);
        if (winnerName != null) {
            User winner = userRepository.findByUsername(winnerName);
            winner.setWins(winner.getWins()+1);
            userRepository.save(winner);
            logger.info("User {} won the game!", winnerName);
        }
        //update level stats
        for (Long playerId : game.getLobby().getParticipantIds()) {
            Optional<User> currentUser = userRepository.findById(playerId);
            if (currentUser.isPresent()) {
                User user = currentUser.get();
                user.setPlayedGames(user.getPlayedGames()+1);
                int newLevel = user.getWins()/2 + user.getKills()/4 + 1;
                user.setLevel(newLevel);
                userRepository.save(user);
                logger.info("User {} reached level {}!", user.getUsername(), user.getLevel());
            } else {
                logger.error("User {} not found!", playerId);
            }
        }
        List<String> leaderboard = game.getLeaderboard();
        logger.info("Ending game: {}", game.getGameId());
        ObjectNode message = mapper.createObjectNode();
        message.put("type", "gameEnd");
        message.put("rank", mapper.valueToTree(leaderboard));
        message.put("reason","Last survivor");
        WebSocketHandler webSocketHandler = getWebSocketHandler();
        webSocketHandler.broadcastToLobby(game.getLobby().getId(), message);
    }

    private void updateGameState(Game game) {
        List<Snake> aliveSnakes = new ArrayList<>();
        for (Snake snake : game.getSnakes()) {
            if (snake.getCoordinates().length == 0) {
                continue; // already dead
            }
            aliveSnakes.add(snake);
            updateSnakeDirection(snake); // checks for direction changes in queue
            snakeService.moveSnake(snake);

            if (snakeService.checkCollision(snake, game)) {
                // Snake has collided with another snake
                logger.info("Collision detected for snake: {}", snake.getUserId());
                spawnCookiesOnDeath(snake, game);
                snake.setCoordinates(new int[0][0]); // Set coordinates to empty to mark as dead
                game.addLeaderboardEntry(snake.getUsername());
            }
        }
        if (aliveSnakes.size() == 1) {
            Snake winnerSnake = aliveSnakes.get(0);
            game.setWinner(winnerSnake.getUsername());
        }



        // Spawne ggf. neue Items (mit 25% Chance)
        Random random = new Random();
        double chance = random.nextDouble();
        if (chance < game.getCookieSpawnRate()) { // 20 % Chance
            spawnItem(game);
        }
    }

    private void spawnCookiesOnDeath(Snake snake, Game game) {
        // Spawn cookies at the coordinates of the dead snake
        int[][] coordinates = snake.getCoordinates();
        for (int[] coordinate : coordinates) {
            boolean alreadyInGame = false;
            for (Item itemAlreadyInGame : game.getItems()) {
                if (Arrays.equals(itemAlreadyInGame.getPosition(), coordinate)) {
                    alreadyInGame = true;
                    break;
                }
            }
            if (!alreadyInGame) {
                Item item = new Item(coordinate, "cookie");
                game.addItem(item);
            }
        }
    }

    private void spawnItem(Game game) {
        boolean occupied = true;
        Random random = new Random();
        int x = 0;
        int y = 0;
        while (occupied) {
            occupied = false;
            x = random.nextInt(30); // 1 bis 20
            y = random.nextInt(25); // 1 bis 20
            for (Snake snake : game.getSnakes()){
                for (int[] coordinate : snake.getCoordinates())
                    if (coordinate[0] == x && coordinate[1] == y) {
                        occupied = true;
                        break;
                }
            }
            for (Item item: game.getItems()) {
                if (item.getPosition()[0] == x && item.getPosition()[1] == y) {
                    occupied = true;
                    break;
                }
            }

        }
        Item item = new Item(new int[]{x, y}, "cookie");
        game.addItem(item);
    }

    public void respondToKeyInputs(Game game, User user, String direction) {
        for (Snake snake : game.getSnakes()) {
            if (snake.getUserId().equals(user.getId())) {
                if (direction.equals("UP") && snake.getDirection().equals("DOWN") && snake.getDirectionQueue().isEmpty()) {}
                else if (direction.equals("DOWN") && snake.getDirection().equals("UP") && snake.getDirectionQueue().isEmpty()) {}
                else if (direction.equals("LEFT") && snake.getDirection().equals("RIGHT") && snake.getDirectionQueue().isEmpty()) {}
                else if (direction.equals("RIGHT") && snake.getDirection().equals("LEFT") && snake.getDirectionQueue().isEmpty()) {}

                else if (direction.equals("DOWN") && snake.getDirection().equals("DOWN") && snake.getDirectionQueue().isEmpty()) {}
                else if (direction.equals("UP") && snake.getDirection().equals("UP") && snake.getDirectionQueue().isEmpty()) {}
                else if (direction.equals("LEFT") && snake.getDirection().equals("LEFT") && snake.getDirectionQueue().isEmpty()) {}
                else if (direction.equals("RIGHT") && snake.getDirection().equals("RIGHT") && snake.getDirectionQueue().isEmpty()) {}
                else {snake.addDirectionQueue(direction);}
            }
        }
    }

    public void updateSnakeDirection(Snake snake) {
        if (snake.getDirectionQueue().size() > 0) {
            String newDirection = snake.popDirectionQueue();
            if (newDirection.equals("UP") && !snake.getDirection().equals("DOWN")) {
                snake.setDirection(newDirection);
            } else if (newDirection.equals("DOWN") && !snake.getDirection().equals("UP")) {
                snake.setDirection(newDirection);
            } else if (newDirection.equals("LEFT") && !snake.getDirection().equals("RIGHT")) {
                snake.setDirection(newDirection);
            } else if (newDirection.equals("RIGHT") && !snake.getDirection().equals("LEFT")) {
                snake.setDirection(newDirection);
            }
        }
    }
    private void startCountdown(Game game, int seconds) throws IOException {
        for (int i = seconds; i > 0; i--) {
            logger.info("Broadcasting countdown for game: {}", game.getGameId());
            ObjectNode message = mapper.createObjectNode();
            message.put("type", "preGame");
            message.put("countdown", i);
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

            WebSocketHandler webSocketHandler = getWebSocketHandler();
            webSocketHandler.broadcastToLobby(game.getLobby().getId(), message);

            // Warte 1 Sekunde vor der nächsten Iteration
            try {
                Thread.sleep(1000); // 1000 Millisekunden = 1 Sekunde
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Countdown wurde unterbrochen: {}", e.getMessage());
                break; // Beende die Schleife bei Unterbrechung
            }
        }
    }
    public static void rankRemainingPlayers(Game game) {
        List<Snake> remainingPlayers = new ArrayList<>();

        for (Snake snake : game.getSnakes()) {
            if (snake.getCoordinates().length == 0) {
                continue;
            } else {
                remainingPlayers.add(snake);
            }
        }

        // Sortiere remainingPlayers nach der Größe von snake.getCoordinates()
        remainingPlayers.sort((s1, s2) -> Integer.compare(s2.getCoordinates().length, s1.getCoordinates().length));
        // Liste umkehren
        Collections.reverse(remainingPlayers);
        for (Snake player : remainingPlayers){
            game.addLeaderboardEntry(player.getUsername());
        }

    }
}


