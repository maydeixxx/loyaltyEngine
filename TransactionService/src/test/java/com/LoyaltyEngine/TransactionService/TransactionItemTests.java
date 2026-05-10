package com.LoyaltyEngine.TransactionService;

import com.LoyaltyEngine.TransactionService.models.domain.TransactionItemDomain;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

public class TransactionItemTests {

    @Test
    @DisplayName("Успешное создание TransactionItemDomain с валидными данным")
    void createTransactionItem_WithValidData() {
        //given
        String category = "ELECTRONICS";
        String name = "laptop";
        BigDecimal price = new BigDecimal("102.2");

        //when
        TransactionItemDomain transactionItem = TransactionItemDomain.createTransactionItem(category, name, price);

        //then
        Assertions.assertEquals(category, transactionItem.getCategory());
        Assertions.assertEquals(name, transactionItem.getName());
        Assertions.assertEquals(price, transactionItem.getPrice());
    }

    @Test
    @DisplayName("Создание TransactionItemDomain с невалидным category")
    void createTransactionItem_WithNotValidCategory() {
        //given
        String category = "";
        String name = "laptop";
        BigDecimal price = new BigDecimal("1.02");

        //when && then
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () -> TransactionItemDomain.createTransactionItem(category, name, price));

        Assertions.assertEquals("Category не может быть пустым", ex.getMessage());
    }

    @Test
    @DisplayName("Создание TransactionItemDomain с невалидным name")
    void createTransactionItem_WithNotValidName() {
        //given
        String category = "ELECTRONICS";
        String name = "";
        BigDecimal price = new BigDecimal("1.02");

        //when && then
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () -> TransactionItemDomain.createTransactionItem(category, name, price));

        Assertions.assertEquals("Name не может быть пустым", ex.getMessage());
    }

    @Test
    @DisplayName("Создание TransactionItemDomain с невалидным price")
    void createTransactionItem_WithNotValidPrice() {
        //given
        String category = "ELECTRONICS";
        String name = "laptop";
        BigDecimal price = new BigDecimal("-1.02");

        //when && then
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () -> TransactionItemDomain.createTransactionItem(category, name, price));

        Assertions.assertEquals("Price не может быть <= 0 или null", ex.getMessage());
    }

}
