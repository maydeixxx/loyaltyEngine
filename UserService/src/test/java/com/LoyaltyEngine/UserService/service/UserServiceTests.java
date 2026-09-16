package com.LoyaltyEngine.UserService.service;

import com.LoyaltyEngine.UserService.exceptions.AuthenticationException;
import com.LoyaltyEngine.UserService.exceptions.CreateUserException;
import com.LoyaltyEngine.UserService.exceptions.UserNotFoundException;
import com.LoyaltyEngine.UserService.exceptions.UserUpdateException;
import com.LoyaltyEngine.UserService.models.domain.UserDomain;
import com.LoyaltyEngine.UserService.models.enums.FieldToUpdate;
import com.LoyaltyEngine.UserService.models.dto.AuthUserDto;
import com.LoyaltyEngine.UserService.models.dto.CreateUserDTO;
import com.LoyaltyEngine.UserService.models.dto.UpdateUserDTO;
import com.LoyaltyEngine.UserService.services.UserService;
import com.LoyaltyEngine.UserService.services.interfaces.UserRepository;
import com.LoyaltyEngine.UserService.services.security.JwtService;
import com.github.f4b6a3.uuid.UuidCreator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import java.util.List;
import java.util.UUID;

@SpringBootTest
@Testcontainers
@TestPropertySource(properties = {
        "eureka.client.enabled=false",
})
public class UserServiceTests {
    @Container
    private static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18.2");

    @Container
    private static KafkaContainer kafka = new KafkaContainer("apache/kafka:latest");

    @DynamicPropertySource
    private static void configProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void clearDb() {
        userRepository.deleteAll();
    }

    private final CreateUserDTO createUserDTO =  new CreateUserDTO("test@gmail.com", "Olivia", "Parks", "TestPassword123!!!");

    @Test
    @DisplayName("Успешно создание юзера")
    void successfulCreateUser() {
        //when
        userService.createUser(createUserDTO);

        //then
        Assertions.assertTrue(userRepository.findUserByEmail(createUserDTO.email()).isPresent());
    }

    @Test
    @DisplayName("Создание существующего юзера")
    void createExistingUser() {
        //when
        userService.createUser(createUserDTO);

        //then
        Assertions.assertThrows(CreateUserException.class, () -> userService.createUser(createUserDTO), "Email [%s] already registered".formatted(createUserDTO.email()));
    }

    @Test
    @DisplayName("Удаление юзера")
    void successfulDeleteUser() {
        //given
        userService.createUser(createUserDTO);

        //when
        Assertions.assertTrue(userRepository.findUserByEmail(createUserDTO.email()).isPresent());
        userService.deleteUser(createUserDTO.email());
        Assertions.assertFalse(userRepository.findUserByEmail(createUserDTO.email()).isPresent());

    }

    @Test
    @DisplayName("Удаление несуществующего юзера")
    void deleteUnknownUser() {
        //given
        String email = "unknown@gmail.com";

        //when && then
        Assertions.assertThrows(UserNotFoundException.class, () -> userService.deleteUser(email), "User by email [%s] not found".formatted(email));
    }

    @Test
    @DisplayName("Поиск юзера по почте")
    void findUserByEmail() {
        //given
        UserDomain user = userService.createUser(createUserDTO);

        //when
        UserDomain userByEmail = userService.findUserByEmail(user.getEmail());

        //then
        Assertions.assertEquals(user, userByEmail);
    }

    @Test
    @DisplayName("Пользователь не найден по почте")
    void unsuccessfulFindUserByEmail() {
        //given
        String email = "unknown";

        //when && then
        Assertions.assertThrows(UserNotFoundException.class, () -> userService.findUserByEmail(email), "User by email [%s] not found".formatted(email));
    }

    @Test
    @DisplayName("Поиск юзера по id")
    void findUserById() {
        //given
        UserDomain user = userService.createUser(createUserDTO);

        //when
        UserDomain userById = userService.findUserById(user.getId().value());

        //then
        Assertions.assertEquals(user, userById);
    }

    @Test
    @DisplayName("Пользователь не найден по id")
    void unsuccessfulFindUserById() {
        //given
        UUID id = UuidCreator.getTimeOrderedEpoch();

        //when && then
        Assertions.assertThrows(UserNotFoundException.class, () -> userService.findUserById(id), "User by id [%s] not found".formatted(id));
    }

    @Test
    @DisplayName("Получение всех пользователей")
    void getAllUsers() {
        //given
        userService.createUser(createUserDTO);
        userService.createUser(new CreateUserDTO("new@gmail.com", createUserDTO.firstName(), createUserDTO.lastName(), createUserDTO.password()));

        //when
        List<UserDomain> allUsers = userService.getAllUsers();

        //then
        Assertions.assertEquals(2, allUsers.size());
    }

    @Test
    @DisplayName("Успешный логин")
    void successfulLogin() {
        //given
        UserDomain user = userService.createUser(createUserDTO);
        AuthUserDto authUserDto = new AuthUserDto(user.getEmail(), createUserDTO.password());

        //when
        String token = userService.login(authUserDto);
        String email = jwtService.getEmailFromToken(token);

        //then
        Assertions.assertEquals(user.getEmail(), email);
    }

    @Test
    @DisplayName("Неправильный пароль")
    void incorrectPasswordInLoginUser() {
        //given
        userService.createUser(createUserDTO);

        //when && then
        Assertions.assertThrows(AuthenticationException.class, () -> userService.login(new AuthUserDto(createUserDTO.email(), "asdw")), "Password is incorrect");
    }

    @Test
    @DisplayName("Пользователь не найден при логине")
    void userNotFoundInLoginUser() {
        //when && then
        Assertions.assertThrows(UserNotFoundException.class, () -> userService.login(new AuthUserDto("ad", "asdw")), "User by email [%s] not found".formatted("ad"));
    }

    @Test
    @DisplayName("Успешное обновление имени")
    void successfulUpdateFirstName() {
        //given
        UserDomain user = userService.createUser(createUserDTO);
        UpdateUserDTO updateDTO = new UpdateUserDTO(FieldToUpdate.FIRST_NAME, null, "NewName", null, null);

        //when
        userService.updateUser(user.getEmail(), updateDTO);
        UserDomain updatedUser = userService.findUserByEmail(user.getEmail());

        //then
        Assertions.assertEquals("NewName", updatedUser.getFirstName());
    }

    @Test
    @DisplayName("Успешное обновление фамилии")
    void successfulUpdateLastName() {
        //given
        UserDomain user = userService.createUser(createUserDTO);
        UpdateUserDTO updateDTO = new UpdateUserDTO(FieldToUpdate.LAST_NAME, null, null, "NewLastName", null);

        //when
        userService.updateUser(user.getEmail(), updateDTO);
        UserDomain updatedUser = userService.findUserByEmail(user.getEmail());

        //then
        Assertions.assertEquals("NewLastName", updatedUser.getLastName());
    }

    @Test
    @DisplayName("Успешное обновление email")
    void successfulUpdateEmail() {
        //given
        UserDomain user = userService.createUser(createUserDTO);
        String newEmail = "newemail@gmail.com";
        UpdateUserDTO updateDTO = new UpdateUserDTO(FieldToUpdate.EMAIL, newEmail, null, null, null);

        //when
        userService.updateUser(user.getEmail(), updateDTO);
        UserDomain updatedUser = userService.findUserByEmail(newEmail);

        //then
        Assertions.assertEquals(newEmail, updatedUser.getEmail());
    }

    @Test
    @DisplayName("Успешное обновление пароля")
    void successfulUpdatePassword() {
        //given
        UserDomain user = userService.createUser(createUserDTO);
        String newPassword = "NewPassword123!!!";
        UpdateUserDTO updateDTO = new UpdateUserDTO(FieldToUpdate.PASSWORD, null, null, null, newPassword);

        //when
        userService.updateUser(user.getEmail(), updateDTO);
        String token = userService.login(new AuthUserDto(user.getEmail(), newPassword));

        //then
        Assertions.assertNotNull(token);
    }

    @Test
    @DisplayName("Неуспешное обновление на тот же email")
    void unsuccessfulUpdateSameEmail() {
        //given
        UserDomain user = userService.createUser(createUserDTO);
        UpdateUserDTO updateDTO = new UpdateUserDTO(FieldToUpdate.EMAIL, createUserDTO.email(), null, null, null);

        //when && then
        Assertions.assertThrows(UserUpdateException.class, () -> userService.updateUser(user.getEmail(), updateDTO), "You cant enter the same email");
    }

    @Test
    @DisplayName("Неуспешное обновление на то же имя")
    void unsuccessfulUpdateSameFirstName() {
        //given
        UserDomain user = userService.createUser(createUserDTO);
        UpdateUserDTO updateDTO = new UpdateUserDTO(FieldToUpdate.FIRST_NAME, null, createUserDTO.firstName(), null, null);

        //when && then
        Assertions.assertThrows(UserUpdateException.class, () -> userService.updateUser(user.getEmail(), updateDTO), "You already have this first name");
    }

    @Test
    @DisplayName("Неуспешное обновление на ту же фамилию")
    void unsuccessfulUpdateSameLastName() {
        //given
        UserDomain user = userService.createUser(createUserDTO);
        UpdateUserDTO updateDTO = new UpdateUserDTO(FieldToUpdate.LAST_NAME, null, null, createUserDTO.lastName(), null);

        //when && then
        Assertions.assertThrows(UserUpdateException.class, () -> userService.updateUser(user.getEmail(), updateDTO), "You already have this last name");
    }

    @Test
    @DisplayName("Неуспешное обновление несуществующего пользователя")
    void unsuccessfulUpdateUserNotFound() {
        //given
        String email = "unknown@gmail.com";
        UpdateUserDTO updateDTO = new UpdateUserDTO(FieldToUpdate.FIRST_NAME, null, "NewName", null, null);

        //when && then
        Assertions.assertThrows(UserNotFoundException.class, () -> userService.updateUser(email, updateDTO), "User by email [%s] not found".formatted(email));
    }
}
