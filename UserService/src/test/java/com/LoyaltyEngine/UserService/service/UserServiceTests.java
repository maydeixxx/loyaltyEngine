package com.LoyaltyEngine.UserService.service;

import com.LoyaltyEngine.UserService.exceptions.CreateUserException;
import com.LoyaltyEngine.UserService.models.dto.CreateUserDTO;
import com.LoyaltyEngine.UserService.services.UserService;
import com.LoyaltyEngine.UserService.services.interfaces.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

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
}
