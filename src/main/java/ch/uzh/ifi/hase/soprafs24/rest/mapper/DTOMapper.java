package ch.uzh.ifi.hase.soprafs24.rest.mapper;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LeaderboardDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;

import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

/**
 * DTOMapper
 * This class is responsible for generating classes that will automatically
 * transform/map the internal representation
 * of an entity (e.g., the User) to the external/API representation (e.g.,
 * UserGetDTO for getting, UserPostDTO for creating)
 * and vice versa.
 * Additional mappers can be defined for new entities.
 * Always created one mapper for getting information (GET) and one mapper for
 * creating information (POST).
 */
@Mapper
public interface DTOMapper {

  DTOMapper INSTANCE = Mappers.getMapper(DTOMapper.class);

  @Mapping(source = "username", target = "username")
  @Mapping(source = "password", target = "password")
  User convertUserPostDTOtoEntity(UserPostDTO userPostDTO);

  @Mapping(source = "id", target = "id")
  @Mapping(source = "username", target = "username")
  @Mapping(source = "status", target = "status")
  @Mapping(source = "creationDate", target = "creationDate")
  @Mapping(source = "token", target = "token")
  @Mapping(source = "wins", target = "wins")
  @Mapping(source = "kills", target = "kills")
  @Mapping(source = "level", target = "level")
  @Mapping(source = "lobbyCode", target = "lobbyCode")
  @Mapping(source = "playedGames", target = "playedGames")
  @Mapping(source = "lengthPR", target = "lengthPR")
  UserGetDTO convertEntityToUserGetDTO(User user);

@Mapping(source = "id", target = "id")
@Mapping(source = "username", target = "username")
@Mapping(source = "level", target = "level")
@Mapping(source = "wins", target = "wins")
@Mapping(source = "kills", target = "kills")
@Mapping(source = "playedGames", target = "playedGames")
@Mapping(source = "lengthPR", target = "lengthPR")
@Mapping(source = "winRate", target = "winRate")
LeaderboardDTO convertEntityToLeaderboardDTO(User user);
}

