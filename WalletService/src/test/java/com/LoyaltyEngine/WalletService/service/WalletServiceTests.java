package com.LoyaltyEngine.WalletService.service;

import com.LoyaltyEngine.WalletService.exceptions.InsufficientFundsException;
import com.LoyaltyEngine.WalletService.exceptions.WalletBlockedException;
import com.LoyaltyEngine.WalletService.exceptions.WalletExistsException;
import com.LoyaltyEngine.WalletService.exceptions.WalletNotFoundException;
import com.LoyaltyEngine.WalletService.models.domain.WalletDomain;
import com.LoyaltyEngine.WalletService.models.domain.WalletTransactionDomain;
import com.LoyaltyEngine.WalletService.models.enums.WalletStatus;
import com.LoyaltyEngine.WalletService.services.WalletService;
import com.LoyaltyEngine.WalletService.services.interfaces.WalletMapper;
import com.LoyaltyEngine.WalletService.services.interfaces.WalletRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Assertions;
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

import com.LoyaltyEngine.WalletService.models.entity.OutboxEvent;
import com.LoyaltyEngine.WalletService.services.interfaces.OutboxEventRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@SpringBootTest
@Testcontainers
@Transactional
@TestPropertySource(properties = {
        "eureka.client.enabled=false"
})
public class WalletServiceTests {
    @Container
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:18.3");

    @DynamicPropertySource
    static void configProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> postgreSQLContainer.getJdbcUrl());
        registry.add("spring.datasource.username", () -> postgreSQLContainer.getUsername());
        registry.add("spring.datasource.password", () -> postgreSQLContainer.getPassword());
    }

    @Autowired
    private WalletService walletService;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private WalletMapper mapper;

    @Test
    @DisplayName("Успешное создание кошелька")
    void successfullyCreatedWallet() {
        //given
        UUID userId = UuidCreator.getTimeOrderedEpoch();

        //when
        walletService.createWallet(userId);

        //then
        WalletDomain wallet = walletService.findWalletByUserId(userId);
        Assertions.assertEquals(wallet.getUserId().value(), userId);
    }

    @Test
    @DisplayName("Неуспешное создание кошелька")
    void unsuccessfulCreateWallet() {
        //given
        UUID userId = UuidCreator.getTimeOrderedEpoch();

        //when / then
        walletService.createWallet(userId);
        Assertions.assertThrows(WalletExistsException.class, () -> walletService.createWallet(userId), () -> "Wallet for user [%s] already created".formatted(userId));
    }

    @Test
    @DisplayName("Неуспешное создание кошелька NPE")
    void unsuccessfulCreateWalletNPE() {
        //when / then
        Assertions.assertThrows(RuntimeException.class, () -> walletService.createWallet(null));
    }

    @Test
    @DisplayName("Успешная оплата без использования кешбека")
    void successfulCreditPoints() throws JsonProcessingException {
        //given
        UUID userId = UuidCreator.getTimeOrderedEpoch();
        walletService.createWallet(userId);

        UUID transactionId = UuidCreator.getTimeOrderedEpoch();
        BigDecimal amountOfCashback = new BigDecimal("10.22");
        BigDecimal amountOfTransaction = new BigDecimal("122.22");
        BigDecimal totalItemPrice = new BigDecimal("122.22");
        Boolean useCashback = false;

        //when
        walletService.creditPoints(userId, transactionId, amountOfCashback, useCashback, amountOfTransaction, totalItemPrice);
        WalletDomain wallet = walletService.findWalletByUserId(userId);

        //then
        Assertions.assertEquals(amountOfCashback, wallet.getBalance().amount());
    }

    @Test
    @DisplayName("Недостаточно средств при оплате")
    void insufficientFunds() throws Exception {
        //given
        UUID userId = UuidCreator.getTimeOrderedEpoch();
        walletService.createWallet(userId);

        UUID transactionId = UuidCreator.getTimeOrderedEpoch();
        BigDecimal amountOfCashback = new BigDecimal("10.22");
        BigDecimal amountOfTransaction = new BigDecimal("112.22");
        BigDecimal totalItemPrice = new BigDecimal("122.22");
        Boolean useCashback = false;

        //when
        walletService.creditPoints(userId, transactionId, amountOfCashback, useCashback, amountOfTransaction, totalItemPrice);

        //then
        List<OutboxEvent> events = outboxEventRepository.findAll();
        Assertions.assertFalse(events.isEmpty());
        OutboxEvent lastEvent = events.get(events.size() - 1);
        Assertions.assertEquals("points_failed", lastEvent.getEventType());
        Assertions.assertTrue(lastEvent.getPayload().contains("Insufficient funds"));
    }

    @Test
    @DisplayName("Кошелек заблокирован")
    void walletIsBlocked() throws Exception {
        //given
        UUID userId = UuidCreator.getTimeOrderedEpoch();
        walletService.createWallet(userId);
        UUID walletId = walletService.findWalletByUserId(userId).getId().value();
        walletService.blockWallet(userId);

        UUID transactionId = UuidCreator.getTimeOrderedEpoch();
        BigDecimal amountOfCashback = new BigDecimal("10.22");
        BigDecimal amountOfTransaction = new BigDecimal("112.22");
        BigDecimal totalItemPrice = new BigDecimal("122.22");
        Boolean useCashback = false;

        //when
        walletService.creditPoints(userId, transactionId, amountOfCashback, useCashback, amountOfTransaction, totalItemPrice);

        //then
        List<OutboxEvent> events = outboxEventRepository.findAll();
        Assertions.assertFalse(events.isEmpty());
        OutboxEvent lastEvent = events.get(events.size() - 1);
        Assertions.assertEquals("points_failed", lastEvent.getEventType());
        Assertions.assertTrue(lastEvent.getPayload().contains("Wallet is blocked"));
    }

    @Test
    @DisplayName("Кошелек не найден")
    void walletNotFound() throws Exception {
        //given
        UUID userId = UuidCreator.getTimeOrderedEpoch();
        UUID transactionId = UuidCreator.getTimeOrderedEpoch();
        BigDecimal amountOfCashback = new BigDecimal("10.22");
        BigDecimal amountOfTransaction = new BigDecimal("112.22");
        BigDecimal totalItemPrice = new BigDecimal("122.22");
        Boolean useCashback = false;

        //when
        walletService.creditPoints(userId, transactionId, amountOfCashback, useCashback, amountOfTransaction, totalItemPrice);

        //then
        List<OutboxEvent> events = outboxEventRepository.findAll();
        Assertions.assertFalse(events.isEmpty());
        OutboxEvent lastEvent = events.get(events.size() - 1);
        Assertions.assertEquals("points_failed", lastEvent.getEventType());
        Assertions.assertTrue(lastEvent.getPayload().contains("not found"));
    }

    @Test
    @DisplayName("Успешная оплата c использованием кешбека")
    void successfulCreditPointsWithCashback() throws JsonProcessingException {
        //given
        UUID userId = UuidCreator.getTimeOrderedEpoch();
        walletService.createWallet(userId);
        WalletDomain wallet = walletService.findWalletByUserId(userId);
        wallet.credit(new BigDecimal("10.00"));
        walletRepository.save(mapper.domainToEntity(wallet));

        UUID transactionId = UuidCreator.getTimeOrderedEpoch();
        BigDecimal amountOfCashback = new BigDecimal("10.00");
        BigDecimal amountOfTransaction = new BigDecimal("112.22");
        BigDecimal totalItemPrice = new BigDecimal("122.22");
        Boolean useCashback = true;

        //when
        walletService.creditPoints(userId, transactionId, amountOfCashback, useCashback, amountOfTransaction, totalItemPrice);
        wallet = walletService.findWalletByUserId(userId);

        //then
        Assertions.assertEquals(new BigDecimal("0.00"), wallet.getBalance().amount());
    }

    @Test
    @DisplayName("Неуспешная оплата c использованием кешбека")
    void unSuccessfulCreditPointsWithCashback() throws Exception {
        //given
        UUID userId = UuidCreator.getTimeOrderedEpoch();
        walletService.createWallet(userId);

        UUID transactionId = UuidCreator.getTimeOrderedEpoch();
        BigDecimal amountOfCashback = new BigDecimal("10.00");
        BigDecimal amountOfTransaction = new BigDecimal("112.22");
        BigDecimal totalItemPrice = new BigDecimal("122.22");
        Boolean useCashback = true;

        //when
        walletService.creditPoints(userId, transactionId, amountOfCashback, useCashback, amountOfTransaction, totalItemPrice);

        //then
        List<OutboxEvent> events = outboxEventRepository.findAll();
        Assertions.assertFalse(events.isEmpty());
        OutboxEvent lastEvent = events.get(events.size() - 1);
        Assertions.assertEquals("points_failed", lastEvent.getEventType());
        Assertions.assertTrue(lastEvent.getPayload().contains("Insufficient funds"));
    }

    @Test
    @DisplayName("Неуспешная оплата c использованием кешбека (Баланс меньше чем кешбек нужный для оплаты)")
    void unSuccessfulCreditPointsWithCashbackBalanceIsLessThanCashback() throws Exception {
        //given
        UUID userId = UuidCreator.getTimeOrderedEpoch();
        walletService.createWallet(userId);
        WalletDomain wallet = walletService.findWalletByUserId(userId);
        wallet.credit(new BigDecimal("1.00"));
        walletRepository.save(mapper.domainToEntity(wallet));

        UUID transactionId = UuidCreator.getTimeOrderedEpoch();
        BigDecimal amountOfCashback = new BigDecimal("10.00");
        BigDecimal amountOfTransaction = new BigDecimal("50.22");
        BigDecimal totalItemPrice = new BigDecimal("122.22");
        Boolean useCashback = true;

        //when
        walletService.creditPoints(userId, transactionId, amountOfCashback, useCashback, amountOfTransaction, totalItemPrice);

        //then
        List<OutboxEvent> events = outboxEventRepository.findAll();
        Assertions.assertFalse(events.isEmpty());
        OutboxEvent lastEvent = events.get(events.size() - 1);
        Assertions.assertEquals("points_failed", lastEvent.getEventType());
        Assertions.assertTrue(lastEvent.getPayload().contains("Insufficient funds"));
    }

    @Test
    @DisplayName("Неуспешная оплата NPE")
    void unsuccessfulCreditPointsNPE() throws Exception {
        //given
        UUID userId = UuidCreator.getTimeOrderedEpoch();
        walletService.createWallet(userId);

        BigDecimal amountOfCashback = new BigDecimal("10.00");
        BigDecimal amountOfTransaction = new BigDecimal("122.22");
        BigDecimal totalItemPrice = new BigDecimal("122.22");
        Boolean useCashback = false;

        //when
        walletService.creditPoints(userId, null, amountOfCashback, useCashback, amountOfTransaction, totalItemPrice);

        //then
        List<OutboxEvent> events = outboxEventRepository.findAll();
        Assertions.assertFalse(events.isEmpty());
        OutboxEvent lastEvent = events.get(events.size() - 1);
        Assertions.assertEquals("points_failed", lastEvent.getEventType());
        Assertions.assertTrue(lastEvent.getPayload().contains("Unknown error"));
    }

    @Test
    @DisplayName("Неуспешная блокировка кошелька")
    void unsuccessfulBlockWalletException() {
        //when / then
        Assertions.assertThrows(RuntimeException.class, () -> walletService.blockWallet(null));
    }

    @Test
    @DisplayName("Неуспешная блокировка кошелька (уже заблокирован)")
    void unsuccessfulBlockWalletExceptionAlreadyBlocked() {
        //given
        UUID userId = UuidCreator.getTimeOrderedEpoch();
        walletService.createWallet(userId);
        walletService.blockWallet(userId);

        //when / then
        Assertions.assertThrows(IllegalArgumentException.class, () -> walletService.blockWallet(userId));
    }

    @Test
    @DisplayName("Успешная активация кошелька")
    void successfulActivatingWallet() {
        //given
        UUID userId = UuidCreator.getTimeOrderedEpoch();
        walletService.createWallet(userId);
        walletService.blockWallet(userId);

        //when
        walletService.unblockWallet(userId);
        WalletDomain wallet = walletService.findWalletByUserId(userId);

        //then
        Assertions.assertEquals(WalletStatus.ACTIVE, wallet.getStatus());
    }

    @Test
    @DisplayName("Неуспешная активация кошелька(уже активирован)")
    void unsuccessfulActivatingWalletAlreadyActive() {
        //given
        UUID userId = UuidCreator.getTimeOrderedEpoch();
        walletService.createWallet(userId);

        //when
        Assertions.assertThrows(IllegalArgumentException.class, () -> walletService.unblockWallet(userId));
    }

    @Test
    @DisplayName("Неуспешная активация кошелька runtime exception")
    void unsuccessfulActivatingWalletRuntimeException() {
        //given
        UUID userId = UuidCreator.getTimeOrderedEpoch();
        walletService.createWallet(userId);

        //when
        Assertions.assertThrows(RuntimeException.class, () -> walletService.unblockWallet(null));
    }

    @Test
    @DisplayName("Успешное получение баланса кошелька")
    void successfulGetBalance() {
        //given
        UUID userId = UuidCreator.getTimeOrderedEpoch();
        walletService.createWallet(userId);

        //when
        BigDecimal balance = walletService.getBalance(userId);

        //then
        Assertions.assertEquals(new BigDecimal("0.00"), balance);
    }

    @Test
    @DisplayName("Успешный поиск транзакций кошелька")
    void successfulGetTransactionHistory() throws JsonProcessingException {
        //given
        UUID userId = UuidCreator.getTimeOrderedEpoch();
        walletService.createWallet(userId);
        WalletDomain wallet = walletService.findWalletByUserId(userId);
        wallet.credit(new BigDecimal("10.00"));
        walletRepository.save(mapper.domainToEntity(wallet));

        UUID transactionId = UuidCreator.getTimeOrderedEpoch();
        BigDecimal amountOfCashback = new BigDecimal("10.00");
        BigDecimal amountOfTransaction = new BigDecimal("112.22");
        BigDecimal totalItemPrice = new BigDecimal("122.22");
        Boolean useCashback = true;

        //when
        walletService.creditPoints(userId, transactionId, amountOfCashback, useCashback, amountOfTransaction, totalItemPrice);
        wallet = walletService.findWalletByUserId(userId);
        List<WalletTransactionDomain> transactionsHistory = walletService.getTransactionsHistory(userId);

        //then
        Assertions.assertEquals(1, transactionsHistory.size());
    }
}
