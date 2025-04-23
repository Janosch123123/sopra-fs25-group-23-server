package ch.uzh.ifi.hase.soprafs24.entity;

import java.util.ArrayList;
import java.util.List;

public class Game {

    private long gameId;
    private static long idCounter = 0; // only to generate unique id
    private Lobby lobby;
    private List<Snake> snakes;
    private List<Item> items;
    private float timestamp;
    private boolean gameOver;
    private String winner;
    private List<String> leaderboard;
    private double cookieSpawnRate;

    public Game() {
        this.gameId = generateUniqueGameId();
        this.snakes = new ArrayList<>(); // Initialize the snakes list
        this.items = new ArrayList<>(); // Initialize the items list
        this.gameOver = false; // Initialize gameOver flag
        this.timestamp = 180;
        this.winner = null;
        this.leaderboard = new ArrayList<>();
        this.cookieSpawnRate = 0.3;
    }
    public float getTimestamp() {return timestamp;}
    public void setTimestamp(float timestamp) {this.timestamp = timestamp;}

    private static synchronized long generateUniqueGameId() {
        idCounter++; // Zähler wird inkrementiert
        return idCounter;
    }
    public String getWinner() {return winner;}
    public void setWinner(String winner) {this.winner = winner;}

    public List<String> getLeaderboard() {return leaderboard;}
    public void setLeaderboard(List<String> leaderboard) {this.leaderboard = leaderboard;}
    public void addLeaderboardEntry(String entry) {this.leaderboard.add(0, entry);}

    public long getGameId() {
        return gameId;
    }

    public void setGameId(long gameId) {
        this.gameId = gameId;
    }

    public Lobby getLobby() {
        return lobby;
    }

    public void setLobby(Lobby lobby) {
        this.lobby = lobby;
    }

    public List<Snake> getSnakes() {
        return snakes;
    }

    public void setSnakes(List<Snake> snakes) {
        this.snakes = snakes;
    }

    public void addSnake(Snake snake) {
        snakes.add(snake);
    }

    public void removeSnake(Snake snake) {
        snakes.remove(snake);
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public void addItem(Item item) {
        items.add(item);
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }

    public boolean getGameOver() {
        return gameOver;
    }

    public boolean isGameOver() {
        // zählen wieviele Spieler noch leben!
        int alives = 0;
        for (Snake snake : snakes) {
            int[][] coordinates = snake.getCoordinates();
            // Überprüfe, ob das Array null ist oder leer ist
            if (coordinates != null && coordinates.length > 0) {
                alives++;
            }
        }
        // Game is over when no players have "alive" coordinates OR timer runs out!
        return alives < 1 || timestamp <= 0;
    }

    public double getCookieSpawnRate() {
        return cookieSpawnRate;
    }
    public void setCookieSpawnRate(double cookieSpawnRate) {
        this.cookieSpawnRate = cookieSpawnRate;
    }

}
