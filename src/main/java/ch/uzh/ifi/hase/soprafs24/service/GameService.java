package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.*;
import ch.uzh.ifi.hase.soprafs24.entity.Powerdowns.Divider;
import ch.uzh.ifi.hase.soprafs24.entity.Powerdowns.ReverseControl;
import ch.uzh.ifi.hase.soprafs24.entity.Powerups.*;
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
import java.util.stream.Collectors;

import static ch.uzh.ifi.hase.soprafs24.service.LobbyService.putGameToLobby;

@Service
@Transactional
public class GameService {

    private final LobbyRepository lobbyRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final SnakeService snakeService;
    private final BotService botService;
    private final ObjectMapper mapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(GameService.class);

    // Replace direct WebSocketHandler dependency with ApplicationContext
    private final ApplicationContext applicationContext;

    @Autowired
    public GameService(LobbyRepository lobbyRepository, UserRepository userRepository,
                       UserService userService, ApplicationContext applicationContext, SnakeService snakeService, BotService botService) {
        this.lobbyRepository = lobbyRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.applicationContext = applicationContext;
        this.snakeService = snakeService;
        this.botService = botService;
    }

    // Add a method to get WebSocketHandler lazily when needed
    private WebSocketHandler getWebSocketHandler() {
        return applicationContext.getBean(WebSocketHandler.class);
    }

    public Game createGame(Lobby lobby, String cookieSpawnRate, Boolean powerupsWanted) {
        // Ensure we are working with a managed entity within the current transaction
        Lobby managedLobby = lobbyRepository.findById(lobby.getId())
                .orElseThrow(() -> new IllegalArgumentException("Lobby not found with ID: " + lobby.getId()));

        Game game = new Game();
        game.setPowerupsWanted(powerupsWanted);
        switch (cookieSpawnRate) {
            case "Slow" -> game.setCookieSpawnRate(0.1);
            case "Medium" -> game.setCookieSpawnRate(0.3);
            case "Fast" -> game.setCookieSpawnRate(0.5);
            case "sugarRush" -> {
                game.setCookieSpawnRate(0.0);
                activateSugarRush(game);
            }
            default -> game.setCookieSpawnRate(0.3); // Default to medium if invalid input
        }
        game.setLobby(managedLobby);
        putGameToLobby(game, managedLobby.getId());

        List<Long> playersId = managedLobby.getParticipantIds();

        // Log player IDs to debug
        logger.info("Creating game with {} players", playersId.size());
        addSnakesToBoard(game, playersId);

        if (cookieSpawnRate.equals("sugarRush")) {
        }
        else {
            if (!powerupsWanted) {
                // X = 13
                game.addItem(new Cookie(new int[]{13, 11}, "cookie"));
                game.addItem(new Cookie(new int[]{13, 12}, "cookie"));
                game.addItem(new Cookie(new int[]{13, 13}, "cookie"));

                // X = 14
                game.addItem(new Cookie(new int[]{14, 11}, "cookie"));
                game.addItem(new Cookie(new int[]{14, 12}, "cookie"));
                game.addItem(new Cookie(new int[]{14, 13}, "cookie"));

                // X = 15
                game.addItem(new Cookie(new int[]{15, 11}, "cookie"));
                game.addItem(new Cookie(new int[]{15, 12}, "cookie"));
                game.addItem(new Cookie(new int[]{15, 13}, "cookie"));

                // X = 16
                game.addItem(new Cookie(new int[]{16, 11}, "cookie"));
                game.addItem(new Cookie(new int[]{16, 12}, "cookie"));
                game.addItem(new Cookie(new int[]{16, 13}, "cookie"));
            }
            else {
                // X = 13
                game.addItem(new Multiplier(new int[]{13, 11}, "powerup"));
                game.addItem(new Cookie(new int[]{13, 12}, "cookie"));
                game.addItem(new Multiplier(new int[]{13, 13}, "powerup"));

                // X = 14
                game.addItem(new Cookie(new int[]{14, 11}, "cookie"));
                game.addItem(new GoldenCookie(new int[]{14, 12}, "powerup"));
                game.addItem(new Cookie(new int[]{14, 13}, "cookie"));

                // X = 15
                game.addItem(new Cookie(new int[]{15, 11}, "cookie"));
                game.addItem(new GoldenCookie(new int[]{15, 12}, "powerup"));
                game.addItem(new Cookie(new int[]{15, 13}, "cookie"));

                // X = 16
                game.addItem(new Multiplier(new int[]{16, 11}, "powerup"));
                game.addItem(new Cookie(new int[]{16, 12}, "cookie"));
                game.addItem(new Multiplier(new int[]{16, 13}, "powerup"));
            }
        }
        return game;
    }

    private void activateSugarRush(Game game) {
        for (int x = 0; x <= 29; x++) {
            for (int y = 0; y <= 24; y++) {
                // Füge an jeder Position ein Cookie hinzu
                game.addItem(new Cookie(new int[]{x, y}, "cookie"));
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
            User user = userService.getUserById(playerId);
            boolean isBot = user.getIsBot();
            snake.setGame(game);
            snake.setUserId(playerId);
            snake.setUsername(userService.getUserById(playerId).getUsername());
            snake.setDirection(direction);
            snake.setCoordinates(coordinate);
            snake.setIsBot(isBot);
            snake.setLength(2);
            snake.setHead(new int[]{2, 2});
            snake.setTail(new int[]{2, 1});
            game.addSnake(snake);
        }
    }

    public void start(Game game) {
        new Thread(() -> { // Startet die Game-Loop in einem eigenen Thread
            try {
                startCountdown(game, 5);
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
            // Game loop
            while (!game.isGameOver()) {
                updateGameState(game);
                game.setTimestamp(game.getTimestamp() - 0.20f);// Aktualisiert den Spielzustand (Bewegungen, Kollisionsprüfung)
                try {
                    broadcastGameState(game); // Sendet Spielzustand an alle WebSocket-Clients
                }
                catch (IOException e) {
                    throw new RuntimeException(e);
                }
                try {
                    Thread.sleep(200); // Wartezeit für den nächsten Loop (z. B. 100ms pro Frame)
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            if (!game.getLobby().isSolo() && game.getTimestamp()>0){game.setWinnerRun(true);}

            if (game.getWinnerRun() && !game.getLobby().isSolo()){
                for(int i = 0; i < 15; i++) {
                    updateGameState(game);
                    game.setTimestamp(game.getTimestamp() - 0.20f);// Aktualisiert den Spielzustand (Bewegungen, Kollisionsprüfung)
                    try {
                        broadcastGameState(game); // Sendet Spielzustand an alle WebSocket-Clients
                    }
                    catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    try {
                        Thread.sleep(200); // Wartezeit für den nächsten Loop (z. B. 100ms pro Frame)
                    }
                    catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                game.setWinnerRun(false);
                System.out.println("Winner run is set to false in start: " + game.getWinnerRun());
            }
            if (game.getLobby().isSolo()){game.setWinnerRun(false);}
            try {
                endGame(game);
            } // send winner to FE etc//
            catch (IOException e) {
                throw new RuntimeException(e);
            }
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

        // Extrahiere die Golden Cookies aus der Items-Liste
        List<int[]> goldenCookiePositions = new ArrayList<>();
        for (Item item : game.getItems()) {
            if ("powerup".equals(item.getType()) && item instanceof GoldenCookie) {
                goldenCookiePositions.add(item.getPosition());
            }
        }
        // Füge die Golden Cookie-Positionen zu den JSON-Daten hinzu
        message.set("goldenCookies", mapper.valueToTree(goldenCookiePositions));

        // Extrahiere die ReverseControl-Items aus der Items-Liste
        List<int[]> reverseControlPositions = new ArrayList<>();
        for (Item item : game.getItems()) {
            if ("powerup".equals(item.getType()) && item instanceof ReverseControl) {
                reverseControlPositions.add(item.getPosition());
            }
        }
        // Füge die ReverseControl-Positionen zu den JSON-Daten hinzu
        message.set("reverseControls", mapper.valueToTree(reverseControlPositions));

        // Extrahiere die Divider-Items aus der Items-Liste
        List<int[]> dividerPositions = new ArrayList<>();
        for (Item item : game.getItems()) {
            if ("powerup".equals(item.getType()) && item instanceof Divider) {
                dividerPositions.add(item.getPosition());
            }
        }
        // Füge die Divider-Positionen zu den JSON-Daten hinzu
        message.set("dividers", mapper.valueToTree(dividerPositions));

        List<int[]> multiplierPositions = new ArrayList<>();
        for (Item item : game.getItems()) {
            if ("powerup".equals(item.getType()) && item instanceof Multiplier) {
                multiplierPositions.add(item.getPosition());
            }
        }
        // Füge die Divider-Positionen zu den JSON-Daten hinzu
        message.set("multipliers", mapper.valueToTree(multiplierPositions));

        // Map mit Username als Key und SnakeEffect-Informationen als Value erstellen
        Map<String, Object> effectDictionary = new HashMap<>();
        for (Snake snake : game.getSnakes()) {
            String username = snake.getUsername(); // Benutzername als Key
            List<String> effectNames = new ArrayList<>();

            // Für jeden Effekt den Klassennamen extrahieren
            for (Item effect : snake.getEffects()) {
                // getSimpleName() gibt nur den Klassennamen ohne Package zurück
                if (effect instanceof GoldenCookie) {
                    effectNames.add(effect.getClass().getSimpleName()+((GoldenCookie) effect).getCount());
                }
                else if (effect instanceof ReverseControl) {
                    effectNames.add(effect.getClass().getSimpleName()+((ReverseControl) effect).getTimer());
                }
                else if (effect instanceof Multiplier) {
                    effectNames.add(effect.getClass().getSimpleName()+(float)(Math.round((10-(((Multiplier) effect).getStart()-game.getTimestamp()))*100)/100.0));
                }
//                effectNames.add(effect.getClass().getSimpleName());
            }

            // Leere Liste oder Liste mit Effektnamen zum Dictionary hinzufügen
            effectDictionary.put(username, effectNames);

        }
        // Füge die strukturierte Map dem JSON-Objekt hinzu
        message.set("effects", mapper.valueToTree(effectDictionary));


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
        game.setWinnerRun(false);
        System.out.println("WinnerRun is set to false in endGame: " + game.getWinnerRun());
        rankRemainingPlayers(game);

        // Check if this is a solo lobby
        Lobby lobby = game.getLobby();
        boolean isSoloLobby = lobby.isSolo();

        // update winning stats
        String winnerName = game.getLeaderboard().get(0);
        if (winnerName != null) {
            User winner = userRepository.findByUsername(winnerName);
            if (!isSoloLobby) {
                logger.info("User won before: {}", winner.getWins());
                winner.setWins(winner.getWins()+1);
                userRepository.save(winner);
                logger.info("User won after: {}", winner.getWins());
            }
            logger.info("User {} won the game!", winnerName);
        }
        //update level stats + playedGames + winRate
        for (Long playerId : game.getLobby().getParticipantIds()) {
            Optional<User> currentUser = userRepository.findById(playerId);
            if (currentUser.isPresent()) {
                User user = currentUser.get();
                if (!isSoloLobby) {
                    user.setPlayedGames(user.getPlayedGames()+1);
                    System.out.println("Games are updated in endGame: " + user.getPlayedGames());
                    int points = 1 + (user.getWins() / 2) + (user.getKills() / 4);
                    double newLevel = 5 * Math.sqrt((double)points/4) - 1;
                    user.setLevel(newLevel);
                    // update winRate
                    double newWinRate = (double) user.getWins() / user.getPlayedGames();
                    user.setWinRate(newWinRate);
                    userRepository.save(user);
                    userRepository.flush();
                }
                logger.info("User {} reached level {}!", user.getUsername(), user.getLevel());
            }
            else {
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
        System.out.println("endGame has WinnerRun at the end: " + game.getWinnerRun());
    }

    private void updateGameState(Game game) {
        List<Snake> aliveSnakes = new ArrayList<>();
        for (Snake snake : game.getSnakes()) {
            if (snake.getCoordinates().length == 0) {
                continue; // already dead
            }
            aliveSnakes.add(snake);
            if (snake.getIsBot()) {
                botService.updateBot(game, snake);
            } else {
                updateSnakeDirection(snake);
            }
            snakeService.moveSnake(snake);

            if (snakeService.checkCollision(snake, game)) {
                // Snake has collided with another snake
                logger.info("Collision detected for snake: {}", snake.getUserId());
                spawnCookiesOnDeath(snake, game);
                snake.setCoordinates(new int[0][0]); // Set coordinates to empty to mark as dead
                game.addLeaderboardEntry(snake.getUsername());
            }
            if (snake.getCoordinates().length != 0) {
                checkPowerupCollision(snake);
            }
        }
        if (aliveSnakes.size() == 1) {
            Snake winnerSnake = aliveSnakes.get(0);
            game.setWinner(winnerSnake.getUsername());
        }


        // Spawne ggf. neue Items
        Random random = new Random();
        double chance = random.nextDouble();
        if (chance < game.getCookieSpawnRate()) {
            spawnItem(game);
        }
    }

    private void checkPowerupCollision(Snake snake) {
        // Position des Kopfes der Schlange abrufen
        int[] head = snake.getCoordinates()[0]; // Der Kopf ist der erste Punkt im Koordinatenarray

        // Durchlaufen der Items im Spiel (Game)
        for (Item item : snake.getGame().getItems()) {
            // Prüfen, ob das aktuelle Item vom Typ "cookie" ist
            if ("powerup".equals(item.getType())) {
                int[] powerupPosition = item.getPosition(); // Cookie-Position abrufen

                // könnte fourCoordinate Powerdown sein
                if (item instanceof Divider) {
                    int[][] powerupFourPositions = ((Divider) item).getFourPositions();
                    // Prüfe Kollision mit allen vier Positionen des Dividers
                    for (int[] powerupFourPosition : powerupFourPositions) {
                        if (head[0] == powerupFourPosition[0] && head[1] == powerupFourPosition[1]) {
                            // Kollision -> Entfernt den Divider aus dem Spiel und wendet den Effekt an
                            item.applyEffect(snake);
                            snake.getGame().getItems().remove(item);
                            return; // Eine Kollision wurde festgestellt
                        }
                    }
                }
                if (item instanceof ReverseControl) {
                    int[][] powerupFourPositions = ((ReverseControl) item).getFourPositions();
                    // Prüfe Kollision mit allen vier Positionen des Dividers
                    for (int[] powerupFourPosition : powerupFourPositions) {
                        if (head[0] == powerupFourPosition[0] && head[1] == powerupFourPosition[1]) {
                            // Kollision -> Entfernt den Divider aus dem Spiel und wendet den Effekt an
                            for (Item effect : snake.getEffects()) {
                                if (effect instanceof ReverseControl){
                                    snake.removeEffect(effect);
                                    break;
                                }
                            }
                            item.applyEffect(snake);
                            snake.getGame().getItems().remove(item);
                            return; // Eine Kollision wurde festgestellt
                        }
                    }
                }


                // Prüfen, ob die Kopfposition mit der Cookie-Position übereinstimmt
                if (head[0] == powerupPosition[0] && head[1] == powerupPosition[1]) {
                    // Kollision -> Entfernt den Cookie aus dem Spiel
                    item.applyEffect(snake);
                    snake.getGame().getItems().remove(item);
                    return; // Eine Kollision wurde festgestellt
                }
            }
        }

        // Keine Kollision gefunden
        return;
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
                Item item = new Cookie(coordinate, "cookie");
                game.addItem(item);
            }
        }
    }

    private void spawnItem(Game game) {
        int[] freeCoord = findFreeCoordinate(game);
        // spawn cookie on all ticks
        if (freeCoord != null) {
            Item item = new Cookie(freeCoord, "cookie");
            game.addItem(item);
        }
        // spawn powerup to a certain probability
        List<int[]> usedCoordinates = findUsedCoordinates(game);

        int[] freeCord = findFreeCoordinate(game);
        int[] posi = freeCord;
        while (posi != null) {
            if (posi != freeCord) {break;}
            posi = findFourAdjacentCoordinates(game);
        }
        if (game.getPowerupsWanted()){
            Random random1 = new Random();
            // spawn golden Cookie
            double chance = random1.nextDouble();
            if (chance < 0.02) {
                if (freeCord != null) {
                    Item itemP = new GoldenCookie(freeCord, "powerup");
                    game.addItem(itemP);
                }
            }
            // spawn Multiplier
            else if (chance < 0.04) {
                if (freeCord != null) {
                    Item itemP = new Multiplier(freeCord, "powerup");
                    game.addItem(itemP);
                }
            }
            // spawn Reverse Control
            else if (chance < 0.06) {
                if (posi != null && posi != freeCoord) {
                    Item itemP = new ReverseControl(posi, "powerup");
                    game.addItem(itemP);
                }
            }
            // spawn Divider
            else if (chance < 0.08) {
                if (posi != null && posi != freeCoord) {
                    Item itemP = new Divider(posi, "powerup");
                    game.addItem(itemP);
                }
            }
        }
        return;
    }
    private int[] findFreeCoordinate(Game game) {
        Random random = new Random();
        List<int[]> usedCoordinates = findUsedCoordinates(game);
        int maxAttempts = 100; // Maximale Anzahl an Versuchen

        for (int i = 0; i < maxAttempts; i++) {
            // Zufällige Koordinate generieren
            int x = random.nextInt(30);
            int y = random.nextInt(25);
            int[] newCoord = new int[]{x, y};

            // Prüfen, ob die Koordinate frei ist
            if (!containsCoordinate(usedCoordinates, newCoord)) {
                return newCoord; // Freie Koordinate gefunden
            }
        }

        // Keine freie Koordinate gefunden
        return null;
    }

    private int[] findFourAdjacentCoordinates(Game game) {
        Random random = new Random();
        // Maximale Anzahl an Versuchen begrenzen
        int maxAttempts = 100;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            // Zufällige Startkoordinate generieren (obere linke Ecke des 2x2 Bereichs)
            // Wir müssen einen Randabstand einhalten, damit die anderen 3 Koordinaten noch im Spielfeld liegen
            int x = random.nextInt(28); // Max 29, da wir einen zusätzlichen Platz für x+1 brauchen
            int y = random.nextInt(23); // Max 24, da wir einen zusätzlichen Platz für y+1 brauchen

            // Die vier Koordinaten des 2x2 Bereichs
            int[][] fourCoordinates = new int[4][2];
            fourCoordinates[0] = new int[]{x, y};         // Obere linke Ecke
            fourCoordinates[1] = new int[]{x + 1, y};     // Obere rechte Ecke
            fourCoordinates[2] = new int[]{x, y + 1};     // Untere linke Ecke
            fourCoordinates[3] = new int[]{x + 1, y + 1}; // Untere rechte Ecke

            // Überprüfen, ob alle vier Positionen frei sind
            boolean allPositionsFree = true;

            for (int[] coordinate : fourCoordinates) {
                // Überprüfen, ob die Position außerhalb des Spielfelds liegt
                if (coordinate[0] < 0 || coordinate[0] >= 30 || coordinate[1] < 0 || coordinate[1] >= 25) {
                    allPositionsFree = false;
                    break;
                }

                // Überprüfen, ob die Position bereits belegt ist
                if (containsCoordinate(findUsedCoordinates(game), coordinate)) {
                    allPositionsFree = false;
                    break;
                }

                    if (!allPositionsFree) {
                    break; // Wenn eine Position belegt ist, suchen wir eine andere Startposition
                }
            }

            if (allPositionsFree) {
                return fourCoordinates[0]; // Wir haben vier freie, nebeneinanderliegende Positionen gefunden
            }
        }

        // Kein freier 2x2 Bereich gefunden
        return null;
    }


    public void respondToKeyInputs(Game game, User user, String direction) {
        for (Snake snake : game.getSnakes()) {
            if (snake.getUserId().equals(user.getId())) {
                if (direction.equals("UP") && snake.getDirection().equals("DOWN") && snake.getDirectionQueue().isEmpty()) {
                }
                else if (direction.equals("DOWN") && snake.getDirection().equals("UP") && snake.getDirectionQueue().isEmpty()) {
                }
                else if (direction.equals("LEFT") && snake.getDirection().equals("RIGHT") && snake.getDirectionQueue().isEmpty()) {
                }
                else if (direction.equals("RIGHT") && snake.getDirection().equals("LEFT") && snake.getDirectionQueue().isEmpty()) {
                }

                else if (direction.equals("DOWN") && snake.getDirection().equals("DOWN") && snake.getDirectionQueue().isEmpty()) {
                }
                else if (direction.equals("UP") && snake.getDirection().equals("UP") && snake.getDirectionQueue().isEmpty()) {
                }
                else if (direction.equals("LEFT") && snake.getDirection().equals("LEFT") && snake.getDirectionQueue().isEmpty()) {
                }
                else if (direction.equals("RIGHT") && snake.getDirection().equals("RIGHT") && snake.getDirectionQueue().isEmpty()) {
                }
                else {
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
            }
            else if (newDirection.equals("DOWN") && !snake.getDirection().equals("UP")) {
                snake.setDirection(newDirection);
            }
            else if (newDirection.equals("LEFT") && !snake.getDirection().equals("RIGHT")) {
                snake.setDirection(newDirection);
            }
            else if (newDirection.equals("RIGHT") && !snake.getDirection().equals("LEFT")) {
                snake.setDirection(newDirection);
            }
            // check if ReverseControl Effect is active (Powerup)
            for (Item powerup : snake.getEffects()) {
                if (powerup instanceof ReverseControl) {
                    ((ReverseControl) powerup).revertMovement(snake);
                    break;
                }
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

            // Extrahiere die Golden Cookies aus der Items-Liste
            List<int[]> goldenCookiePositions = new ArrayList<>();
            for (Item item : game.getItems()) {
                if ("powerup".equals(item.getType()) && item instanceof GoldenCookie) {
                    goldenCookiePositions.add(item.getPosition());
                }
            }
            // Füge die Golden Cookie-Positionen zu den JSON-Daten hinzu
            message.set("goldenCookies", mapper.valueToTree(goldenCookiePositions));

            // Extrahiere die ReverseControl-Items aus der Items-Liste
            List<int[]> reverseControlPositions = new ArrayList<>();
            for (Item item : game.getItems()) {
                if ("powerup".equals(item.getType()) && item instanceof ReverseControl) {
                    reverseControlPositions.add(item.getPosition());
                }
            }
            // Füge die ReverseControl-Positionen zu den JSON-Daten hinzu
            message.set("reverseControls", mapper.valueToTree(reverseControlPositions));

            // Extrahiere die Divider-Items aus der Items-Liste
            List<int[]> dividerPositions = new ArrayList<>();
            for (Item item : game.getItems()) {
                if ("powerup".equals(item.getType()) && item instanceof Divider) {
                    dividerPositions.add(item.getPosition());
                }
            }
            // Füge die Divider-Positionen zu den JSON-Daten hinzu
            message.set("dividers", mapper.valueToTree(dividerPositions));

            List<int[]> multiplierPositions = new ArrayList<>();
            for (Item item : game.getItems()) {
                if ("powerup".equals(item.getType()) && item instanceof Multiplier) {
                    multiplierPositions.add(item.getPosition());
                }
            }
            // Füge die Divider-Positionen zu den JSON-Daten hinzu
            message.set("multipliers", mapper.valueToTree(multiplierPositions));

            WebSocketHandler webSocketHandler = getWebSocketHandler();
            webSocketHandler.broadcastToLobby(game.getLobby().getId(), message);

            // Warte 1 Sekunde vor der nächsten Iteration
            try {
                Thread.sleep(1000); // 1000 Millisekunden = 1 Sekunde
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Countdown wurde unterbrochen: {}", e.getMessage());
                break; // Beende die Schleife bei Unterbrechung
            }
        }
    }

    public void rankRemainingPlayers(Game game) {
        List<Snake> remainingPlayers = new ArrayList<>();

        for (Snake snake : game.getSnakes()) {
            if (snake.getCoordinates().length == 0) {
                continue;
            }
            else {
                remainingPlayers.add(snake);
            }
        }

        Lobby lobby = game.getLobby();
        boolean isSoloLobby = lobby.isSolo();

        // Sortiere remainingPlayers nach der Größe von snake.getCoordinates()
        remainingPlayers.sort((s1, s2) -> Integer.compare(s2.getCoordinates().length, s1.getCoordinates().length));
        // Liste umkehren
        logger.info("Sorting remaining players by length");
        Collections.reverse(remainingPlayers);
        for (Snake player : remainingPlayers) {
            if (!(game.getLeaderboard().contains(player.getUsername()))) {
                game.addLeaderboardEntry(player.getUsername());
            }
            User user = userRepository.findByUsername(player.getUsername());
            logger.info("Adding {} to leaderboard", player.getUsername());
            logger.info("Leaderboard: {}", game.getLeaderboard());
            if (!isSoloLobby) {
                logger.info("So we are really going here even thow we shoudnt");
                if (user.getLengthPR() < player.getCoordinates().length) {
                    user.setLengthPR(player.getCoordinates().length);
                    userRepository.save(user);
                    userRepository.flush();
                    }
            }
            int[][] newCoords = new int[0][0];
            player.setCoordinates(newCoords);
        }

    }
    public List<int[]> findUsedCoordinates(Game game) {
        List<int[]> usedCoordinates = new ArrayList<>();
        for (Snake snake : game.getSnakes()) {
            usedCoordinates.addAll(Arrays.asList(snake.getCoordinates()));

        }
        // Koordinaten aller Items hinzufügen
        for (Item item : game.getItems()) {
            // Prüfe, ob es sich um ein spezielles Item mit mehreren Positionen handelt
            if (item instanceof Divider) {
                // Füge alle vier Positionen des Dividers hinzu
                usedCoordinates.addAll(Arrays.asList(((Divider) item).getFourPositions()));
            }
            else if (item instanceof ReverseControl) {
                // Füge alle vier Positionen des ReverseControl hinzu
                usedCoordinates.addAll(Arrays.asList(((ReverseControl) item).getFourPositions()));
            }
            else {
                // Für reguläre Items mit einer Position
                usedCoordinates.add(item.getPosition());
            }
        }
        String coordinates = usedCoordinates.stream()
                .map(coord -> "[" + coord[0] + ", " + coord[1] + "]")
                .collect(Collectors.joining(", "));
        return usedCoordinates;
    }
    private boolean containsCoordinate(List<int[]> coordinates, int[] coord) {
        for (int[] c : coordinates) {
            if (c[0] == coord[0] && c[1] == coord[1]) {
                return true;
            }
        }
        return false;
    }


}


