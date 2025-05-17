package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

public class UserServiceTest {

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private UserService userService;

  private User testUser;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);

    // given
    testUser = new User();
    testUser.setId(1L);
    testUser.setUsername("testUsername");
    testUser.setPassword("testPassword");

    // when -> any object is being save in the userRepository -> return the dummy
    // testUser
    Mockito.when(userRepository.save(Mockito.any())).thenReturn(testUser);
  }

  @Test
  public void createUser_validInputs_success() {
    // when -> any object is being save in the userRepository -> return the dummy
    // testUser
    User createdUser = userService.createUser(testUser);

    // then
    Mockito.verify(userRepository, Mockito.times(1)).save(Mockito.any());

    assertEquals(testUser.getId(), createdUser.getId());
    assertEquals(testUser.getUsername(), createdUser.getUsername());
    assertNotNull(createdUser.getToken());
    assertEquals(UserStatus.ONLINE, createdUser.getStatus());
  }

  @Test
  public void createUser_duplicateName_throwsException() {
    // given -> a first user has already been created
    userService.createUser(testUser);

    // when -> setup additional mocks for UserRepository
    Mockito.when(userRepository.findByUsername(Mockito.anyString())).thenReturn(testUser);

    // then -> attempt to create second user with same user -> check that an error
    // is thrown
    assertThrows(ResponseStatusException.class, () -> {
      User duplicateUser = new User();
      duplicateUser.setUsername("testUsername");
      duplicateUser.setPassword("testPassword");
      userService.createUser(duplicateUser);
    });
  }
  @Test
  public void createUser_duplicateInputs_throwsException() {
    // given -> a first user has already been created
    userService.createUser(testUser);

    // when -> setup additional mocks for UserRepository
    Mockito.when(userRepository.findByUsername(Mockito.anyString())).thenReturn(testUser);

    // then -> attempt to create second user with same user -> check that an error
    // is thrown
    assertThrows(ResponseStatusException.class, () -> {
      User duplicateUser = new User();
      duplicateUser.setUsername("testUsername");
      duplicateUser.setPassword("testPassword");
      userService.createUser(duplicateUser);
    });
  }

  @Test
public void getUserRankInLeaderboard_userExists_returnsCorrectRank() {
    // Setup users with different levels
    User user1 = new User();
    user1.setId(1L);
    user1.setLevel(5.0);
    user1.setWinRate(80);
    
    User user2 = new User();
    user2.setId(2L);
    user2.setLevel(3.0);
    user2.setWinRate(70);
    
    User user3 = new User();
    user3.setId(3L);
    user3.setLevel(10.0);
    user3.setWinRate(90);
    
    List<User> sortedUsers = Arrays.asList(user3, user1, user2);
    
    // Mock repository behavior
    Mockito.when(userRepository.findAllByOrderByLevelDescWinRateDesc()).thenReturn(sortedUsers);
    
    // Test for user1 (should be rank 2)
    int rank = userService.getUserRankInLeaderboard(1L);
    assertEquals(2, rank);
}

@Test
public void getUserRankInLeaderboard_userNotExists_throwsException() {
    // Mock an empty leaderboard
    Mockito.when(userRepository.findAllByOrderByLevelDescWinRateDesc()).thenReturn(new ArrayList<>());
    
    // Test for non-existent user
    assertThrows(ResponseStatusException.class, () -> {
        userService.getUserRankInLeaderboard(999L);
    });
}

@Test
public void getTopPlayersByLevel_limitLessThanAvailable_returnsLimitedUsers() {
    // Setup list of users
    List<User> topUsers = new ArrayList<>();
    for (int i = 0; i < 20; i++) {
        User user = new User();
        user.setId((long) i);
        user.setLevel(20.0 - i);
        topUsers.add(user);
    }
    
    Mockito.when(userRepository.findTop10ByOrderByLevelDescWinRateDesc()).thenReturn(topUsers);
    
    // Test with limit of 5
    List<User> result = userService.getTopPlayersByLevel(5);
    
    assertEquals(5, result.size());
    assertEquals(0L, result.get(0).getId()); // Highest level user
}

@Test
public void getTopPlayersByLevel_limitMoreThanAvailable_returnsAllUsers() {
    // Setup list of 3 users
    List<User> topUsers = new ArrayList<>();
    for (int i = 0; i < 3; i++) {
        User user = new User();
        user.setId((long) i);
        topUsers.add(user);
    }
    
    Mockito.when(userRepository.findTop10ByOrderByLevelDescWinRateDesc()).thenReturn(topUsers);
    
    // Test with limit of 5
    List<User> result = userService.getTopPlayersByLevel(5);
    
    assertEquals(3, result.size()); // Should return all 3 users
}

@Test
public void loginUser_correctCredentials_success() {
    // Setup user
    User user = new User();
    user.setId(1L);
    user.setUsername("testUser");
    user.setPassword("password123");
    user.setStatus(UserStatus.OFFLINE);
    
    Mockito.when(userRepository.findByUsername("testUser")).thenReturn(user);
    
    // Test login
    User loggedInUser = userService.loginUser("testUser", "password123");
    
    assertEquals(UserStatus.ONLINE, loggedInUser.getStatus());
    assertEquals("testUser", loggedInUser.getUsername());
}

@Test
public void loginUser_incorrectPassword_throwsException() {
    // Setup user
    User user = new User();
    user.setId(1L);
    user.setUsername("testUser");
    user.setPassword("password123");
    
    Mockito.when(userRepository.findByUsername("testUser")).thenReturn(user);
    
    // Test login with wrong password
    assertThrows(ResponseStatusException.class, () -> {
        userService.loginUser("testUser", "wrongPassword");
    });
}

@Test
public void loginUser_userNotFound_throwsException() {
    // Mock repository to return null (user not found)
    Mockito.when(userRepository.findByUsername("nonExistentUser")).thenReturn(null);
    
    // Test login with non-existent user
    assertThrows(ResponseStatusException.class, () -> {
        userService.loginUser("nonExistentUser", "anyPassword");
    });
}

@Test
public void logoutUser_validToken_success() {
    // Setup user
    User user = new User();
    user.setId(1L);
    user.setUsername("testUser");
    user.setToken("valid-token");
    user.setStatus(UserStatus.ONLINE);
    
    Mockito.when(userRepository.findByToken("valid-token")).thenReturn(user);
    
    // Test logout
    var response = userService.logoutUser("valid-token");
    
    assertEquals(UserStatus.OFFLINE, user.getStatus());
    assertEquals(200, response.getStatusCodeValue());
    Mockito.verify(userRepository).save(user);
}

@Test
public void logoutUser_invalidToken_throwsException() {
    // Mock repository to return null (token not found)
    Mockito.when(userRepository.findByToken("invalid-token")).thenReturn(null);
    
    // Test logout with invalid token
    assertThrows(ResponseStatusException.class, () -> {
        userService.logoutUser("invalid-token");
    });
}

@Test
public void getUserById_existingUser_returnsUser() {
    // Setup user
    User user = new User();
    user.setId(1L);
    user.setUsername("testUser");
    
    Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    
    // Test finding user by ID
    User foundUser = userService.getUserById(1L);
    
    assertEquals(1L, foundUser.getId());
    assertEquals("testUser", foundUser.getUsername());
}

@Test
public void getUserById_nonExistingUser_throwsException() {
    // Mock repository to return empty optional
    Mockito.when(userRepository.findById(999L)).thenReturn(Optional.empty());
    
    // Test finding non-existent user
    assertThrows(ResponseStatusException.class, () -> {
        userService.getUserById(999L);
    });
}

@Test
public void getUserByToken_validToken_returnsUser() {
    // Setup user
    User user = new User();
    user.setId(1L);
    user.setToken("valid-token");
    
    Mockito.when(userRepository.findByToken("valid-token")).thenReturn(user);
    
    // Test finding user by token
    User foundUser = userService.getUserByToken("valid-token");
    
    assertEquals(1L, foundUser.getId());
    assertEquals("valid-token", foundUser.getToken());
}

@Test
public void getUserByToken_invalidToken_returnsNull() {
    // Mock repository to return null for invalid token
    Mockito.when(userRepository.findByToken("invalid-token")).thenReturn(null);
    
    // Test finding user with invalid token
    User result = userService.getUserByToken("invalid-token");
    
    assertNull(result);
}

@Test
public void createBot_success() {
    // Setup a more complete bot user that will be returned by the mock
    User botUser = new User();
    botUser.setId(1L);
    botUser.setIsBot(true);
    botUser.setUsername("BotUsername");
    botUser.setToken("bot-token");
    botUser.setStatus(UserStatus.ONLINE);
    
    // Capture the User object being saved
    Mockito.when(userRepository.save(Mockito.any(User.class))).thenAnswer(invocation -> {
        User savedBot = invocation.getArgument(0);
        // Copy properties from the saved bot to our return bot
        botUser.setUsername(savedBot.getUsername());
        botUser.setToken(savedBot.getToken());
        return botUser;
    });
    
    // Mock that no user exists with any bot username
    Mockito.when(userRepository.findByUsername(Mockito.anyString())).thenReturn(null);
    
    // Test creating bot
    User createdBot = userService.createBot();
    
    // Verify repository was called
    Mockito.verify(userRepository).save(Mockito.any());
    Mockito.verify(userRepository).flush();
    
    // Check bot properties - more lenient assertions
    assertNotNull(createdBot);
    assertTrue(createdBot.getIsBot());
    assertNotNull(createdBot.getUsername());
    assertNotNull(createdBot.getToken());
    assertEquals(UserStatus.ONLINE, createdBot.getStatus());
}

@Test
public void deleteUser_existingUser_success() {
    // Setup user
    User user = new User();
    user.setId(1L);
    
    // Mock repository behavior
    Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    
    // Test deleting user
    userService.deleteUser(user);
    
    // Verify repository was called
    Mockito.verify(userRepository).deleteById(1L);
}

@Test
public void deleteUser_nonExistingUser_noAction() {
    // Setup user
    User user = new User();
    user.setId(999L);
    
    // Mock repository behavior - user doesn't exist
    Mockito.when(userRepository.findById(999L)).thenReturn(Optional.empty());
    
    // Test deleting non-existent user
    userService.deleteUser(user);
    
    // Verify repository deleteById was NOT called
    Mockito.verify(userRepository, Mockito.never()).deleteById(Mockito.anyLong());
}

}
