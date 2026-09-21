package com.LoyaltyEngine.WalletService.controller;

import com.LoyaltyEngine.WalletService.models.dto.CreateWalletDTO;
import com.LoyaltyEngine.WalletService.services.WalletService;
import com.LoyaltyEngine.WalletService.services.interfaces.WalletRepository;
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

import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
@TestPropertySource(properties = {
        "eureka.client.enabled=false"
})
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(roles = "ADMIN")
public class WalletExceptionHandlerTests {
    @Container
    private static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18.2");

    @DynamicPropertySource
    static void configProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletService walletService;

    @BeforeEach
    void clearDb() {
        walletRepository.deleteAll();
    }

    @Test
    @DisplayName("IllegalArgumentException")
    void InvalidArgumentExceptionTest() throws Exception {
        //given
        UUID userId = UuidCreator.getTimeOrderedEpoch();
        walletService.createWallet(userId);
        walletService.blockWallet(userId);

        //when
        String result = mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/wallets/%s/block".formatted(userId)))
                .andExpect(status().is(400))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode response = objectMapper.readTree(result);

        //then
        Assertions.assertEquals("Wallet already blocked", response.get("message").asText());
    }

    @Test
    @DisplayName("WalletNotFoundException")
    void WalletNotFoundExceptionTest() throws Exception {
        //given
        UUID userId = UuidCreator.getTimeOrderedEpoch();

        //when
        String result = mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/wallets/%s/block".formatted(userId)))
                .andExpect(status().is(404))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode response = objectMapper.readTree(result);

        //then
        Assertions.assertEquals("Wallet by user id %s not found".formatted(userId), response.get("message").asText());
    }

    @Test
    @DisplayName("MethodArgumentNotValidException")
    void MethodArgumentNotValidExceptionTest() throws Exception {
        //given
        CreateWalletDTO createWalletDTO = new CreateWalletDTO(null);

        //when
        String result = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createWalletDTO)))
                .andExpect(status().is(400))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode response = objectMapper.readTree(result);

        //then
        Assertions.assertEquals("Error validating data", response.get("error").asText());
    }

    @Test
    @DisplayName("WalletExistsException")
    void WalletExistsExceptionTest() throws Exception {
        //given
        UUID userId = UuidCreator.getTimeOrderedEpoch();
        walletService.createWallet(userId);
        CreateWalletDTO createWalletDTO = new CreateWalletDTO(userId);

        //when
        String result = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createWalletDTO)))
                .andExpect(status().is(409))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode response = objectMapper.readTree(result);

        //then
        Assertions.assertEquals("Wallet exists", response.get("error").asText());
    }

}
