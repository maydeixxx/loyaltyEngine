package com.LoyaltyEngine.RuleEngineService.services;

import com.LoyaltyEngine.RuleEngineService.models.eventModels.CalculatedCashbackEventModel;
import com.LoyaltyEngine.RuleEngineService.models.eventModels.TransactionCreatedEvent;
import com.LoyaltyEngine.RuleEngineService.models.eventModels.TransactionItemEvent;
import com.LoyaltyEngine.RuleEngineService.services.interfaces.CashbackStrategy;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalculateCashbackServiceTest {

    @Mock
    private CashbackStrategy categoryStrategy;
    @Mock
    private CashbackStrategy defaultStrategy;

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();

    private final Tracer tracer = Tracer.NOOP;

    private CalculateCashbackService calculateCashbackService;

    private final UUID transactionId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        calculateCashbackService = new CalculateCashbackService(List.of(categoryStrategy, defaultStrategy), registry, tracer);
    }

    @Test
    @DisplayName("Успешный расчет: первая стратегия обрабатывает категорию, вторая — дефолт")
    void calculateCashback_mixedStrategies_calculatesCashbackCorrectly() {
        //given
        TransactionItemEvent item1 = new TransactionItemEvent("electronics", "Phone", new BigDecimal("1000.00"));
        TransactionItemEvent item2 = new TransactionItemEvent(null, "Book", new BigDecimal("500.00"));

        TransactionCreatedEvent transaction = new TransactionCreatedEvent(
                transactionId,
                userId,
                new BigDecimal("1500.00"),
                List.of(item1, item2),
                LocalDateTime.now(),
                false
        );

        when(categoryStrategy.calculate(item1, transaction)).thenReturn(Optional.of(new BigDecimal("10.00")));
        when(categoryStrategy.calculate(item2, transaction)).thenReturn(Optional.empty());
        when(defaultStrategy.calculate(item2, transaction)).thenReturn(Optional.of(new BigDecimal("1.00")));

        //when
        CalculatedCashbackEventModel result = calculateCashbackService.calculateCashback(transaction, transactionId);

        //then
        assertAll(
                () -> assertEquals(transactionId, result.transactionId()),
                () -> assertEquals(userId, result.userId()),
                () -> assertEquals(new BigDecimal("1500.00"), result.amountOfTransaction()),
                () -> assertEquals(new BigDecimal("1500.00"), result.totalItemPrice()),
                () -> assertEquals(new BigDecimal("105.00"), result.amount())
        );
    }

    @Test
    @DisplayName("Если ни одна стратегия не подошла — выбрасывается IllegalStateException")
    void calculateCashback_noStrategyApplicable_throwsIllegalStateException() {
        //given
        TransactionItemEvent item = new TransactionItemEvent("unknown", "Widget", new BigDecimal("100.00"));
        TransactionCreatedEvent transaction = new TransactionCreatedEvent(
                transactionId,
                userId,
                new BigDecimal("100.00"),
                List.of(item),
                LocalDateTime.now(),
                false
        );

        when(categoryStrategy.calculate(item, transaction)).thenReturn(Optional.empty());
        when(defaultStrategy.calculate(item, transaction)).thenReturn(Optional.empty());

        //when & then
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> calculateCashbackService.calculateCashback(transaction, transactionId)
        );
        assertEquals("No applicable strategy for item [Widget]", exception.getMessage());
    }

    @Test
    @DisplayName("Пустой список позиций возвращает нулевой кэшбек")
    void calculateCashback_emptyItems_returnsZeroCashback() {
        //given
        TransactionCreatedEvent transaction = new TransactionCreatedEvent(
                transactionId,
                userId,
                BigDecimal.ZERO,
                List.of(),
                LocalDateTime.now(),
                false
        );

        //when
        CalculatedCashbackEventModel result = calculateCashbackService.calculateCashback(transaction, transactionId);

        //then
        assertEquals(BigDecimal.ZERO, result.amount());
        assertEquals(BigDecimal.ZERO, result.totalItemPrice());
    }

    @Test
    @DisplayName("null список позиций безопасно возвращает нулевой кэшбек")
    void calculateCashback_nullItems_returnsZeroCashback() {
        //given
        TransactionCreatedEvent transaction = new TransactionCreatedEvent(
                transactionId,
                userId,
                BigDecimal.ZERO,
                null,
                LocalDateTime.now(),
                false
        );

        //when
        CalculatedCashbackEventModel result = calculateCashbackService.calculateCashback(transaction, transactionId);

        //then
        assertEquals(BigDecimal.ZERO, result.amount());
        assertEquals(BigDecimal.ZERO, result.totalItemPrice());
    }
}
