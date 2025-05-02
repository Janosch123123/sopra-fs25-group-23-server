package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;
import java.util.Date;

/**
 * User Service
 * This class is the "worker" and responsible for all functionality related to
 * the user
 * (e.g., it creates, modifies, deletes, finds). The result will be passed back
 * to the caller.
 */
@Service
@Transactional
public class UserService {

  private final Logger log = LoggerFactory.getLogger(UserService.class);

  private final UserRepository userRepository;

  // @Autowired
  public UserService(@Qualifier("userRepository") UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public List<User> getUsers() {
    return this.userRepository.findAll();
  }

  public User createUser(User newUser) {
    newUser.setToken(UUID.randomUUID().toString());
    newUser.setStatus(UserStatus.ONLINE);
    newUser.setCreationDate(new Date());
    newUser.setWins(0);
    newUser.setKills(0);
    newUser.setLevel(1.0);
    newUser.setPlayedGames(0);
    newUser.setLengthPR(0);
    newUser.setWinRate(0);

    checkIfUserExists(newUser);

    // saves the given entity but data is only persisted in the database once
    // flush() is called
    newUser = userRepository.save(newUser);
    userRepository.flush();

    log.debug("Created Information for User: {}", newUser);
    return newUser;
  }
    public int getUserRankInLeaderboard(Long userId) {
        // Hole alle Benutzer, sortiert nach Level und WinRate
        List<User> allUsers = userRepository.findAllByOrderByLevelDescWinRateDesc();

        // Finde den Index des gesuchten Benutzers
        for (int i = 0; i < allUsers.size(); i++) {
            if (allUsers.get(i).getId().equals(userId)) {
                // Index + 1, da Ränge bei 1 beginnen, nicht bei 0
                return i + 1;
            }
        }

        // Wenn der Benutzer nicht gefunden wurde, werfe eine Exception
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found in leaderboard");
    }


    public List<User> getTopPlayersByLevel(int limit) {
        // Wir begrenzen die Anzahl der zurückgegebenen Benutzer auf den angegebenen Wert
        List<User> allUsers = userRepository.findTop10ByOrderByLevelDescWinRateDesc();
        if (allUsers.size() > limit) {
            return allUsers.subList(0, limit);
        }
        return allUsers;
    }

        public User loginUser(String username, String password) {
    User user = userRepository.findByUsername(username);
    if (user != null && user.getPassword().equals(password)) {
      user.setStatus(UserStatus.ONLINE);
      return user;
    } else {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Username or password does not match");
    }
  }

  public ResponseEntity<String> logoutUser(String token) {
    System.out.println("Received Token: " + token);
    User user = userRepository.findByToken(token);
    if (user != null) {
      System.out.println("User found with token: " + user.getToken());
      user.setStatus(UserStatus.OFFLINE);
      userRepository.save(user);
      return ResponseEntity.ok("{\"message\": \"Logout successful\"}");
    } else {
      System.out.println("No user found with token: " + token);
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
    }
  }

  public User getUserById(Long id) {
    return userRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
  }

  public User getUserByToken(String token) {
    User user = userRepository.findByToken(token);

    return user;
  }

  // public User updateUser(Long id, User updatedUser) {
  // System.out.println("Updating User with ID: {}" + id);
  // User user = getUserById(id);
  // user.setUsername(updatedUser.getUsername());
  // user.setPassword(updatedUser.getPassword());
  // user = userRepository.save(user);
  // userRepository.flush();
  // System.out.println("Updated Information for User: {}" + user);
  // return user;
  // }

  /**
   * This is a helper method that will check the uniqueness criteria of the
   * username and the name
   * defined in the User entity. The method will do nothing if the input is unique
   * and throw an error otherwise.
   *
   * @param userToBeCreated
   * @throws org.springframework.web.server.ResponseStatusException
   * @see User
   */
  private void checkIfUserExists(User userToBeCreated) {
    User userByUsername = userRepository.findByUsername(userToBeCreated.getUsername());

    String baseErrorMessage = "The %s provided %s not unique. Therefore, the user could not be created!";
    if (userByUsername != null && !userByUsername.getId().equals(userToBeCreated.getId())) {
      log.error("Conflict: User with username {} already exists", userToBeCreated.getUsername());
      throw new ResponseStatusException(HttpStatus.CONFLICT, String.format(baseErrorMessage, "username", "is"));
    }
  }
}
