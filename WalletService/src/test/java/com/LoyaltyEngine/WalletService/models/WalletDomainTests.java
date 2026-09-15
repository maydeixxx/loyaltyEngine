package com.LoyaltyEngine.WalletService.models;

import com.LoyaltyEngine.WalletService.exceptions.InsufficientFundsException;
import com.LoyaltyEngine.WalletService.exceptions.WalletBlockedException;
import com.LoyaltyEngine.WalletService.models.domain.WalletDomain;
import com.LoyaltyEngine.WalletService.models.domain.enums.WalletStatus;
import com.LoyaltyEngine.WalletService.models.entity.Wallet;
import com.LoyaltyEngine.WalletService.services.WalletService;
import com.LoyaltyEngine.WalletService.services.interfaces.WalletRepository;
import com.github.f4b6a3.uuid.UuidCreator;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@SpringBootTest
@TestPropertySource(properties = {
        "eureka.client.enabled=false"
})
@Testcontainers
public class WalletDomainTests {
    @Container
    private static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18.2");
    @Autowired
    private WalletService walletService;
    @Autowired
    private WalletRepository walletRepository;

    @DynamicPropertySource
    private static void configProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    @DisplayName("Создание walletDomain")
    void successfulCreateWalletDomain() {
        //given
        UUID userId = UuidCreator.getTimeOrderedEpoch();

        //when
        WalletDomain wallet = WalletDomain.createWallet(userId);

        //then
        Assertions.assertEquals(userId, wallet.getUserId().value());
    }

    @Test
    @DisplayName("Невалидный userId")
    void notValidUserIdCreateWallet() {
        //given
        UUID userId = null;

        //when && then
        Assertions.assertThrows(NullPointerException.class, () ->WalletDomain.createWallet(userId), "userId cant be null");
    }

    @Test
    @DisplayName("Блок кошелька")
    void successfulBlockWallet() {
        //given
        WalletDomain wallet = WalletDomain.createWallet(UuidCreator.getTimeOrderedEpoch());

        //when
        wallet.blockWallet();

        //then
        Assertions.assertEquals(WalletStatus.BLOCKED, wallet.getStatus());
    }

    @Test
    @DisplayName("Неуспешный блок кошелька")
    void unsuccessfulBlockWallet() {
        //given
        WalletDomain wallet = WalletDomain.createWallet(UuidCreator.getTimeOrderedEpoch());
        wallet.blockWallet();

        //when && then
        Assertions.assertThrows(IllegalArgumentException.class, () -> wallet.blockWallet(), "Wallet already blocked");
    }

    @Test
    @DisplayName("Активация кошелька")
    void successfulActivateWallet() {
        //given
        UUID userId = UuidCreator.getTimeOrderedEpoch();
        WalletDomain wallet = WalletDomain.createWallet(userId);
        wallet.blockWallet();

        //when
        wallet.activateWallet();

        //then
        Assertions.assertEquals(WalletStatus.ACTIVE, wallet.getStatus());
    }

    @Test
    @DisplayName("Неуспешная активация кошелька")
    void unsuccessfulActivateWallet() {
        //given
        UUID userId = UuidCreator.getTimeOrderedEpoch();
        WalletDomain wallet = WalletDomain.createWallet(userId);

        //when && then
        Assertions.assertThrows(IllegalArgumentException.class, () -> wallet.activateWallet(), "Wallet already active");
    }

    @Test
    @DisplayName("Восстановление кошелька")
    void restoreFromExistingWallet() {
        //given
        UUID userId = UuidCreator.getTimeOrderedEpoch();
        walletService.createWallet(userId);
        Wallet wallet = walletRepository.findWalletByUserId(userId).get();

        UUID walletId = wallet.getId();
        BigDecimal amount = wallet.getBalance();
        WalletStatus status = wallet.getStatus();
        Long version = wallet.getVersion();
        LocalDateTime createdAt = wallet.getCreatedAt();
        LocalDateTime updatedAt = wallet.getUpdatedAt();

        //when
        WalletDomain walletDomain = WalletDomain.restoreFromExisting(walletId, userId, amount, status, version, createdAt, updatedAt);

        //then
        Assertions.assertEquals(userId, walletDomain.getUserId().value());
    }

    @Test
    @DisplayName("Обновление таймстампа")
    void updateUpdatedAt() {
        //given
        UUID userId = UuidCreator.getTimeOrderedEpoch();
        WalletDomain wallet = WalletDomain.createWallet(userId);
        LocalDateTime oldUpdatedAt = wallet.getUpdatedAt();

        //when
        wallet.updateUpdatedAt(LocalDateTime.now());
        LocalDateTime newUpdatedAt = wallet.getUpdatedAt();

        //then
        Assertions.assertNotEquals(oldUpdatedAt, newUpdatedAt);
    }

    @Test
    @DisplayName("Неуспешное обновление таймстампа")
    void unsuccessfulUpdateUpdatedAt() {
        //given
        UUID userId = UuidCreator.getTimeOrderedEpoch();
        WalletDomain wallet = WalletDomain.createWallet(userId);

        //when && then
        Assertions.assertThrows(IllegalArgumentException.class, () -> wallet.updateUpdatedAt(null), "New time cant be null");
    }

    @Test
    @DisplayName("Успешно начисление кешбека")
    void successfulCredit() {
        //given
        UUID userId = UuidCreator.getTimeOrderedEpoch();
        WalletDomain wallet = WalletDomain.createWallet(userId);
        BigDecimal oldBalance = wallet.getBalance().amount();

        //when
        wallet.credit(new BigDecimal("10.00"));

        //then
        Assertions.assertNotEquals(oldBalance, wallet.getBalance().amount());
    }

    @Test
    @DisplayName("Неуспешно начисление кешбека")
    void unsuccessfulCredit() {
        //given
        UUID userId = UuidCreator.getTimeOrderedEpoch();
        WalletDomain wallet = WalletDomain.createWallet(userId);
        wallet.blockWallet();

        //then
        Assertions.assertThrows(WalletBlockedException.class, () -> wallet.credit(new BigDecimal("10.00")), "Wallet isn`t active");
    }

    @Test
    @DisplayName("Списание кешбека")
    void successfulDebit() {
        //given
        UUID userId = UuidCreator.getTimeOrderedEpoch();
        WalletDomain wallet = WalletDomain.createWallet(userId);
        wallet.credit(new BigDecimal("10.00"));

        //when
        wallet.debit(new BigDecimal("5.00"));

        //then
        Assertions.assertEquals(new BigDecimal("5.00"), wallet.getBalance().amount());
    }

    @Test
    @DisplayName("Списание кешбека у блокнутого кошелька")
    void unsuccessfulDebitWalletBlocked() {
        //given
        UUID userId = UuidCreator.getTimeOrderedEpoch();
        WalletDomain wallet = WalletDomain.createWallet(userId);
        wallet.credit(new BigDecimal("10.00"));
        wallet.blockWallet();

        //when
        Assertions.assertThrows(WalletBlockedException.class, () -> wallet.debit(new BigDecimal("5.00")), "Wallet isn`t active");
    }

    @Test
    @DisplayName("Списание кешбека (недостаточно средств)")
    void unsuccessfulDebitInsufficientFunds() {
        //given
        UUID userId = UuidCreator.getTimeOrderedEpoch();
        WalletDomain wallet = WalletDomain.createWallet(userId);

        //when
        Assertions.assertThrows(InsufficientFundsException.class, () -> wallet.debit(new BigDecimal("5.00")), "Insufficient funds");
    }

    @Test
    @DisplayName("Сравнение кошелька")
    void walletEquals() {
        //given
        UUID userId = UuidCreator.getTimeOrderedEpoch();
        walletService.createWallet(userId);
        WalletDomain wallet = walletService.findWalletByUserId(userId);
        WalletDomain wallet1 = walletService.findWalletByUserId(userId);
        wallet1.updateUpdatedAt(LocalDateTime.now());

        //when && then
        Assertions.assertNotEquals(wallet.getUpdatedAt(), wallet1.getUpdatedAt());
        Assertions.assertEquals(wallet, wallet1);
    }
}
