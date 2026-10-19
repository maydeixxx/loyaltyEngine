package com.LoyaltyEngine.RuleEngineService.services;

import com.LoyaltyEngine.RuleEngineService.models.eventModels.TransactionCreatedEvent;
import com.LoyaltyEngine.RuleEngineService.models.eventModels.TransactionItemEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CashbackStrategyTest {

    @Mock
    private RuleEngineService ruleEngineService;

    @InjectMocks
    private CategoryCashbackStrategy categoryStrategy;

    private final DefaultCashbackStrategy defaultStrategy = new DefaultCashbackStrategy();

    private final TransactionCreatedEvent transaction = new TransactionCreatedEvent(
            UUID.randomUUID(),
            UUID.randomUUID(),
            new BigDecimal("100.00"),
            List.of(),
            LocalDateTime.now(),
            false
    );

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("CategoryCashbackStrategy: returns empty Optional when item category is null or blank")
    void categoryStrategy_nullOrBlankCategory_returnsEmpty(String category) {
        //given
        TransactionItemEvent item = new TransactionItemEvent(category, "Item", new BigDecimal("50.00"));

        //when
        Optional<BigDecimal> result = categoryStrategy.calculate(item, transaction);

        //then
        assertTrue(result.isEmpty());
        verifyNoInteractions(ruleEngineService);
    }

    @Test
    @DisplayName("CategoryCashbackStrategy: delegates to RuleEngineService when category is present")
    void categoryStrategy_validCategory_delegatesToService() {
        //given
        TransactionItemEvent item = new TransactionItemEvent("electronics", "Phone", new BigDecimal("1000.00"));
        when(ruleEngineService.getPercentageForCategory("electronics"))
                .thenReturn(Optional.of(new BigDecimal("10.00")));

        //when
        Optional<BigDecimal> result = categoryStrategy.calculate(item, transaction);

        //then
        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("10.00"), result.get());
    }

    @Test
    @DisplayName("DefaultCashbackStrategy: always returns default percentage of 1.00%")
    void defaultStrategy_anyItem_returnsDefaultPercentage() {
        //given
        TransactionItemEvent item = new TransactionItemEvent("any", "Any Item", new BigDecimal("100.00"));

        //when
        Optional<BigDecimal> result = defaultStrategy.calculate(item, transaction);

        //then
        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("1.00"), result.get());
    }
}
