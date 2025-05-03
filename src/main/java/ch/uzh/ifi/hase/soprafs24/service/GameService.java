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

import java.io.IOException;
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

    // Flag for testing mode
    private boolean testMode = true;
    private int testSnakeIndex = -1;

    // Hard-coded game states
    private List<int[][]> hardCodedStates;
    private int currentStateIndex = 0;

    private String[] squareDirections = {"RIGHT", "UP", "LEFT", "DOWN"};
    private int currentDirectionIndex = 0;
    private int moveCounter = 0;

    private final ApplicationContext applicationContext;

    @Autowired
    public GameService(LobbyRepository lobbyRepository, UserRepository userRepository,
                       UserService userService, ApplicationContext applicationContext, SnakeService snakeService) {
        this.lobbyRepository = lobbyRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.applicationContext = applicationContext;
        this.snakeService = snakeService;
        initializeHardCodedStates();
    }

    /**
     * Initializes the hard-coded states for the test snake movement pattern.
     * Each state represents the snake positions for a frame of the game.
     * The snake moves 1 field at a time to create a smooth movement around a 4x4 square.
     */
    private void initializeHardCodedStates() {
        hardCodedStates = new ArrayList<>();
        
        // ----- TOP EDGE: Moving right from (13,11) to (16,11) -----
        // State 1: Starting position, head at (13,11)
        int[][] state1 = new int[5][2];
        state1[0] = new int[]{13, 11}; // head
        state1[1] = new int[]{13, 12};
        state1[2] = new int[]{13, 13};
        state1[3] = new int[]{13, 14};
        state1[4] = new int[]{14, 14}; // tail
        hardCodedStates.add(state1);
        
        // State 2: Move 1 step right, head at (14,11)
        int[][] state2 = new int[5][2];
        state2[0] = new int[]{14, 11}; // head
        state2[1] = new int[]{13, 11};
        state2[2] = new int[]{13, 12};
        state2[3] = new int[]{13, 13};
        state2[4] = new int[]{13, 14}; // tail
        hardCodedStates.add(state2);
        
        // State 3: Move 1 step right, head at (15,11)
        int[][] state3 = new int[5][2];
        state3[0] = new int[]{15, 11}; // head
        state3[1] = new int[]{14, 11};
        state3[2] = new int[]{13, 11};
        state3[3] = new int[]{13, 12};
        state3[4] = new int[]{13, 13}; // tail
        hardCodedStates.add(state3);
        
        // State 4: Move 1 step right, head at (16,11)
        int[][] state4 = new int[5][2];
        state4[0] = new int[]{16, 11}; // head
        state4[1] = new int[]{15, 11};
        state4[2] = new int[]{14, 11};
        state4[3] = new int[]{13, 11};
        state4[4] = new int[]{13, 12}; // tail
        hardCodedStates.add(state4);
        
        // ----- RIGHT EDGE: Moving down from (16,11) to (16,14) -----
        // State 5: Move 1 step down, head at (16,12)
        int[][] state5 = new int[5][2];
        state5[0] = new int[]{16, 12}; // head
        state5[1] = new int[]{16, 11};
        state5[2] = new int[]{15, 11};
        state5[3] = new int[]{14, 11};
        state5[4] = new int[]{13, 11}; // tail
        hardCodedStates.add(state5);
        
        // State 6: Move 1 step down, head at (16,13)
        int[][] state6 = new int[5][2];
        state6[0] = new int[]{16, 13}; // head
        state6[1] = new int[]{16, 12};
        state6[2] = new int[]{16, 11};
        state6[3] = new int[]{15, 11};
        state6[4] = new int[]{14, 11}; // tail
        hardCodedStates.add(state6);
        
        // State 7: Move 1 step down, head at (16,14)
        int[][] state7 = new int[5][2];
        state7[0] = new int[]{16, 14}; // head
        state7[1] = new int[]{16, 13};
        state7[2] = new int[]{16, 12};
        state7[3] = new int[]{16, 11};
        state7[4] = new int[]{15, 11}; // tail
        hardCodedStates.add(state7);
        
        // ----- BOTTOM EDGE: Moving left from (16,14) to (13,14) -----
        // State 8: Move 1 step left, head at (15,14)
        int[][] state8 = new int[5][2];
        state8[0] = new int[]{15, 14}; // head
        state8[1] = new int[]{16, 14};
        state8[2] = new int[]{16, 13};
        state8[3] = new int[]{16, 12};
        state8[4] = new int[]{16, 11}; // tail
        hardCodedStates.add(state8);
        
        // State 9: Move 1 step left, head at (14,14)
        int[][] state9 = new int[5][2];
        state9[0] = new int[]{14, 14}; // head
        state9[1] = new int[]{15, 14};
        state9[2] = new int[]{16, 14};
        state9[3] = new int[]{16, 13};
        state9[4] = new int[]{16, 12}; // tail
        hardCodedStates.add(state9);
        
        // State 10: Move 1 step left, head at (13,14)
        int[][] state10 = new int[5][2];
        state10[0] = new int[]{13, 14}; // head
        state10[1] = new int[]{14, 14};
        state10[2] = new int[]{15, 14};
        state10[3] = new int[]{16, 14};
        state10[4] = new int[]{16, 13}; // tail
        hardCodedStates.add(state10);
        
        // ----- LEFT EDGE: Moving up from (13,14) to (13,11) -----
        // State 11: Move 1 step up, head at (13,13)
        int[][] state11 = new int[5][2];
        state11[0] = new int[]{13, 13}; // head
        state11[1] = new int[]{13, 14};
        state11[2] = new int[]{14, 14};
        state11[3] = new int[]{15, 14};
        state11[4] = new int[]{16, 14}; // tail
        hardCodedStates.add(state11);
        
        // State 12: Move 1 step up, head at (13,12)
        int[][] state12 = new int[5][2];
        state12[0] = new int[]{13, 12}; // head
        state12[1] = new int[]{13, 13};
        state12[2] = new int[]{13, 14};
        state12[3] = new int[]{14, 14};
        state12[4] = new int[]{15, 14}; // tail
        hardCodedStates.add(state12);
        
    }

    private WebSocketHandler getWebSocketHandler() {
        return applicationContext.getBean(WebSocketHandler.class);
    }

    public Game createGame(Lobby lobby, String cookieSpawnRate) {
        Lobby managedLobby = lobbyRepository.findById(lobby.getId())
                .orElseThrow(() -> new IllegalArgumentException("Lobby not found with ID: " + lobby.getId()));

        Game game = new Game();
        switch (cookieSpawnRate) {
            case "Slow" -> game.setCookieSpawnRate(0.1);
            case "Medium" -> game.setCookieSpawnRate(0.3);
            case "Fast" -> game.setCookieSpawnRate(0.5);
            case "sugarRush" -> {
                game.setCookieSpawnRate(0.0);
                activateSugarRush(game);
            }
            default -> game.setCookieSpawnRate(0.3);
        }
        game.setLobby(managedLobby);
        putGameToLobby(game, managedLobby.getId());

        List<Long> playersId = managedLobby.getParticipantIds();

        logger.info("Creating game with {} players", playersId.size());
        addSnakesToBoard(game, playersId);

        if (testMode && game.getSnakes().size() > 0) {
            setupTestSnake(game);
        }

        return game;
    }

    private void setupTestSnake(Game game) {
        testSnakeIndex = 0;
        Snake testSnake = game.getSnakes().get(testSnakeIndex);

        // Set the initial state from our hard-coded states
        int[][] initialState = hardCodedStates.get(0);
        testSnake.setCoordinates(initialState);
        testSnake.setLength(initialState.length);
        testSnake.setHead(initialState[0]);
        testSnake.setTail(initialState[initialState.length - 1]);

        // Direction based on initial state
        testSnake.setDirection("RIGHT");

        logger.info("Test snake set up for {} with hard-coded states", testSnake.getUsername());
    }

    private void activateSugarRush(Game game) {
        for (int x = 0; x <= 29; x++) {
            for (int y = 0; y <= 24; y++) {
                game.addItem(new Item(new int[]{x, y}, "cookie"));
            }
        }
    }

    private void addSnakesToBoard(Game game, List<Long> playersId) {
        for (Long playerId : playersId) {
            logger.info("Adding snake for player: {}", playerId);

            int index = playersId.indexOf(playerId);

            int[][] coordinate;
            coordinate = switch (index % 4) {
                case 0 -> new int[][]{{4, 4}, {3, 4}, {2, 4}};
                case 1 -> new int[][]{{25, 20}, {26, 20}, {27, 20}};
                case 2 -> new int[][]{{25, 4}, {25, 3}, {25, 2}};
                case 3 -> new int[][]{{4, 20}, {4, 21}, {4, 22}};
                default -> new int[][]{{4, 4}, {3, 4}, {2, 4}};
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
        new Thread(() -> {
            try {
                startCountdown(game, 5);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            while (!game.isGameOver()) {
                updateGameState(game);
                game.setTimestamp(game.getTimestamp() - 0.20f);
                try {
                    broadcastGameState(game);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            try {
                endGame(game);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    private void broadcastGameState(Game game) throws IOException {
        logger.info("Broadcasting game state for game: {}", game.getGameId());
        ObjectNode message = mapper.createObjectNode();
        message.put("type", "gameState");
        message.put("timestamp", Math.round(game.getTimestamp()));

        Map<String, Object> snakesDictionary = new HashMap<>();
        for (Snake snake : game.getSnakes()) {
            String username = snake.getUsername();
            snakesDictionary.put(username, snake.getCoordinates());
        }
        message.set("snakes", mapper.valueToTree(snakesDictionary));

        List<int[]> cookiePositions = new ArrayList<>();
        for (Item item : game.getItems()) {
            if ("cookie".equals(item.getType())) {
                cookiePositions.add(item.getPosition());
            }
        }
        message.set("cookies", mapper.valueToTree(cookiePositions));

        WebSocketHandler webSocketHandler = getWebSocketHandler();
        webSocketHandler.broadcastToLobby(game.getLobby().getId(), message);
    }

    public void endGame(Game game) throws IOException {
        rankRemainingPlayers(game);

        String winnerName = game.getLeaderboard().get(0);
        if (winnerName != null) {
            User winner = userRepository.findByUsername(winnerName);
            winner.setWins(winner.getWins() + 1);
            userRepository.save(winner);
            logger.info("User {} won the game!", winnerName);
        }

        for (Long playerId : game.getLobby().getParticipantIds()) {
            Optional<User> currentUser = userRepository.findById(playerId);
            if (currentUser.isPresent()) {
                User user = currentUser.get();
                user.setPlayedGames(user.getPlayedGames() + 1);
                int newLevel = 1 + (user.getWins() / 2) + (user.getKills() / 4);
                user.setLevel(newLevel);
                userRepository.save(user);
                userRepository.flush();
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
        message.put("reason", "Last survivor");
        WebSocketHandler webSocketHandler = getWebSocketHandler();
        webSocketHandler.broadcastToLobby(game.getLobby().getId(), message);
    }

    private void updateGameState(Game game) {
        List<Snake> aliveSnakes = new ArrayList<>();
        for (Snake snake : game.getSnakes()) {
            if (snake.getCoordinates().length == 0) {
                continue;
            }

            if (testMode && game.getSnakes().indexOf(snake) == testSnakeIndex) {
                moveTestSnakeWithHardCodedStates(snake);
            } else {
                aliveSnakes.add(snake);
                updateSnakeDirection(snake);
                snakeService.moveSnake(snake);
            }

            if (snakeService.checkCollision(snake, game)) {
                logger.info("Collision detected for snake: {}", snake.getUserId());
                spawnCookiesOnDeath(snake, game);
                snake.setCoordinates(new int[0][0]);
                game.addLeaderboardEntry(snake.getUsername());
            }
        }
        if (aliveSnakes.size() == 1) {
            Snake winnerSnake = aliveSnakes.get(0);
            game.setWinner(winnerSnake.getUsername());
        }

        if (!testMode) {
            Random random = new Random();
            double chance = random.nextDouble();
            if (chance < game.getCookieSpawnRate()) {
                spawnItem(game);
            }
        }
    }

    /**
     * Moves the test snake by cycling through the hard-coded states.
     *
     * @param snake The test snake to be moved
     */
    private void moveTestSnakeWithHardCodedStates(Snake snake) {
        // Move to the next state in the cycle
        currentStateIndex = (currentStateIndex + 1) % hardCodedStates.size();
        
        // Set the snake coordinates to the current state
        int[][] newState = hardCodedStates.get(currentStateIndex);
        
        // Update the snake with the new state
        snake.setCoordinates(newState);
        snake.setHead(newState[0]);
        snake.setTail(newState[newState.length - 1]);
        
        // Update direction based on the head movement
        updateDirectionFromStates(snake, currentStateIndex);
        
        logger.info("Test snake moved to state {} at position [{},{}]", 
                   currentStateIndex, newState[0][0], newState[0][1]);
    }
    
    /**
     * Updates the snake's direction based on the current state index.
     *
     * @param snake The snake to update
     * @param stateIndex The current state index
     */
    private void updateDirectionFromStates(Snake snake, int stateIndex) {
        // States 1-4: Moving RIGHT along top edge
        if (stateIndex >= 0 && stateIndex <= 3) {
            snake.setDirection("RIGHT");
        }
        // States 5-7: Moving DOWN along right edge
        else if (stateIndex >= 4 && stateIndex <= 6) {
            snake.setDirection("DOWN");
        }
        // States 8-10: Moving LEFT along bottom edge
        else if (stateIndex >= 7 && stateIndex <= 9) {
            snake.setDirection("LEFT");
        }
        // States 11-13: Moving UP along left edge
        else {
            snake.setDirection("UP");
        }
    }

    private void spawnCookiesOnDeath(Snake snake, Game game) {
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
        if (testMode) return;

        boolean occupied = true;
        Random random = new Random();
        int x = 0;
        int y = 0;
        while (occupied) {
            occupied = false;
            x = random.nextInt(30);
            y = random.nextInt(25);
            for (Snake snake : game.getSnakes()) {
                for (int[] coordinate : snake.getCoordinates())
                    if (coordinate[0] == x && coordinate[1] == y) {
                        occupied = true;
                        break;
                    }
            }
            for (Item item : game.getItems()) {
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
            if (testMode && game.getSnakes().indexOf(snake) == testSnakeIndex) {
                logger.info("Ignoring input for test snake: {}", direction);
                continue;
            }

            if (snake.getUserId().equals(user.getId())) {
                if (direction.equals("UP") && snake.getDirection().equals("DOWN") && snake.getDirectionQueue().isEmpty()) {
                } else if (direction.equals("DOWN") && snake.getDirection().equals("UP") && snake.getDirectionQueue().isEmpty()) {
                } else if (direction.equals("LEFT") && snake.getDirection().equals("RIGHT") && snake.getDirectionQueue().isEmpty()) {
                } else if (direction.equals("RIGHT") && snake.getDirection().equals("LEFT") && snake.getDirectionQueue().isEmpty()) {
                } else if (direction.equals("DOWN") && snake.getDirection().equals("DOWN") && snake.getDirectionQueue().isEmpty()) {
                } else if (direction.equals("UP") && snake.getDirection().equals("UP") && snake.getDirectionQueue().isEmpty()) {
                } else if (direction.equals("LEFT") && snake.getDirection().equals("LEFT") && snake.getDirectionQueue().isEmpty()) {
                } else if (direction.equals("RIGHT") && snake.getDirection().equals("RIGHT") && snake.getDirectionQueue().isEmpty()) {
                } else {
                    snake.addDirectionQueue(direction);
                }
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
                String username = snake.getUsername();
                snakesDictionary.put(username, snake.getCoordinates());
            }
            message.set("snakes", mapper.valueToTree(snakesDictionary));

            List<int[]> cookiePositions = new ArrayList<>();
            for (Item item : game.getItems()) {
                if ("cookie".equals(item.getType())) {
                    cookiePositions.add(item.getPosition());
                }
            }
            message.set("cookies", mapper.valueToTree(cookiePositions));

            WebSocketHandler webSocketHandler = getWebSocketHandler();
            webSocketHandler.broadcastToLobby(game.getLobby().getId(), message);

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Countdown wurde unterbrochen: {}", e.getMessage());
                break;
            }
        }
    }

    public void rankRemainingPlayers(Game game) {
        List<Snake> remainingPlayers = new ArrayList<>();

        for (Snake snake : game.getSnakes()) {
            if (snake.getCoordinates().length == 0) {
                continue;
            } else {
                remainingPlayers.add(snake);
            }
        }

        remainingPlayers.sort((s1, s2) -> Integer.compare(s2.getCoordinates().length, s1.getCoordinates().length));
        Collections.reverse(remainingPlayers);
        for (Snake player : remainingPlayers) {
            game.addLeaderboardEntry(player.getUsername());
            User user = userRepository.findByUsername(player.getUsername());
            if (user.getLengthPR() < player.getCoordinates().length) {
                user.setLengthPR(player.getCoordinates().length);
                userRepository.save(user);
                userRepository.flush();
            }
        }
    }
}


