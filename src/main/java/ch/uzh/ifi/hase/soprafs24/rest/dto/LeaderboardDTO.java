package ch.uzh.ifi.hase.soprafs24.rest.dto;

public class LeaderboardDTO {
    private Long id;
    private String username;
    private double level;
    private int wins;
    private int kills;
    private int playedGames;
    private int lengthPR;
    private double winRate;

    // Getter und Setter
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

    public int getPlayedGames() {
        return playedGames;
    }

    public void setPlayedGames(int playedGames) {
        this.playedGames = playedGames;
    }

    public int getLengthPR() {
        return lengthPR;
    }

    public void setLengthPR(int lengthPR) {
        this.lengthPR = lengthPR;
    }
    public double getWinRate() {return winRate;}

    public void setWinRate(int winRate) {this.winRate = winRate;}
}
