package com.LoyaltyEngine.WalletService.controller;

import com.LoyaltyEngine.WalletService.models.domain.WalletDomain;
import com.LoyaltyEngine.WalletService.models.domain.enums.WalletStatus;
import com.LoyaltyEngine.WalletService.models.dto.CreateWalletDTO;
import com.LoyaltyEngine.WalletService.models.entity.Wallet;
import com.LoyaltyEngine.WalletService.services.WalletService;
import com.LoyaltyEngine.WalletService.services.interfaces.WalletMapper;
import com.LoyaltyEngine.WalletService.services.interfaces.WalletRepository;
import com.fasterxml.jackson.databind.JsonNode;
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
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@TestPropertySource(properties = {
        "eureka.client.enabled=false"
})
@Testcontainers
@WithMockUser(roles = "ADMIN")
@AutoConfigureMockMvc(addFilters = false)
public class WalletControllerTests {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18.3");

    @DynamicPropertySource
    static void configProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private WalletMapper walletMapper;

    @Autowired
    private WalletService walletService;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void clearDb() {
        walletRepository.deleteAll();
    }

    @Test
    @DisplayName("Успешное создание кошелька")
    void successfullyCreateWallet() throws Exception {
        //given
        UUID userId = UuidCreator.getTimeOrderedEpoch();
        CreateWalletDTO createWalletDTO = new CreateWalletDTO(userId);

        //when && then
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/wallets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createWalletDTO)))
                .andExpect(status().isCreated());

        List<Wallet> all = walletRepository.findAll();
        Assertions.assertEquals(all.getFirst().getUserId(), userId);
    }

    @Test
    @DisplayName("Кошелек уже существует")
    void walletAlreadyExists() throws Exception {
        //given
        UUID userId = UuidCreator.getTimeOrderedEpoch();
        CreateWalletDTO createWalletDTO = new CreateWalletDTO(userId);

        //when
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/wallets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createWalletDTO)))
                .andExpect(status().isCreated());

        String mvcResult = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createWalletDTO)))
                .andExpect(status().is(409))
                .andReturn()
                .getResponse()
                .getContentAsString();

        //then
        JsonNode response = objectMapper.readTree(mvcResult);
        Assertions.assertEquals("wallet exists", response.get("error").asText());
    }

    @Test
    @DisplayName("Успешное получение баланса пользователя")
    void successfulGetWalletBalance() throws Exception {
        //given
        UUID userId = UuidCreator.getTimeOrderedEpoch();
        walletService.createWallet(userId);
        WalletDomain wallet = walletService.findWalletByUserId(userId);
        wallet.credit(new BigDecimal("10.22"));
        walletRepository.save(walletMapper.domainToEntity(wallet));

        //when
        String result = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1//wallets/%s/balance".formatted(userId)))
                .andExpect(status().is(200))
                .andReturn()
                .getResponse()
                .getContentAsString();

        //then
        Assertions.assertEquals(new BigDecimal("10.22"), new BigDecimal(result));
    }

    @Test
    @DisplayName("Успешное получение истории транзакций")
    void successfulGetTransactionHistory() throws Exception {
        //given
        UUID userId = UuidCreator.getTimeOrderedEpoch();
        walletService.createWallet(userId);
        UUID walletId = walletService.findWalletByUserId(userId).getId().value();

        UUID transactionId = UuidCreator.getTimeOrderedEpoch();
        BigDecimal amountOfCashback = new BigDecimal("10.22");
        BigDecimal amountOfTransaction = new BigDecimal("122.22");
        BigDecimal totalItemPrice = new BigDecimal("122.22");
        Boolean useCashback = false;

        walletService.creditPoints(userId, transactionId, amountOfCashback, useCashback, amountOfTransaction, totalItemPrice);

        //when
        String result = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/wallets/%s/history".formatted(userId)))
                .andExpect(status().is(200))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode response = objectMapper.readTree(result);

        //then
        Assertions.assertEquals(walletId, UuidCreator.fromString(response.get(0).get("walletId").asText()));
    }

    @Test
    @DisplayName("Блок кошелька")
    void successfulBlockWallet() throws Exception {
        //given
        UUID userId = UuidCreator.getTimeOrderedEpoch();
        walletService.createWallet(userId);

        //when && then
        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/wallets/%s/block".formatted(userId)))
                .andExpect(status().is(200));
        WalletStatus status = walletService.findWalletByUserId(userId).getStatus();

        //then
        Assertions.assertEquals(WalletStatus.BLOCKED, status);
    }

    @Test
    @DisplayName("Анблок кошелька")
    void successfulUnblockWallet() throws Exception {
        //given
        UUID userId = UuidCreator.getTimeOrderedEpoch();
        walletService.createWallet(userId);
        walletService.blockWallet(userId);

        //when && then
        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/wallets/%s/unblock".formatted(userId)))
                .andExpect(status().is(200));
        WalletStatus status = walletService.findWalletByUserId(userId).getStatus();

        //then
        Assertions.assertEquals(WalletStatus.ACTIVE, status);
    }
}
