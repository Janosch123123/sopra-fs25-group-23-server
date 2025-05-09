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

    @Column(name = "admin_id")
    private Long adminId;

    @Column
    private Long gameId;

    @Column
    private String visibility;

    @Column
    private boolean solo = false;

    @Column
    private String spawnRate = "Medium";

    @Column
    private boolean powerupsWanted = false;

    @Column
    private boolean sugarRush = false;

    // Change from a list of User objects to a list of user IDs
    @ElementCollection(fetch = FetchType.EAGER) // Added EAGER fetch type
    @CollectionTable(name = "lobby_participants", joinColumns = @JoinColumn(name = "lobby_id"))
    @Column(name = "participant_id")
    private List<Long> participantIds = new ArrayList<>();

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAdminId() {
        return adminId;
    }

    public void setAdminId(Long adminId) {
        this.adminId = adminId;
    }

    public boolean isSolo() {
        return solo;
    }

    public void setSolo(boolean solo) {
        this.solo = solo;
    }

    public List<Long> getParticipantIds() {
        return participantIds;
    }

    public void setParticipantIds(List<Long> participantIds) {
        this.participantIds = participantIds;
    }

    // Helper method to add a participant ID
    public synchronized void addParticipantId(Long userId) {
        if (!this.participantIds.contains(userId)) {
            this.participantIds.add(userId);
        }
    }

    // Helper method to remove a participant ID
    public void removeParticipantId(Long userId) {
        this.participantIds.remove(userId);
    }

    // Convenience method to add a participant using User object
    public synchronized void addParticipant(User user) {
        if (user != null && user.getId() != null) {
            addParticipantId(user.getId());
        }
    }

    // Convenience method to remove a participant using User object
    public void removeParticipant(User user) {
        if (user != null && user.getId() != null) {
            removeParticipantId(user.getId());
        }
    }

    // Helper method to set admin using User object
    public void setAdminFromUser(User user) {
        if (user != null) {
            this.adminId = user.getId();
        }
    }

    public Long getGameId() {
        return gameId;
    }

    public void setGameId(Long gameId) {
        this.gameId = gameId;
    }

    public String getVisibility() {
        return visibility;
    }
    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public String getSpawnRate() {return spawnRate;}
    public void setSpawnRate(String spawnRate) {this.spawnRate = spawnRate;}

    public boolean getPowerupsWanted() {return powerupsWanted;}
    public void setPowerupsWanted(Boolean powerupsWanted) {this.powerupsWanted = powerupsWanted;}

    public boolean getSugarRush() {return sugarRush;}
    public void setSugarRush(Boolean sugarRush) {this.sugarRush = sugarRush;}
}