package ch.uzh.ifi.hase.soprafs24.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.sql.Date;

/**
 * Lobby Controller
 * This class is responsible for handling all REST request that are related to
 * the lobby.
 * The controller will receive the request and delegate the execution to the
 * LobbyService and finally return the result.
 */
@RestController
public class LobbyController {

  private final LobbyService lobbyService;

  LobbyController(LobbyService lobbyService) {
    this.lobbyService = lobbyService;
  }

  @PostMapping("/lobbies/create")
  @ResponseStatus(HttpStatus.CREATED)
  public LobbyGetDTO createLobby(@RequestHeader("X-Token") String token) {
      // Get the authenticated user from the token
      User user = userService.getUserByToken(token);
      
      // Create a new lobby with that user as host
      // Perhaps generate a random name or ID for the lobby
      Lobby createdLobby = lobbyService.createLobby(user);
      
      // Convert the created lobby to a DTO and return it
      return DTOMapper.INSTANCE.convertEntityToLobbyGetDTO(createdLobby);
  }
}