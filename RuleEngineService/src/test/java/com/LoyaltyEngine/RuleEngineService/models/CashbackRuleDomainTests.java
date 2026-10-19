package com.LoyaltyEngine.RuleEngineService.models;

import com.LoyaltyEngine.RuleEngineService.exceptions.CashbackRuleValidationException;
import com.LoyaltyEngine.RuleEngineService.exceptions.CashbackUpdateException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CashbackRuleDomainTests {

    private final LocalDateTime now = LocalDateTime.now();
    private final LocalDateTime validFrom = now.minusDays(1);
    private final LocalDateTime validTo = now.plusDays(30);

    @Test
    @DisplayName("createCashbackRule: creates domain entity with valid data and normalized category")
    void createCashbackRule_validData_createsSuccessfully() {
        //given
        String category = "  EleCtroNics  ";
        BigDecimal percentage = new BigDecimal("10.50");

        //when
        CashbackRuleDomain rule = CashbackRuleDomain.createCashbackRule(category, percentage, validFrom, validTo);

        //then
        assertAll(
                () -> assertNotNull(rule.getId()),
                () -> assertNotNull(rule.getId().value()),
                () -> assertEquals("electronics", rule.getCategory()),
                () -> assertEquals(percentage, rule.getPercentage()),
                () -> assertEquals(validFrom, rule.getValidFrom()),
                () -> assertEquals(validTo, rule.getValidTo())
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("createCashbackRule: throws CashbackRuleValidationException when category is null or blank")
    void createCashbackRule_nullOrBlankCategory_throwsValidationException(String category) {
        CashbackRuleValidationException ex = assertThrows(
                CashbackRuleValidationException.class,
                () -> CashbackRuleDomain.createCashbackRule(category, new BigDecimal("10.00"), validFrom, validTo)
        );
        assertEquals("Category cannot be null or blank", ex.getMessage());
    }

    @Test
    @DisplayName("createCashbackRule: throws CashbackRuleValidationException when percentage is less than 1")
    void createCashbackRule_percentageLessThanOne_throwsValidationException() {
        CashbackRuleValidationException ex = assertThrows(
                CashbackRuleValidationException.class,
                () -> CashbackRuleDomain.createCashbackRule("electronics", new BigDecimal("0.99"), validFrom, validTo)
        );
        assertEquals("Percentage cannot be less than 1", ex.getMessage());
    }

    @Test
    @DisplayName("createCashbackRule: throws CashbackRuleValidationException when validFrom is null")
    void createCashbackRule_nullValidFrom_throwsValidationException() {
        CashbackRuleValidationException ex = assertThrows(
                CashbackRuleValidationException.class,
                () -> CashbackRuleDomain.createCashbackRule("electronics", new BigDecimal("5.00"), null, validTo)
        );
        assertEquals("Valid from cannot be null", ex.getMessage());
    }

    @Test
    @DisplayName("createCashbackRule: throws CashbackRuleValidationException when validTo is null")
    void createCashbackRule_nullValidTo_throwsValidationException() {
        CashbackRuleValidationException ex = assertThrows(
                CashbackRuleValidationException.class,
                () -> CashbackRuleDomain.createCashbackRule("electronics", new BigDecimal("5.00"), validFrom, null)
        );
        assertEquals("Valid to cannot be null", ex.getMessage());
    }

    @Test
    @DisplayName("createCashbackRule: throws CashbackRuleValidationException when validFrom is after validTo")
    void createCashbackRule_validFromAfterValidTo_throwsValidationException() {
        LocalDateTime futureFrom = now.plusDays(10);
        LocalDateTime futureTo = now.plusDays(5);

        CashbackRuleValidationException ex = assertThrows(
                CashbackRuleValidationException.class,
                () -> CashbackRuleDomain.createCashbackRule("electronics", new BigDecimal("5.00"), futureFrom, futureTo)
        );
        assertEquals("Valid from cannot be after validTo", ex.getMessage());
    }

    @Test
    @DisplayName("createCashbackRule: throws CashbackRuleValidationException when validTo is in the past")
    void createCashbackRule_validToInPast_throwsValidationException() {
        LocalDateTime pastFrom = now.minusDays(10);
        LocalDateTime pastTo = now.minusDays(1);

        CashbackRuleValidationException ex = assertThrows(
                CashbackRuleValidationException.class,
                () -> CashbackRuleDomain.createCashbackRule("electronics", new BigDecimal("5.00"), pastFrom, pastTo)
        );
        assertEquals("Valid to cant be before present time", ex.getMessage());
    }

    @Test
    @DisplayName("restoreFromExisting: successfully restores expired rule without throwing validation exception")
    void restoreFromExisting_pastValidTo_doesNotThrowValidationException() {
        //given
        UUID ruleId = UUID.randomUUID();
        LocalDateTime pastFrom = now.minusDays(30);
        LocalDateTime pastTo = now.minusDays(5);

        //when
        CashbackRuleDomain rule = CashbackRuleDomain.restoreFromExisting(
                ruleId, "books", new BigDecimal("5.00"), pastFrom, pastTo
        );

        //then
        assertAll(
                () -> assertEquals(ruleId, rule.getId().value()),
                () -> assertEquals("books", rule.getCategory()),
                () -> assertEquals(pastTo, rule.getValidTo())
        );
    }

    @Test
    @DisplayName("updatePercentage: updates percentage when valid")
    void updatePercentage_validPercentage_updatesSuccessfully() {
        //given
        CashbackRuleDomain rule = CashbackRuleDomain.createCashbackRule("electronics", new BigDecimal("5.00"), validFrom, validTo);

        //when
        rule.updatePercentage(new BigDecimal("12.00"));

        //then
        assertEquals(new BigDecimal("12.00"), rule.getPercentage());
    }

    @Test
    @DisplayName("updatePercentage: throws CashbackUpdateException when percentage is invalid")
    void updatePercentage_invalidPercentage_throwsUpdateException() {
        //given
        CashbackRuleDomain rule = CashbackRuleDomain.createCashbackRule("electronics", new BigDecimal("5.00"), validFrom, validTo);

        //then
        assertThrows(CashbackUpdateException.class, () -> rule.updatePercentage(null));
        assertThrows(CashbackUpdateException.class, () -> rule.updatePercentage(new BigDecimal("5.00")));
        assertThrows(CashbackUpdateException.class, () -> rule.updatePercentage(new BigDecimal("0.50")));
    }

    @Test
    @DisplayName("updateValidTo: updates validTo when date is in the future")
    void updateValidTo_validDate_updatesSuccessfully() {
        //given
        CashbackRuleDomain rule = CashbackRuleDomain.createCashbackRule("electronics", new BigDecimal("5.00"), validFrom, validTo);
        LocalDateTime newValidTo = now.plusDays(60);

        //when
        rule.updateValidTo(newValidTo);

        //then
        assertEquals(newValidTo, rule.getValidTo());
    }

    @Test
    @DisplayName("updateValidTo: throws CashbackUpdateException when date is null or in the past")
    void updateValidTo_invalidDate_throwsUpdateException() {
        //given
        CashbackRuleDomain rule = CashbackRuleDomain.createCashbackRule("electronics", new BigDecimal("5.00"), validFrom, validTo);

        //then
        assertThrows(CashbackUpdateException.class, () -> rule.updateValidTo(null));
        assertThrows(CashbackUpdateException.class, () -> rule.updateValidTo(now.minusDays(1)));
    }

    @Test
    @DisplayName("equals and hashCode: entities with same ID are equal")
    void equalsAndHashCode_sameId_areEqual() {
        //given
        UUID id = UUID.randomUUID();
        CashbackRuleDomain rule1 = CashbackRuleDomain.restoreFromExisting(id, "electronics", new BigDecimal("5.00"), validFrom, validTo);
        CashbackRuleDomain rule2 = CashbackRuleDomain.restoreFromExisting(id, "books", new BigDecimal("10.00"), validFrom, validTo);
        CashbackRuleDomain rule3 = CashbackRuleDomain.restoreFromExisting(UUID.randomUUID(), "electronics", new BigDecimal("5.00"), validFrom, validTo);

        //then
        assertEquals(rule1, rule2);
        assertEquals(rule1.hashCode(), rule2.hashCode());
        assertNotEquals(rule1, rule3);
    }
}
