package com.LoyaltyEngine.TransactionService.modelsTests;

import com.LoyaltyEngine.TransactionService.models.domain.TransactionDomain;
import com.LoyaltyEngine.TransactionService.models.domain.TransactionItemDomain;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

public class TransactionDomainTests {

    private static final Currency currency = Currency.getInstance("USD");

    @Test
    @DisplayName("Создание TransactionDomain с валидными данными")
    void createTransactionDomain_WithValidData() {
        //given
        UUID userId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("101.00");
        UUID idempotencyKey = UUID.randomUUID();
        List<TransactionItemDomain> items = List.of(
                TransactionItemDomain.createTransactionItem("el", "lap", new BigDecimal("101.00"), currency)
        );

        //when
        TransactionDomain transactionDomain = TransactionDomain.create(userId, idempotencyKey, amount, currency, items, false);

        //then
        Assertions.assertEquals(userId, transactionDomain.getUserId().value());
        Assertions.assertEquals(new BigDecimal("101.00"), transactionDomain.getAmount().amount());
        Assertions.assertEquals(items, transactionDomain.getItems());
    }

    @Test
    @DisplayName("Создание TransactionDomain с null userId")
    void createTransactionDomain_WithNullUserId() {
        //given
        BigDecimal amount = new BigDecimal("101.00");
        UUID idempotencyKey = UUID.randomUUID();
        List<TransactionItemDomain> items = List.of(
                TransactionItemDomain.createTransactionItem("el", "lap", new BigDecimal("101.00"), currency)
        );

        //when && then
        NullPointerException ex = Assertions.assertThrows(NullPointerException.class, () ->
                TransactionDomain.create(null, idempotencyKey, amount, currency, items, false));

        //then
        Assertions.assertEquals("User id cant be null", ex.getMessage());
    }

    @Test
    @DisplayName("Создание TransactionDomain с null amount")
    void createTransactionDomain_WithNullAmount() {
        //given
        UUID userId = UUID.randomUUID();
        UUID idempotencyKey = UUID.randomUUID();
        List<TransactionItemDomain> items = List.of(
                TransactionItemDomain.createTransactionItem("el", "lap", new BigDecimal("101.00"), currency)
        );

        //when && then
        NullPointerException ex = Assertions.assertThrows(NullPointerException.class, () ->
                TransactionDomain.create(userId, idempotencyKey, null, currency, items, false));

        //then
        Assertions.assertEquals("Amount cant be null", ex.getMessage());
    }

    @Test
    @DisplayName("Создание TransactionDomain с null idempotencyKey")
    void createTransactionDomain_WithNullIdempotencyKey() {
        //given
        UUID userId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("101.00");
        List<TransactionItemDomain> items = List.of(
                TransactionItemDomain.createTransactionItem("el", "lap", new BigDecimal("101.00"), currency)
        );

        //when && then
        NullPointerException ex = Assertions.assertThrows(NullPointerException.class, () ->
                TransactionDomain.create(userId, null, amount, currency, items, false));

        //then
        Assertions.assertEquals("Idempotency key cant be null", ex.getMessage());
    }

    @Test
    @DisplayName("Создание TransactionDomain с empty items")
    void createTransactionDomain_WithEmptyItems() {
        //given
        UUID userId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("100.1");
        UUID idempotencyKey = UUID.randomUUID();

        //when && then
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () ->
                TransactionDomain.create(userId, idempotencyKey, amount, currency, new ArrayList<>(), false));

        //then
        Assertions.assertEquals("Items size must be >= 1", ex.getMessage());
    }

}
