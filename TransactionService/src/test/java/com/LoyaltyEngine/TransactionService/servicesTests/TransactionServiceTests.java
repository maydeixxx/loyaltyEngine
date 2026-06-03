package com.LoyaltyEngine.TransactionService.servicesTests;

import com.LoyaltyEngine.TransactionService.exceptions.TransactionNotFoundException;
import com.LoyaltyEngine.TransactionService.models.domain.TransactionDomain;
import com.LoyaltyEngine.TransactionService.models.domain.TransactionItemDomain;
import com.LoyaltyEngine.TransactionService.services.TransactionService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@SpringBootTest
@Testcontainers
@Transactional
public class TransactionServiceTests {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18.3");

    @DynamicPropertySource
    static void configProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TransactionService transactionService;

    private final List<TransactionItemDomain> items = List.of(
            TransactionItemDomain.createTransactionItem("ELECTRONICS", "LAPTOP", new BigDecimal("102.2"))
    );

    @Test
    @DisplayName("Успешное создание транзакции")
    void createTransactionSuccess() {
        //given
        BigDecimal amount = new BigDecimal("102.2");
        UUID idempotencyKey = UUID.randomUUID();

        //when
        TransactionDomain transaction = transactionService.createTransaction(1L, amount, items, idempotencyKey);

        //then
        Assertions.assertEquals(1, transaction.getUserId());
        Assertions.assertEquals(amount, transaction.getAmount());
        Assertions.assertEquals(idempotencyKey, transaction.getIdempotencyKey());
    }

    @Test
    @DisplayName("Создание транзакции с уже существующим IK")
    void createTransactionIKPresent() {
        //given
        UUID idempotencyKey = UUID.randomUUID();
        transactionService.createTransaction(1L, new BigDecimal("102.2"), items, idempotencyKey);

        //when
        TransactionDomain secondTransaction = transactionService.createTransaction(2L, new BigDecimal("103.2"), items, idempotencyKey);

        //then
        Assertions.assertEquals(1L, secondTransaction.getUserId());
        Assertions.assertEquals(new BigDecimal("102.2"), secondTransaction.getAmount());
    }

    @Test
    @DisplayName("Успешный поиск транзакции по id")
    void findTransactionByIdSuccess() {
        //given
        TransactionDomain transaction = transactionService.createTransaction(1L, new BigDecimal("105.1"), items, UUID.randomUUID());
        UUID id = transaction.getId();

        //when
        TransactionDomain transactionById = transactionService.getTransactionById(id);

        //then
        Assertions.assertEquals(transaction.getAmount(), transactionById.getAmount());
        Assertions.assertEquals(transaction.getId(), transactionById.getId());
        Assertions.assertEquals(transaction.getItems(), transactionById.getItems());
        Assertions.assertEquals(transaction.getIdempotencyKey(), transactionById.getIdempotencyKey());
    }

    @Test
    @DisplayName("Неуспешный поиск транзакции по id")
    void findTransactionByIdNotSuccessful() {
        //given
        UUID uuid = UUID.randomUUID();

        //when && then
        Assertions.assertThrows(TransactionNotFoundException.class, () -> transactionService.getTransactionById(uuid));
    }

    @Test
    @DisplayName("Поиск транзакции по userId")
    void findTransactionByUserId() {
        //given
        TransactionDomain transaction = transactionService.createTransaction(1L, new BigDecimal("105.1"), items, UUID.randomUUID());

        //when
        TransactionDomain transactionByUserId = transactionService.getTransactionByUserId(1L).getFirst();

        //then
        Assertions.assertEquals(transaction.getAmount(), transactionByUserId.getAmount());
        Assertions.assertEquals(transaction.getId(), transactionByUserId.getId());
        Assertions.assertEquals(transaction.getItems(), transactionByUserId.getItems());
        Assertions.assertEquals(transaction.getIdempotencyKey(), transactionByUserId.getIdempotencyKey());
    }

}
