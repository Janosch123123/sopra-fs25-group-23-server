package ch.uzh.ifi.hase.soprafs24.repository;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("userRepository")
public interface UserRepository extends JpaRepository<User, Long> {

  User findByUsername(String username);

  User findByToken(String token);

  List<User> findTop10ByOrderByLevelDescWinRateDesc();

    List<User> findAllByOrderByLevelDescWinRateDesc();
}
