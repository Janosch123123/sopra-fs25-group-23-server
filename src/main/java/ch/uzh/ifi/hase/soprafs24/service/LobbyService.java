package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;

@Service
@Transactional
public class LobbyService {

    private final Logger log = LoggerFactory.getLogger(LobbyService.class);
    private static final Map<Long, Game> lobbyGamesMap = new ConcurrentHashMap<>();

    private final LobbyRepository lobbyRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    // @Autowired
    public LobbyService(LobbyRepository lobbyRepository, UserRepository userRepository, UserService userService) {
        this.lobbyRepository = lobbyRepository;
        this.userRepository = userRepository;
        this.userService = userService;
    }
    public static Game getGameByLobby(long lobbyId) {
        return lobbyGamesMap.get(lobbyId);
    }
    public static void putGameToLobby(Game game, Long lobbyId) {lobbyGamesMap.put(lobbyId, game);}

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
                if (lobby.getParticipantIds().size() < 3) {
                    lobby.addParticipant(user);
                    lobbyRepository.save(lobby);

                    System.out.println("User joined existing lobby");
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

        log.info("Created new lobby with ID: {} and admin: {}", savedLobby.getId(), admin.getUsername());

        return savedLobby;
    }



    // /**
    //  * Creates a new lobby with user identified by token as admin
    //  *
    //  * @param token the authentication token for the user who will be admin
    //  * @return the created lobby
    //  * @throws IllegalArgumentException if token is invalid or user not found
    //  */
    // public Lobby createLobbyFromToken(String token) {
    //     if (token == null || token.isEmpty()) {
    //         throw new IllegalArgumentException("Authentication token cannot be null or empty");
    //     }

    //     // Get user from token
    //     User user = userService.getUserByToken(token);

    //     if (user == null) {
    //         throw new IllegalArgumentException("Invalid token or user not found");
    //     }

    //     // Use existing method to create the lobby with the found user
    //     return createPrivateLobby(user);
    // }

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

}