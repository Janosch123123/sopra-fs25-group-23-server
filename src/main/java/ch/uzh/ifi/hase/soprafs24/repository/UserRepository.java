package ch.uzh.ifi.hase.soprafs24.repository;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository("userRepository")
public interface UserRepository extends JpaRepository<User, Long> {

  User findByUsername(String username);

  User findByToken(String token);

//  Optional<User> findById(Long id);   bereits implementiert von JPA
    // user fetch: use .isPresent which returns a boolean. and if true us .get to get a User object.

}
