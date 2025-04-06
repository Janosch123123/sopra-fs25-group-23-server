package ch.uzh.ifi.hase.soprafs24.entity;

import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Game {

    private long gameId;
    private static long idCounter = 0; // only to generate unique id
    private Lobby lobby;
    private List<Snake> snakes;
    private List<Item> items;
    private boolean gameOver;

    public Game() {
        this.gameId = generateUniqueGameId();
        this.snakes = new ArrayList<>(); // Initialize the snakes list
        this.items = new ArrayList<>();  // Initialize the items list
        this.gameOver = false;           // Initialize gameOver flag
    }
    private static synchronized long generateUniqueGameId() {
        idCounter++; // Zähler wird inkrementiert
        return idCounter;}

    public long getGameId() {return gameId;}

    public Lobby getLobby() {return lobby;}
    public void setLobby(Lobby lobby) {this.lobby = lobby;}

    public List<Snake> getSnakes(){return snakes;}
    public void setSnakes(List<Snake> snakes){this.snakes = snakes;}
    public void addSnake(Snake snake){snakes.add(snake);}
    public void removeSnake(Snake snake){snakes.remove(snake);}

    public List<Item> getItems() {return items;}
    public void setItems(List<Item> items) {this.items = items;}
    public void addItem(Item item){items.add(item);}

    public void setGameOver(boolean gameOver) {this.gameOver = gameOver;}
    public boolean getGameOver() {return gameOver;}

    public boolean isGameOver() {
        // zählen wieviele Spieler noch leben!
        int alives = 0;
        for (Snake snake : snakes) {
            if (snake.getCoordinates() != null){
                alives++;
            }
        }
        return alives <= 1; // Game is over when 0 or 1 players remain
    }
}
