package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LeaderboardDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * User Controller
 * This class is responsible for handling all REST request that are related to
 * the user.
 * The controller will receive the request and delegate the execution to the
 * UserService and finally return the result.
 */
@RestController
public class UserController {

  private final UserService userService;

  UserController(UserService userService) {
    this.userService = userService;
  }

  @GetMapping("/users")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public List<UserGetDTO> getAllUsers() {
    // fetch all users in the internal representation
    List<User> users = userService.getUsers();
    List<UserGetDTO> userGetDTOs = new ArrayList<>();

    // convert each user to the API representation
    for (User user : users) {
      userGetDTOs.add(DTOMapper.INSTANCE.convertEntityToUserGetDTO(user));
    }
    return userGetDTOs;
  }

  @GetMapping("/users/{id}") // get user by id
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public UserGetDTO getUserById(@PathVariable Long id) {
    User user = userService.getUserById(id);
    if (user == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
    }
    return DTOMapper.INSTANCE.convertEntityToUserGetDTO(user);
  }
    @GetMapping("/leaderboard") // Abrufen der aktuellen globalen Bestenliste
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<LeaderboardDTO> getLeaderboard() {
        // Die Top 10 Spieler nach Level abrufen
        List<User> topPlayers = userService.getTopPlayersByLevel(10);
        List<LeaderboardDTO> leaderboardDTOs = new ArrayList<>();

        // Jeden Benutzer in die speziell für die Bestenliste optimierte API-Repräsentation umwandeln
        for (User user : topPlayers) {
            leaderboardDTOs.add(DTOMapper.INSTANCE.convertEntityToLeaderboardDTO(user));
        }
        return leaderboardDTOs;
    }


  @PostMapping("/users") // register user
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  public UserGetDTO createUser(@RequestBody UserPostDTO userPostDTO) {
    // convert API user to internal representation
    User userInput = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);

    // create user
    User createdUser = userService.createUser(userInput);
    // convert internal representation of user back to API
    UserGetDTO userGetDTO = DTOMapper.INSTANCE.convertEntityToUserGetDTO(createdUser);
    userGetDTO.setToken(createdUser.getToken());
    return userGetDTO;
  }

  @PostMapping("/auth/login") // Action: log in
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public UserGetDTO loginUser(@RequestBody User User) {
    User user = userService.loginUser(User.getUsername(), User.getPassword());
    return DTOMapper.INSTANCE.convertEntityToUserGetDTO(user);
  }

  @PostMapping("/auth/logout") // Not used currently
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public Map<String, String> logoutUser(@RequestBody Map<String, String> requestBody) {
    String token = requestBody.get("token");
    userService.logoutUser(token);
    return Map.of("message", "Logout successful");
  }

  @PostMapping("/auth/verify") // This is just in general: any logged in user can see this
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public Map<String, Object> verifyUser(@RequestBody Map<String, String> requestBody) {
    String token = requestBody.get("formatedToken");
    User user = userService.getUserByToken(token);
    if (user == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
    }
    return Map.of("authorized", true);
  }
  // @PostMapping("/auth/verify/user") // This is if this site is specifically for a user with a certain ID.
  // @ResponseStatus(HttpStatus.OK)
  // @ResponseBody
  // public Map<String, Object> verifyUserEdit(@RequestBody Map<String, String> requestBody) {
  //   String token = requestBody.get("formatedToken");
  //   User user = userService.getUserByToken(token);
  //   if (user == null) {
  //     throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
  //   }
  //   if (user.getId() != Long.parseLong(requestBody.get("id"))) {
  //     throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid token");
  //   }
  //   return Map.of("authorized", true);
  // }

  // @PutMapping("/users/{id}")
  // @ResponseStatus(HttpStatus.OK)
  // @ResponseBody
  // public UserGetDTO editUserProfile(@RequestBody Map<String, Object> requestBody) {
  //   Long id = requestBody.get("id") != null ? Long.valueOf(requestBody.get("id").toString()) : null;
  //   String username = requestBody.get("username") != null ? requestBody.get("username").toString() : null;
  //   String birthDateString = requestBody.get("birthDate") != null ? requestBody.get("birthDate").toString() : null;
  //   Date birthDate = birthDateString != null ? Date.valueOf(birthDateString) : null;

  //   if (id == null) {
  //     throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User ID is required");
  //   }

  //   User user = userService.getUserById(id);
  //   if (user == null) {
  //     throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
  //   }

  //   user.setUsername(username);

  //   User updatedUser = userService.updateUser(id, user);
  //   return DTOMapper.INSTANCE.convertEntityToUserGetDTO(updatedUser);
  // }
}
