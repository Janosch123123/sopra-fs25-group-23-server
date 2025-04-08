package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.repository.LobbyRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class LobbyController {

    private final LobbyRepository lobbyRepository;

    public LobbyController(LobbyRepository lobbyRepository) {
        this.lobbyRepository = lobbyRepository;
    }

    @GetMapping("/lobbies")
    @ResponseStatus(HttpStatus.OK)
    public List<Lobby> getAllLobbies() {
        return lobbyRepository.findAll();
    }

//    @GetMapping("/lobbies/{lobbyId}")
//    @ResponseStatus(HttpStatus.OK)
//    public Lobby getLobbyById(@PathVariable Long lobbyId) {
//        return lobbyRepository.findById(lobbyId)
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
//                    "Lobby with ID " + lobbyId + " not found"));
//    }
}