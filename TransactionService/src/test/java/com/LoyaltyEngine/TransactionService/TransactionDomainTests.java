package com.LoyaltyEngine.TransactionService;

import com.LoyaltyEngine.TransactionService.models.domain.TransactionDomain;
import com.LoyaltyEngine.TransactionService.models.domain.TransactionItemDomain;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TransactionDomainTests {

    @Test
    @DisplayName("Создание TransactionDomain с валидными данными")
    void createTransactionDomain_WithValidData() {
        //given
        Long userId = Long.parseLong("1");
        BigDecimal amount = new BigDecimal("100.1");
        UUID idempotencyKey = UUID.randomUUID();
        List<TransactionItemDomain> items = List.of(
                TransactionItemDomain.createTransactionItem("el", "lap", new BigDecimal("101"))
        );

        //when
        TransactionDomain transactionDomain = TransactionDomain.create(userId, amount, idempotencyKey, items);

        //then
        Assertions.assertEquals(userId, transactionDomain.getUserId());
        Assertions.assertEquals(amount, transactionDomain.getAmount());
        Assertions.assertEquals(items, transactionDomain.getItems());
    }

    @Test
    @DisplayName("Создание TransactionDomain с null userId")
    void createTransactionDomain_WithNullUserId() {
        //given
        BigDecimal amount = new BigDecimal("100.1");
        UUID idempotencyKey = UUID.randomUUID();
        List<TransactionItemDomain> items = List.of(
                TransactionItemDomain.createTransactionItem("el", "lap", new BigDecimal("101"))
                );

        //when && then
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () -> TransactionDomain.create(null, amount, idempotencyKey, items));

        //then
        Assertions.assertEquals("UserId не может быть null", ex.getMessage());
    }

    @Test
    @DisplayName("Создание TransactionDomain с -1 userId")
    void createTransactionDomain_WithNegativeUserId() {
        //given
        BigDecimal amount = new BigDecimal("100.1");
        UUID idempotencyKey = UUID.randomUUID();
        List<TransactionItemDomain> items = List.of(
                TransactionItemDomain.createTransactionItem("el", "lap", new BigDecimal("101"))
        );

        //when && then
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () -> TransactionDomain.create((long) -1, amount, idempotencyKey, items));

        //then
        Assertions.assertEquals("UserId не может быть меньше 1", ex.getMessage());
    }

    @Test
    @DisplayName("Создание TransactionDomain с null amount")
    void createTransactionDomain_WithNullAmount() {
        //given
        UUID idempotencyKey = UUID.randomUUID();
        List<TransactionItemDomain> items = List.of(
                TransactionItemDomain.createTransactionItem("el", "lap", new BigDecimal("101"))
        );

        //when && then
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () -> TransactionDomain.create(1L, null, idempotencyKey, items));

        //then
        Assertions.assertEquals("Amount не может быть меньше 0 или равной 0", ex.getMessage());
    }

    @Test
    @DisplayName("Создание TransactionDomain с null idempotencyKey")
    void createTransactionDomain_WithNullIdempotencyKey() {
        //given
        BigDecimal amount = new BigDecimal("100.1");
        UUID idempotencyKey = UUID.randomUUID();
        List<TransactionItemDomain> items = List.of(
                TransactionItemDomain.createTransactionItem("el", "lap", new BigDecimal("101"))
        );

        //when && then
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () -> TransactionDomain.create(1L, amount, null, items));

        //then
        Assertions.assertEquals("Idempotency key не может быть null", ex.getMessage());
    }

    @Test
    @DisplayName("Создание TransactionDomain с empty items")
    void createTransactionDomain_WithEmptyItems() {
        //given
        BigDecimal amount = new BigDecimal("100.1");
        UUID idempotencyKey = UUID.randomUUID();
        List<TransactionItemDomain> items = List.of(
                TransactionItemDomain.createTransactionItem("el", "lap", new BigDecimal("101"))
        );

        //when && then
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () -> TransactionDomain.create(1L, amount, idempotencyKey, new ArrayList<>()));

        //then
        Assertions.assertEquals("Items не может быть пустым", ex.getMessage());
    }

}
