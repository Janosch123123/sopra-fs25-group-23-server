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
    
    @Autowired
    public LobbyService(LobbyRepository lobbyRepository, UserRepository userRepository) {
        this.lobbyRepository = lobbyRepository;
        this.userRepository = userRepository;
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
        lobby.setAdmin(admin);
        
        // Save to get an ID
        Lobby savedLobby = lobbyRepository.save(lobby);
        
        // Add admin as first participant
        savedLobby.addParticipant(admin);
        userRepository.save(admin);
        
        log.info("Created new lobby with ID: {} and admin: {}", savedLobby.getId(), admin.getUsername());
        
        return savedLobby;
    }
    
    /**
     * Adds a user to a lobby
     *
     * @param lobbyId the ID of the lobby
     * @param user the user to add
     * @return the updated lobby
     */
    public Lobby joinLobby(Long lobbyId, User user) {
        if (lobbyId == null || user == null) {
            throw new IllegalArgumentException("Lobby ID and user cannot be null");
        }
        
        // Find the lobby
        Lobby lobby = lobbyRepository.findById(lobbyId)
            .orElseThrow(() -> new IllegalArgumentException("Lobby not found: " + lobbyId));
            
        log.debug("User {} joining lobby {}", user.getUsername(), lobbyId);
        
        // Add user to lobby
        lobby.addParticipant(user);
        userRepository.save(user);
        
        return lobby;
    }
    
    /**
     * Removes a user from a lobby
     *
     * @param lobbyId the ID of the lobby
     * @param user the user to remove
     * @return the updated lobby or null if the lobby was deleted
     */
    public Lobby leaveLobby(Long lobbyId, User user) {
        if (lobbyId == null || user == null) {
            throw new IllegalArgumentException("Lobby ID and user cannot be null");
        }
        
        // Find the lobby
        Lobby lobby = lobbyRepository.findById(lobbyId)
            .orElseThrow(() -> new IllegalArgumentException("Lobby not found: " + lobbyId));
            
        log.debug("User {} leaving lobby {}", user.getUsername(), lobbyId);
        
        // Remove user from lobby
        lobby.removeParticipant(user);
        userRepository.save(user);
        
        // Check if lobby is now empty and user was admin
        if (user.equals(lobby.getAdmin()) && lobby.getParticipants().isEmpty()) {
            log.info("Deleting empty lobby {}", lobbyId);
            lobbyRepository.delete(lobby);
            return null;
        }
        
        return lobby;
    }
    
    /**
     * Gets all lobbies
     *
     * @return list of all lobbies
     */
    public List<Lobby> getAllLobbies() {
        return lobbyRepository.findAll();
    }
    
    /**
     * Gets a lobby by ID
     *
     * @param lobbyId the ID of the lobby
     * @return the lobby if found
     */
    public Optional<Lobby> getLobbyById(Long lobbyId) {
        return lobbyRepository.findById(lobbyId);
    }
}