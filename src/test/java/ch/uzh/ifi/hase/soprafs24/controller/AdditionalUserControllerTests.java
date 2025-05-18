package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LeaderboardDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
public class AdditionalUserControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    private User testUser;
    private List<User> testLeaderboardUsers;

    @BeforeEach
    public void setup() {
        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testUsername");
        testUser.setPassword("testPassword");
        testUser.setToken("test-token");
        testUser.setStatus(UserStatus.ONLINE);
        
        // Setup leaderboard test users
        testLeaderboardUsers = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            User user = new User();
            user.setId((long) i);
            user.setUsername("player" + i);
            user.setStatus(UserStatus.ONLINE);
            // Assuming the User entity has level property for leaderboard
            // If not available in your actual code, you'll need to adjust accordingly
            // user.setLevel(100 - i * 10); // Descending levels for leaderboard
            testLeaderboardUsers.add(user);
        }
    }

    // Test for login endpoint
    @Test
    public void loginUser_validCredentials_success() throws Exception {
        // given
        Mockito.when(userService.loginUser(anyString(), anyString())).thenReturn(testUser);

        // when/then
        mockMvc.perform(MockMvcRequestBuilders.post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"testUsername\",\"password\":\"testPassword\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testUsername"))
                .andExpect(jsonPath("$.status").value(testUser.getStatus().toString()));
    }

    @Test
    public void loginUser_invalidCredentials_throwsException() throws Exception {
        // given
        Mockito.when(userService.loginUser(anyString(), anyString()))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        // when/then
        mockMvc.perform(MockMvcRequestBuilders.post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"wrongUsername\",\"password\":\"wrongPassword\"}"))
                .andExpect(status().isUnauthorized());
    }

    // Test for logout endpoint
    @Test
    public void logoutUser_validToken_success() throws Exception {
        // given
        // Just let the method run - no need to use doNothing() 
        // as it doesn't return anything and the method exists

        // when/then
        mockMvc.perform(MockMvcRequestBuilders.post("/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"test-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logout successful"));
    }

    // Test for token verification endpoint
    @Test
    public void verifyUser_validToken_success() throws Exception {
        // given
        Mockito.when(userService.getUserByToken(anyString())).thenReturn(testUser);

        // when/then
        mockMvc.perform(MockMvcRequestBuilders.post("/auth/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"formatedToken\":\"test-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorized").value(true));
    }

    @Test
    public void verifyUser_invalidToken_throwsException() throws Exception {
        // given
        Mockito.when(userService.getUserByToken(anyString())).thenReturn(null);

        // when/then
        mockMvc.perform(MockMvcRequestBuilders.post("/auth/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"formatedToken\":\"invalid-token\"}"))
                .andExpect(status().isUnauthorized());
    }

    // Test for leaderboard endpoints
    @Test
    public void getLeaderboard_success() throws Exception {
        // given
        Mockito.when(userService.getTopPlayersByLevel(10)).thenReturn(testLeaderboardUsers);

        // when/then
        mockMvc.perform(MockMvcRequestBuilders.get("/leaderboard")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].username").value("player1"));
    }

    @Test
    public void getLeaderboardRank_validUserId_success() throws Exception {
        // given
        Mockito.when(userService.getUserById(1L)).thenReturn(testUser);
        Mockito.when(userService.getUserRankInLeaderboard(1L)).thenReturn(3);

        // when/then
        mockMvc.perform(MockMvcRequestBuilders.get("/leaderboard/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rank").value(3));
    }

    @Test
    public void getLeaderboardRank_invalidUserId_throwsException() throws Exception {
        // given
        Mockito.when(userService.getUserById(99L)).thenReturn(null);

        // when/then
        mockMvc.perform(MockMvcRequestBuilders.get("/leaderboard/99")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    // Edge case tests
    @Test
    public void getAllUsers_emptyList_returnsEmptyArray() throws Exception {
        // given
        Mockito.when(userService.getUsers()).thenReturn(Collections.emptyList());

        // when/then
        mockMvc.perform(MockMvcRequestBuilders.get("/users")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    public void createUser_missingRequiredField_badRequest() throws Exception {
        // In this case, we need to mock specifically for the case when bad input is provided
        // The controller itself might not be validating the input, so let's simulate
        // the service throwing an exception when invalid input is received
        Mockito.when(userService.createUser(any())).thenThrow(
            new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing required fields"));

        // when/then - testing with missing password
        mockMvc.perform(MockMvcRequestBuilders.post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"testUsername\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void createUser_invalidJsonFormat_badRequest() throws Exception {
        // when/then - testing with invalid JSON
        mockMvc.perform(MockMvcRequestBuilders.post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"testUsername\""))  // Missing closing brace
                .andExpect(status().isBadRequest());
    }
}