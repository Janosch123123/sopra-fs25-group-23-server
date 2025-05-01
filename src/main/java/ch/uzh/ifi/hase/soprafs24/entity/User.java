package ch.uzh.ifi.hase.soprafs24.entity;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * Internal User Representation
 * This class composes the internal representation of the user and defines how
 * the user is stored in the database.
 * Every variable will be mapped into a database field with the @Column
 * annotation
 * - nullable = false -> this cannot be left empty
 * - unique = true -> this value must be unqiue across the database -> composes
 * the primary key
 */
@Entity
@Table(name = "USER")
public class User implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue
  private Long id;

  @Column(nullable = false, unique = true)
  private String username;

  @Column(nullable = false, unique = true)
  private String token;

  @Column(nullable = false)
  private UserStatus status;

  @Column(nullable = false)
  private String password;

  @Column(nullable = false)
  private Date creationDate;

  @Column
  private int wins;

  @Column
  private int kills;

  @Column
  private double level;

  @Column
  private long lobbyCode;

  @Column
  private int playedGames;

  @Column
  private int lengthPR;

  @Column double winRate;

  public double getWinRate() {return winRate;}
  public void setWinRate(int winRate) {this.winRate = winRate;}

  public int getLengthPR() {return lengthPR;}
  public void setLengthPR(int lengthPR) {this.lengthPR = lengthPR;}

  public int getPlayedGames() {return playedGames;}
  public void setPlayedGames(int playedGames) {this.playedGames = playedGames;}

  public double getLevel() {
    return level;
  }

  public void setLevel(double level) {
    this.level = level;
  }

  public int getWins() {
    return wins;
  }

  public void setWins(int wins) {
    this.wins = wins;
  }

  public int getKills() {
    return kills;
  }

  public void setKills(int kills) {
    this.kills = kills;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public UserStatus getStatus() {
    return status;
  }

  public void setStatus(UserStatus status) {
    this.status = status;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public Date getCreationDate() {
    return creationDate;
  }

  public void setCreationDate(Date creationDate) {
    this.creationDate = creationDate;
  }

  public long getLobbyCode() {
    return lobbyCode;
  }

  public void setLobbyCode(long lobbyCode) {
    this.lobbyCode = lobbyCode;
  }
}
