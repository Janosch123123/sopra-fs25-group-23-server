package ch.uzh.ifi.hase.soprafs24.entity;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Internal Lobby Representation
 * This class composes the internal representation of the lobby and defines how
 * the lobby is stored in the database.
 */
@Entity
@Table(name = "LOBBY")
public class Lobby implements Serializable {

    private static final long serialVersionUID = 1L;
  
    @Id
    @GeneratedValue
    private Long id;

    // Many-to-One relationship with User (admin)
    @OneToOne
    @JoinColumn(name = "admin_id", nullable = false, unique = true)
    private User admin;
    
    // Optional: One-to-Many relationship with User (participants)
    // If you want to manage this relationship from the Lobby side as well
    @OneToMany(mappedBy = "lobby")
    private List<User> participants = new ArrayList<>();



    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public User getAdmin() {
        return admin;
    }
    
    public void setAdmin(User admin) {
        this.admin = admin;
    }
    
    public List<User> getParticipants() {
        return participants;
    }
    
    public void setParticipants(List<User> participants) {
        this.participants = participants;
    }
    
    // Helper method to add a participant
    public void addParticipant(User user) {
        this.participants.add(user);
        user.setLobby(this); // Set the bidirectional relationship
    }
    
    // Helper method to remove a participant
    public void removeParticipant(User user) {
        this.participants.remove(user);
        user.setLobby(null); // Clear the bidirectional relationship
    }
}