# 🐍 Snake With Friends

Snake With Friends is a modern multiplayer twist on the classic Snake game, with a focus on fun, multiplayer interaction, and personalization. Whether alone, with friends, or strangers: there's something for everyone, even your favorite background music!

## Technologies Used

The technologies we used for this project are:

* Java
* Gradle
* Spring Boot (Backend)
* WebSockets (Real-time Communication)
* TypeScript & React (Frontend)
* Google App Engine (Deployment)
* Docker (Containerization)

## High-Level Components

Our backend is structured into several main components that enable the game's core functionality.

### Lobby Management

Responsible for:

* Creating and joining lobbies
* Managing lobby participants
* Handling lobby settings
* Broadcasting messages to players in a lobby

**Main class**: [`LobbyService.java`](https://github.com/Janosch123123/sopra-fs25-group-23-server/blob/main/src/main/java/ch/uzh/ifi/hase/soprafs24/service/LobbyService.java)  
**Controller**: [`LobbyController.java`](https://github.com/Janosch123123/sopra-fs25-group-23-server/blob/main/src/main/java/ch/uzh/ifi/hase/soprafs24/controller/LobbyController.java)  
**Repository**: [`LobbyRepository.java`](https://github.com/Janosch123123/sopra-fs25-group-23-server/blob/main/src/main/java/ch/uzh/ifi/hase/soprafs24/repository/LobbyRepository.java)  
**Session management**: [`WebSocketHandler.java`](https://github.com/Janosch123123/sopra-fs25-group-23-server/blob/main/src/main/java/ch/uzh/ifi/hase/soprafs24/handler/WebSocketHandler.java)  

### User Management

Responsible for:

* User registration and login
* Managing user profiles and statuses
* Tracking user statistics

**Main class**: [`UserService.java`](https://github.com/Janosch123123/sopra-fs25-group-23-server/blob/main/src/main/java/ch/uzh/ifi/hase/soprafs24/service/UserService.java)  
**Controller**: [`UserController.java`](https://github.com/Janosch123123/sopra-fs25-group-23-server/blob/main/src/main/java/ch/uzh/ifi/hase/soprafs24/controller/UserController.java)  
**Entities**: [`User.java`](https://github.com/Janosch123123/sopra-fs25-group-23-server/blob/main/src/main/java/ch/uzh/ifi/hase/soprafs24/entity/User.java)  
**DTOs**: [`UserPostDTO.java`](https://github.com/Janosch123123/sopra-fs25-group-23-server/blob/main/src/main/java/ch/uzh/ifi/hase/soprafs24/rest/dto/UserPostDTO.java), [`UserGetDTO.java`](https://github.com/Janosch123123/sopra-fs25-group-23-server/blob/main/src/main/java/ch/uzh/ifi/hase/soprafs24/rest/dto/UserGetDTO.java)  
**Repository**: [`UserRepository.java`](https://github.com/Janosch123123/sopra-fs25-group-23-server/blob/main/src/main/java/ch/uzh/ifi/hase/soprafs24/repository/UserRepository.java)  
**Mapper**: [`DTOMapper.java`](https://github.com/Janosch123123/sopra-fs25-group-23-server/blob/main/src/main/java/ch/uzh/ifi/hase/soprafs24/rest/mapper/DTOMapper.java)  
**Constants**: [`UserStatus.java`](https://github.com/Janosch123123/sopra-fs25-group-23-server/blob/main/src/main/java/ch/uzh/ifi/hase/soprafs24/constant/UserStatus.java)  

### Game Logic

Responsible for:

* Initializing games
* Managing snake movements and game ticks
* Handling power-ups, power-downs, and cookies
* Triggering end-game conditions
* Broadcasting game state updates via WebSockets
* Updating global leaderboard and user statistics

**Main class**: [`GameService.java`](https://github.com/Janosch123123/sopra-fs25-group-23-server/blob/main/src/main/java/ch/uzh/ifi/hase/soprafs24/service/GameService.java)  
**Sub-services**: [`SnakeService.java`](https://github.com/Janosch123123/sopra-fs25-group-23-server/blob/main/src/main/java/ch/uzh/ifi/hase/soprafs24/service/SnakeService.java), [`BotService.java`](https://github.com/Janosch123123/sopra-fs25-group-23-server/blob/main/src/main/java/ch/uzh/ifi/hase/soprafs24/service/BotService.java)  
**Entities**: [`Game.java`](https://github.com/Janosch123123/sopra-fs25-group-23-server/blob/main/src/main/java/ch/uzh/ifi/hase/soprafs24/entity/Game.java), [`Snake.java`](https://github.com/Janosch123123/sopra-fs25-group-23-server/blob/main/src/main/java/ch/uzh/ifi/hase/soprafs24/entity/Snake.java), [`Item.java`](https://github.com/Janosch123123/sopra-fs25-group-23-server/blob/main/src/main/java/ch/uzh/ifi/hase/soprafs24/entity/Item.java)  
**Handler**: [`WebSocketHandler.java`](https://github.com/Janosch123123/sopra-fs25-group-23-server/blob/main/src/main/java/ch/uzh/ifi/hase/soprafs24/handler/WebSocketHandler.java)  
**Configuration**: [`WebSocketConfig.java`](https://github.com/Janosch123123/sopra-fs25-group-23-server/blob/main/src/main/java/ch/uzh/ifi/hase/soprafs24/config/WebSocketConfig.java)  

### Power-Ups and Power-Downs

Special in-game items with unique effects, handled as part of the game loop.

* Power-ups: [`Cookie.java`](https://github.com/Janosch123123/sopra-fs25-group-23-server/blob/main/src/main/java/ch/uzh/ifi/hase/soprafs24/entity/Powerups/Cookie.java), [`GoldenCookie.java`](https://github.com/Janosch123123/sopra-fs25-group-23-server/blob/main/src/main/java/ch/uzh/ifi/hase/soprafs24/entity/Powerups/GoldenCookie.java), [`Multiplier.java`](https://github.com/Janosch123123/sopra-fs25-group-23-server/blob/main/src/main/java/ch/uzh/ifi/hase/soprafs24/entity/Powerups/Multiplier.java)  
* Power-downs: [`Divider.java`](https://github.com/Janosch123123/sopra-fs25-group-23-server/blob/main/src/main/java/ch/uzh/ifi/hase/soprafs24/entity/Powerdowns/Divider.java), [`ReverseControl.java`](https://github.com/Janosch123123/sopra-fs25-group-23-server/blob/main/src/main/java/ch/uzh/ifi/hase/soprafs24/entity/Powerdowns/ReverseControl.java)  

These items are attached to the `Item` entity and controlled via game rules and the `GameService`.


## Launch & Deployment

### Prerequisites

#### Spring Boot

Spring Boot is the backbone technology in this project.

Getting started with Spring Boot:
-   Documentation: https://docs.spring.io/spring-boot/docs/current/reference/html/index.html
-   Guides: http://spring.io/guides
    -   Building a RESTful Web Service: http://spring.io/guides/gs/rest-service/
    -   Building REST services with Spring: https://spring.io/guides/tutorials/rest/

### Development

#### Build

You can use the local Gradle Wrapper to build the application.
-   macOS: `./gradlew`
-   Linux: `./gradlew`
-   Windows: `./gradlew.bat`

More Information about [Gradle Wrapper](https://docs.gradle.org/current/userguide/gradle_wrapper.html) and [Gradle](https://gradle.org/docs/).


Then, you can build the project using:

```bash
./gradlew build
```

#### Run

```bash
./gradlew bootRun
```

You can verify that the server is running by visiting `localhost:8080` in your browser.

#### Test

```bash
./gradlew test
```

Useful guide on testing: [link](https://www.baeldung.com/spring-boot-testing).

#### Development Mode

You can start the backend in development mode, this will automatically trigger a new build and reload the application once the content of a file has been changed.

Start two terminal windows and run:

`./gradlew build --continuous`

and in the other one:

`./gradlew bootRun`

If you want to avoid running all tests with every change, use the following command instead:

`./gradlew build --continuous -xtest`

#### API Endpoint Testing with Postman

We recommend using [Postman](https://www.getpostman.com) to test your API Endpoints.

#### Debugging

If something is not working and/or you don't know what is going on, we recommend using a debugger and step-through the process step-by-step.

To configure a debugger for SpringBoot's Tomcat servlet (i.e. the process you start with `./gradlew bootRun` command), do the following:

1. Open Tab: **Run**/Edit Configurations
2. Add a new Remote Configuration and name it properly
3. Start the Server in Debug mode: `./gradlew bootRun --debug-jvm`
4. Press `Shift + F9` or the use **Run**/Debug "Name of your task"
5. Set breakpoints in the application where you need it
6. Step through the process one step at a time

#### How to do releases

We have set up a Github Actions Workflow that automatically deploys whenever code is pushed on the main branch.

* Workflow file for main commits: `main.yml`
* Workflow file for pull requests (for running tests): `pr.yml`

--- 

## 👥 Team

- [Marc Mahler](https://github.com/MarcMahler)
- [Janosch Beck](https://github.com/Janosch123123)
- [Jarno Bucher](https://github.com/StalyTV)
- [Joel Schmidt](https://github.com/jojo2-8902)
- [Luke Fohringer](https://github.com/LuckyLuke637)

---

## 🙏 Acknowledgments

- SOPRA team at UZH
- Inspired by classic Snake games
- Feedback from testers during development

---

## 📋 Roadmap

For developers interested in contributing to Snake With Friends, here are the top features we'd like to see implemented:

1. **Mobile Support**: Adapt the user interface for mobile devices and implement touch controls for the snake.

2. **Custom Snake Customization**: Allow users to customize their snake's appearance with different skins, patterns, or accessories that can be unlocked through gameplay.

3. **Tournament Mode**: Implement a structured tournament system where players can compete in brackets to determine an ultimate winner across multiple games.

---

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](https://github.com/Janosch123123/sopra-fs25-group-23-server/blob/main/LICENSE) file for details.