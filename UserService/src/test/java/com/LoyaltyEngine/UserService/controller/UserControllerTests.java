package com.LoyaltyEngine.UserService.controller;

import com.LoyaltyEngine.UserService.models.domain.UserDomain;
import com.LoyaltyEngine.UserService.models.enums.FieldToUpdate;
import com.LoyaltyEngine.UserService.models.dto.AuthUserDto;
import com.LoyaltyEngine.UserService.models.dto.CreateUserDTO;
import com.LoyaltyEngine.UserService.models.dto.UpdateUserDTO;
import com.LoyaltyEngine.UserService.services.UserService;
import com.LoyaltyEngine.UserService.services.interfaces.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.f4b6a3.uuid.UuidCreator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
@WithMockUser(roles = "ADMIN")
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
        "eureka.client.enabled=false"
})
public class UserControllerTests {

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
    private UserRepository userRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void clearDb() {
        userRepository.deleteAll();
    }

    private final CreateUserDTO createUserDTO = new CreateUserDTO("test@gmail.com", "Olivia", "Parks", "TestPassword123!!!");

    @Test
    @DisplayName("Успешная регистрация пользователя")
    void successfulRegisterUser() throws Exception {
        //when && then
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createUserDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").value(createUserDTO.email()))
                .andExpect(jsonPath("$.firstName").value(createUserDTO.firstName()))
                .andExpect(jsonPath("$.lastName").value(createUserDTO.lastName()));

        Assertions.assertTrue(userRepository.findUserByEmail(createUserDTO.email()).isPresent());
    }

    @Test
    @DisplayName("Регистрация уже существующего пользователя")
    void registerUserAlreadyExists() throws Exception {
        //given
        userService.createUser(createUserDTO);

        //when && then
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createUserDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("error creating user"));
    }

    @Test
    @DisplayName("Успешная аутентификация")
    void successfulAuthenticate() throws Exception {
        //given
        userService.createUser(createUserDTO);
        AuthUserDto authDto = new AuthUserDto(createUserDTO.email(), createUserDTO.password());

        //when
        String token = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/users/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authDto)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        //then
        Assertions.assertFalse(token.isBlank());
    }

    @Test
    @DisplayName("Неуспешная аутентификация (неверный пароль)")
    void unsuccessfulAuthenticateIncorrectPassword() throws Exception {
        //given
        userService.createUser(createUserDTO);
        AuthUserDto authDto = new AuthUserDto(createUserDTO.email(), "WrongPassword123!");

        //when && then
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/users/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authDto)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("authentication error"));
    }

    @Test
    @DisplayName("Успешное получение всех пользователей")
    void successfulGetAllUsers() throws Exception {
        //given
        userService.createUser(createUserDTO);
        userService.createUser(new CreateUserDTO("another@gmail.com", "John", "Doe", "TestPassword123!!!"));

        //when
        String result = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/users/get_all"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        //then
        JsonNode response = objectMapper.readTree(result);
        Assertions.assertEquals(2, response.size());
    }

    @Test
    @DisplayName("Успешное получение пользователя по id")
    void successfulGetUserById() throws Exception {
        //given
        UserDomain user = userService.createUser(createUserDTO);

        //when && then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/users/id/{id}", user.getId().value()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId().value().toString()))
                .andExpect(jsonPath("$.email").value(user.getEmail()));
    }

    @Test
    @DisplayName("Пользователь не найден по id")
    void unsuccessfulGetUserByIdNotFound() throws Exception {
        //given
        UUID randomId = UuidCreator.getTimeOrderedEpoch();

        //when && then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/users/id/{id}", randomId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("failed to find user"));
    }

    @Test
    @DisplayName("Успешное удаление пользователя")
    void successfulDeleteUser() throws Exception {
        //given
        userService.createUser(createUserDTO);

        //when && then
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/users/{email}", createUserDTO.email()))
                .andExpect(status().isNoContent());

        Assertions.assertTrue(userRepository.findUserByEmail(createUserDTO.email()).isEmpty());
    }

    @Test
    @DisplayName("Удаление несуществующего пользователя")
    void unsuccessfulDeleteUserNotFound() throws Exception {
        //given
        String email = "unknown@gmail.com";

        //when && then
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/users/{email}", email))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("failed to find user"));
    }

    @Test
    @DisplayName("Успешное обновление пользователя")
    void successfulUpdateUser() throws Exception {
        //given
        UserDomain user = userService.createUser(createUserDTO);
        UpdateUserDTO updateDTO = new UpdateUserDTO(FieldToUpdate.FIRST_NAME, null, "UpdatedName", null, null);

        //when && then
        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/users/{email}", user.getEmail())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isNoContent());

        UserDomain updatedUser = userService.findUserByEmail(user.getEmail());
        Assertions.assertEquals("UpdatedName", updatedUser.getFirstName());
    }

    @Test
    @DisplayName("Неуспешное обновление пользователя (тот же email)")
    void unsuccessfulUpdateUserSameEmail() throws Exception {
        //given
        UserDomain user = userService.createUser(createUserDTO);
        UpdateUserDTO updateDTO = new UpdateUserDTO(FieldToUpdate.EMAIL, createUserDTO.email(), null, null, null);

        //when && then
        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/users/{email}", user.getEmail())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("error updating user"));
    }
}
