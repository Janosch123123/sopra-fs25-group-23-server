package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.handler.WebSocketHandler;
import ch.uzh.ifi.hase.soprafs24.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
// import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
public class LobbyService {

    private final Logger log = LoggerFactory.getLogger(LobbyService.class);
    private static final Map<Long, Game> lobbyGamesMap = new ConcurrentHashMap<>();

    private final LobbyRepository lobbyRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final ApplicationContext applicationContext;

    // @Autowired
    public LobbyService(LobbyRepository lobbyRepository, UserRepository userRepository, UserService userService, ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        this.lobbyRepository = lobbyRepository;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    public static Game getGameByLobby(long lobbyId) {
        return lobbyGamesMap.get(lobbyId);
    }

    public static void putGameToLobby(Game game, Long lobbyId) {
        lobbyGamesMap.put(lobbyId, game);
    }

    /**
     * Creates a new lobby with the given user as admin
     *
     * @param admin the user who will be the admin of the lobby
     * @return the created lobby
     */
    public Lobby createPrivateLobby(User admin) {
        if (admin == null) {
            throw new IllegalArgumentException("Admin user cannot be null");
        }

        log.debug("Creating new lobby with admin: {}", admin.getUsername());

        // Create new lobby
        Lobby lobby = new Lobby();
        lobby.setAdminId(admin.getId());
        lobby.setVisibility("private");

        // Save to get an ID
        Lobby savedLobby = lobbyRepository.save(lobby);

        // Add admin as first participant
        savedLobby.addParticipant(admin);
        lobbyRepository.save(savedLobby);

        log.info("Created new lobby with ID: {} and admin: {}", savedLobby.getId(), admin.getUsername());

        return savedLobby;
    }

    public Lobby handleQuickPlay(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User user cannot be null");
        }
        List<Lobby> lobbies = lobbyRepository.findByVisibility("public");

        if (lobbies.isEmpty()) {
            return createPublicLobby(user);
        } else {
            for (Lobby lobby : lobbies) {
                if (lobby.getParticipantIds().size() < 4) {
                    lobby.addParticipant(user);
                    lobbyRepository.save(lobby);
                    return lobby;
                }
            }
        }
        return createPublicLobby(user);
    }

    public Lobby createPublicLobby(User admin) {
        if (admin == null) {
            throw new IllegalArgumentException("Admin user cannot be null");
        }

        log.debug("Creating new lobby with admin: {}", admin.getUsername());

        // Create new lobby
        Lobby lobby = new Lobby();
        lobby.setAdminId(admin.getId());
        lobby.setVisibility("public");

        // Save to get an ID
        Lobby savedLobby = lobbyRepository.save(lobby);

        // Add admin as first participant
        savedLobby.addParticipant(admin);
        lobbyRepository.save(savedLobby);

        // Schedule bot additions
        botAdder(savedLobby);
        
        log.info("Created new lobby with ID: {} and admin: {}", savedLobby.getId(), admin.getUsername());
        
        return savedLobby;
    }
    private void botAdder(Lobby lobby) {

        Random random = new Random();
        int randomInt = random.nextInt(6) + 5;       
        addBotToLobbyAsync(lobby, randomInt);
        
    }

    private boolean checkIfGameStarted(Lobby lobby) {
        return lobby.getGameId() != null;
    }


    private WebSocketHandler getWebSocketHandler() {
        return applicationContext.getBean(WebSocketHandler.class);
    }

    private void addBotToLobbyAsync(Lobby lobby, int delayInSeconds) {
        System.out.println("Scheduling bot addition for lobby: " + lobby.getId() + " in " + delayInSeconds + " seconds");
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.schedule(() -> {
            System.out.println("IN THE SCHEDULE: " + lobby.getId());
            addBotHelper(lobby);
        }, delayInSeconds, TimeUnit.SECONDS);
    }
    
    private void addBotHelper(Lobby lobby) {
            try {
                Lobby latestLobby = lobbyRepository.findById(lobby.getId())
                .orElseThrow(() -> new IllegalStateException("Lobby not found with ID: " ));
                if (latestLobby.getParticipantIds().size() < 4 && !checkIfGameStarted(latestLobby)) {
                    User createdBot = userService.createBot();
                    addLobbyCodeToUser(createdBot, latestLobby.getId());
        
                    latestLobby.addParticipant(createdBot);
                    lobbyRepository.save(latestLobby);
        
                    WebSocketHandler webSocketHandler = getWebSocketHandler();
                    webSocketHandler.sendLobbyStateToUsers(latestLobby.getId());

                    if (latestLobby.getParticipantIds().size() < 4) {
                        System.out.println("Adding another bot to lobby:" + latestLobby.getParticipantIds().size());
                        botAdder(latestLobby);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to add bot to lobby: {}", e.getMessage());
            }
        
    }

    /**
     * Gets a lobby by its ID
     *
     * @param lobbyId the ID of the lobby to retrieve
     * @return the lobby if found
     */
    public Lobby getLobbyById(Long lobbyId) {
        return lobbyRepository.findById(lobbyId)
                .orElseThrow(() -> new IllegalArgumentException("Lobby not found with ID: " + lobbyId));
    }

    /**
     * Validates if a lobby code exists
     *
     * @param lobbyCode the lobby code to validate
     * @return true if valid, false otherwise
     */
    public boolean validateLobby(long lobbyCode) {
        try {
            getLobbyById(lobbyCode);
            Optional<Lobby> lobby = lobbyRepository.findById(lobbyCode);
            if (lobby.isPresent()) {
                Lobby foundLobby = lobby.get();
                return foundLobby.getParticipantIds().size() <= 3;
            }

            return true;
        } catch (Exception e) {
            log.error("Error validating lobby code: {}", e.getMessage());
            return false;
        }
    }

    public void addLobbyCodeToUser(User user, long lobbyCode) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null and lobby code must be positive");
        }

        user.setLobbyCode(lobbyCode);
        userRepository.save(user);
    }

    /**
     * Find the lobby that a user is participating in
     * 
     * @param userId The ID of the user
     * @return The Lobby object or null if not found
     */
    public Lobby findLobbyForUser(Long userId) {
        List<Lobby> lobbies = lobbyRepository.findAll();

        for (Lobby lobby : lobbies) {
            if (lobby.getParticipantIds().contains(userId)) {
                return lobby;
            }
        }

        return null;
    }

    /**
     * Updates an existing lobby
     * 
     * @param lobby the lobby to update
     * @return the updated lobby
     */
    public Lobby updateLobby(Lobby lobby) {
        if (lobby == null) {
            throw new IllegalArgumentException("Lobby cannot be null");
        }

        // Verify the lobby exists
        lobbyRepository.findById(lobby.getId())
                .orElseThrow(() -> new IllegalArgumentException("Lobby not found with ID: " + lobby.getId()));

        return lobbyRepository.save(lobby);
    }

    /**
     * Deletes a lobby by its ID
     *
     * @param lobbyId the ID of the lobby to delete
     */
    public void deleteLobby(Long lobbyId) {
        // Check if the lobby exists
        Optional<Lobby> lobbyOptional = lobbyRepository.findById(lobbyId);
        if (lobbyOptional.isPresent()) {
            // Delete the lobby from the repository
            lobbyRepository.deleteById(lobbyId);
            log.info("Lobby with ID {} has been deleted.", lobbyId);
        } else {
            log.warn("Attempted to delete a non-existent lobby with ID {}.", lobbyId);
        }
    }

}