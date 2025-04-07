package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class LobbyService {
    
    private final Logger log = LoggerFactory.getLogger(LobbyService.class);
    
    private final LobbyRepository lobbyRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    
    @Autowired
    public LobbyService(LobbyRepository lobbyRepository, UserRepository userRepository, UserService userService) {
        this.lobbyRepository = lobbyRepository;
        this.userRepository = userRepository;
        this.userService = userService;
    }
    
    /**
     * Creates a new lobby with the given user as admin
     *
     * @param admin the user who will be the admin of the lobby
     * @return the created lobby
     */
    public Lobby createLobby(User admin) {
        if (admin == null) {
            throw new IllegalArgumentException("Admin user cannot be null");
        }
        
        log.debug("Creating new lobby with admin: {}", admin.getUsername());
        
        // Create new lobby
        Lobby lobby = new Lobby();
        lobby.setAdminId(admin.getId());
        
        // Save to get an ID
        Lobby savedLobby = lobbyRepository.save(lobby);
        
        // Add admin as first participant
        savedLobby.addParticipant(admin);
        lobbyRepository.save(savedLobby);
        
        log.info("Created new lobby with ID: {} and admin: {}", savedLobby.getId(), admin.getUsername());
        
        return savedLobby;
    }
    
    /**
     * Creates a new lobby with user identified by token as admin
     *
     * @param token the authentication token for the user who will be admin
     * @return the created lobby
     * @throws IllegalArgumentException if token is invalid or user not found
     */
    public Lobby createLobbyFromToken(String token) {
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("Authentication token cannot be null or empty");
        }
        
        // Get user from token
        User user = userService.getUserByToken(token);
        
        if (user == null) {
            throw new IllegalArgumentException("Invalid token or user not found");
        }
        
        // Use existing method to create the lobby with the found user
        return createLobby(user);
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
}