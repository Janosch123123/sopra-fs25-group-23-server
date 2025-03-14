package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.Collections;
import java.util.List;

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

/**
 * UserControllerTest
 * This is a WebMvcTest which allows to test the UserController i.e. GET/POST
 * request without actually sending them over the network.
 * This tests if the UserController works.
 */
@WebMvcTest(UserController.class)
public class UserControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private UserService userService;

  private User testUser;

  @BeforeEach
  public void setup() {
    testUser = new User();
    testUser.setId(1L);
    testUser.setUsername("testUsername");
    testUser.setPassword("testPassword");
  }

  @Test
  public void givenUsers_whenGetUsers_thenReturnJsonArray() throws Exception {
    // given
    User user = new User();
    user.setUsername("firstname@lastname");
    user.setStatus(UserStatus.ONLINE);

    List<User> allUsers = Collections.singletonList(user);

    // this mocks the UserService -> we define above what the userService should
    // return when getUsers() is called
    given(userService.getUsers()).willReturn(allUsers);

    // when
    MockHttpServletRequestBuilder getRequest = get("/users").contentType(MediaType.APPLICATION_JSON);

    // then
    mockMvc.perform(getRequest).andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].username", is(user.getUsername())))
        .andExpect(jsonPath("$[0].status", is(user.getStatus().toString())));
  }

  @Test
  public void createUser_validInput_userCreated() throws Exception {
    // given
    User user = new User();
    user.setId(1L);
    user.setUsername("testUsername");
    user.setToken("1");
    user.setStatus(UserStatus.ONLINE);

    UserPostDTO userPostDTO = new UserPostDTO();
    userPostDTO.setUsername("testUsername");

    given(userService.createUser(Mockito.any())).willReturn(user);

    // when/then -> do the request + validate the result
    MockHttpServletRequestBuilder postRequest = post("/users")
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(userPostDTO));

    // then
    mockMvc.perform(postRequest)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id", is(user.getId().intValue())))
        .andExpect(jsonPath("$.username", is(user.getUsername())))
        .andExpect(jsonPath("$.status", is(user.getStatus().toString())));
  }

  @Test
  public void createUser_validInputs_success() throws Exception {
    Mockito.when(userService.createUser(any())).thenReturn(testUser);

    UserPostDTO userPostDTO = new UserPostDTO();
    userPostDTO.setUsername("testUsername");
    userPostDTO.setPassword("testPassword");

    mockMvc.perform(MockMvcRequestBuilders.post("/users")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"username\":\"testUsername\",\"password\":\"testPassword\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.username").value("testUsername"));
  }

  @Test
  public void createUser_duplicateUsername_throwsException() throws Exception {
    Mockito.when(userService.createUser(any())).thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists"));

    mockMvc.perform(MockMvcRequestBuilders.post("/users")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"username\":\"testUsername\",\"password\":\"testPassword\"}"))
        .andExpect(status().isConflict());
  }

  @Test
  public void getUserById_validUserId_success() throws Exception {
    Mockito.when(userService.getUserById(anyLong())).thenReturn(testUser);

    mockMvc.perform(MockMvcRequestBuilders.get("/users/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("testUsername"));
  }

  @Test
  public void getUserById_invalidUserId_throwsException() throws Exception {
    Mockito.when(userService.getUserById(anyLong())).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

    mockMvc.perform(MockMvcRequestBuilders.get("/users/1"))
        .andExpect(status().isNotFound());
  }

  @Test
  public void updateUserProfile_validUserId_success() throws Exception {
    Mockito.when(userService.getUserById(anyLong())).thenReturn(testUser);
    Mockito.when(userService.updateUser(anyLong(), any())).thenReturn(testUser);

    mockMvc.perform(MockMvcRequestBuilders.put("/users/1")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"id\":1,\"username\":\"updatedUsername\",\"birthDate\":\"2000-01-01\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("updatedUsername"));
  }

  @Test
  public void updateUserProfile_invalidUserId_throwsException() throws Exception {
    Mockito.when(userService.getUserById(anyLong())).thenReturn(null);

    mockMvc.perform(MockMvcRequestBuilders.put("/users/1")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"id\":1,\"username\":\"updatedUsername\",\"birthDate\":\"2000-01-01\"}"))
        .andExpect(status().isNotFound());
  }

  /**
   * Helper Method to convert userPostDTO into a JSON string such that the input
   * can be processed
   * Input will look like this: {"name": "Test User", "username": "testUsername"}
   * 
   * @param object
   * @return string
   */
  private String asJsonString(final Object object) {
    try {
      return new ObjectMapper().writeValueAsString(object);
    } catch (JsonProcessingException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          String.format("The request body could not be created.%s", e.toString()));
    }
  }
}