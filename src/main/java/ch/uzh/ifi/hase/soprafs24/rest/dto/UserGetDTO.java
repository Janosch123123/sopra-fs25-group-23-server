package ch.uzh.ifi.hase.soprafs24.rest.dto;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import java.util.Date;

public class UserGetDTO {

  private Long id;

  private String username;
  private UserStatus status;
  private Date creationDate;
  private String token; // added token field
  private int wins;
  private int kills;
  private int level;
  private int lobbyCode;
  private int playedGames;

  public int getPlayedGames() {return playedGames;}
  public void setPlayedGames(int playedGames) {this.playedGames = playedGames;}

  public int getLobbyCode() {return lobbyCode;}
  public void setLobbyCode(int lobbyCode) {this.lobbyCode = lobbyCode;}

  public int getLevel() {
    return level;
  }
  public void setLevel(int level) {
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

  public UserStatus getStatus() {
    return status;
  }

  public void setStatus(UserStatus status) {
    this.status = status;
  }

  public Date getCreationDate() {
    return creationDate;
  }

  public void setCreationDate(Date creationDate) {
    this.creationDate = creationDate;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }
}
