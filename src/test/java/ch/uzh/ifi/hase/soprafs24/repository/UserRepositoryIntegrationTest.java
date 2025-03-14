package ch.uzh.ifi.hase.soprafs24.repository;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.service.UserService;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Date;

@DataJpaTest
public class UserRepositoryIntegrationTest {

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private UserRepository userRepository;


  // Removed this Test because according to the rqeuirements, the User should not be able to "have" a name
  // @Test
  // public void findByName_success() {
  //   // given
  //   User user = new User();
  //   user.setName("Firstname Lastname");
  //   user.setUsername("firstname@lastname");
  //   user.setStatus(UserStatus.OFFLINE);
  //   user.setToken("1");

  //   entityManager.persist(user);
  //   entityManager.flush();

  //   // when
  //   User found = userRepository.findByName(user.getName());

  //   // then
  //   assertNotNull(found.getId());
  //   assertEquals(found.getName(), user.getName());
  //   assertEquals(found.getUsername(), user.getUsername());
  //   assertEquals(found.getToken(), user.getToken());
  //   assertEquals(found.getStatus(), user.getStatus());
  // }

  @Test
  public void findByUsername_success() {
    // given
    User user = new User();
    user.setUsername("firstname@lastname");
    user.setStatus(UserStatus.ONLINE);
    user.setToken("1");
    user.setCreationDate(new Date());
    user.setPassword("password123");

    entityManager.persist(user);
    entityManager.flush();

    // when
    User found = userRepository.findByUsername(user.getUsername());

    // then
    assertNotNull(found.getId());
    assertEquals(found.getUsername(), user.getUsername());
    assertEquals(found.getToken(), user.getToken());
    assertEquals(found.getStatus(), user.getStatus());
   }
   @Test
   public void findByUsername_failure() {
     // given
     User user = new User();
     user.setUsername("firstname@lastname");
     user.setStatus(UserStatus.ONLINE);
     user.setToken("1");
     user.setCreationDate(new Date());
     user.setPassword("password123");
 
     entityManager.persist(user);
     entityManager.flush();
 
     // when
     User found = userRepository.findByUsername("DoesNotExist");
 
     // then
     assertEquals(null, found);
    }
}
