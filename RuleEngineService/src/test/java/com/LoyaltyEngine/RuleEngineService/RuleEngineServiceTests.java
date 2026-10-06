package com.LoyaltyEngine.RuleEngineService;

import com.LoyaltyEngine.RuleEngineService.models.CashbackRuleDomain;
import com.LoyaltyEngine.RuleEngineService.models.dto.UpdateCashbackModelDTO;
import com.LoyaltyEngine.RuleEngineService.models.eventModels.CalculatedCashbackEventModel;
import com.LoyaltyEngine.RuleEngineService.models.eventModels.TransactionCreatedEvent;
import com.LoyaltyEngine.RuleEngineService.models.eventModels.TransactionItemEvent;
import com.LoyaltyEngine.RuleEngineService.services.CalculateCashbackService;
import com.LoyaltyEngine.RuleEngineService.services.RuleEngineService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@SpringBootTest
@Testcontainers
@Transactional
@TestPropertySource(properties = {
        "eureka.client.enabled=false"
})
public class RuleEngineServiceTests {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18.3");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private RuleEngineService ruleEngineService;

    @Autowired
    private CalculateCashbackService calculateCashbackService;

    @Test
    @DisplayName("Успешное создание нового правила")
    void successfullyCreatedRule() {
        //given
        String category = "electronics";
        BigDecimal percentage = new BigDecimal("12.5");
        LocalDateTime validFrom = LocalDateTime.now();
        LocalDateTime validTo = LocalDateTime.now().plusDays(1);

        //when
        ruleEngineService.createCashbackRule(category, percentage, validFrom, validTo);

        //then
        List<CashbackRuleDomain> allRules = ruleEngineService.getAllRules();
        Assertions.assertEquals(1, allRules.size());
        Assertions.assertEquals("electronics", allRules.getFirst().getCategory());
    }

    @Test
    @DisplayName("Успешное удаление правила")
    void successfulDeletingRule() {
        //given
        String category = "electronics";
        BigDecimal percentage = new BigDecimal("12.5");
        LocalDateTime validFrom = LocalDateTime.now();
        LocalDateTime validTo = LocalDateTime.now().plusDays(1);
        ruleEngineService.createCashbackRule(category, percentage, validFrom, validTo);

        //when
        UUID id = ruleEngineService.getAllRules().getFirst().getId().value();
        ruleEngineService.deleteCashbackRule(id);
        List<CashbackRuleDomain> allRules = ruleEngineService.getAllRules();

        //then
        Assertions.assertEquals(0, allRules.size());
    }

    @Test
    @DisplayName("Успешное обновление правила")
    void successfulUpdateRule() {
        //given
        String category = "electronics";
        BigDecimal percentage = new BigDecimal("12.5");
        LocalDateTime validFrom = LocalDateTime.now();
        LocalDateTime validTo = LocalDateTime.now().plusDays(1);
        ruleEngineService.createCashbackRule(category, percentage, validFrom, validTo);

        UpdateCashbackModelDTO newRule = new UpdateCashbackModelDTO(new BigDecimal("15.0"), null);
        UUID id = ruleEngineService.getAllRules().getFirst().getId().value();

        //when
        ruleEngineService.updateCashbackRule(newRule, id);
        CashbackRuleDomain updatedRule = ruleEngineService.getAllRules().getFirst();

        //then
        Assertions.assertEquals("electronics", updatedRule.getCategory());
        Assertions.assertEquals(new BigDecimal("15.0"), updatedRule.getPercentage());
    }

    @Test
    @DisplayName("Получение процента для категории")
    void getPercentageByCategory() {
        //given
        String category = "electronics";
        BigDecimal percentage = new BigDecimal("12.5");
        LocalDateTime validFrom = LocalDateTime.now();
        LocalDateTime validTo = LocalDateTime.now().plusDays(1);
        ruleEngineService.createCashbackRule(category, percentage, validFrom, validTo);

        //when
        Optional<BigDecimal> percentage1 = ruleEngineService.getPercentageForCategory("electronics");

        //then
        Assertions.assertTrue(percentage1.isPresent());
        Assertions.assertEquals(new BigDecimal("12.50"), percentage1.get());
    }

    @Test
    @DisplayName("Категория без правила возвращает пустой Optional")
    void getPercentageByUnknownCategoryReturnsEmpty() {
        //given
        String category = "electronics";
        BigDecimal percentage = new BigDecimal("12.5");
        LocalDateTime validFrom = LocalDateTime.now();
        LocalDateTime validTo = LocalDateTime.now().plusDays(1);
        ruleEngineService.createCashbackRule(category, percentage, validFrom, validTo);

        //when
        Optional<BigDecimal> percentage1 = ruleEngineService.getPercentageForCategory("cars");

        //then
        Assertions.assertTrue(percentage1.isEmpty());
    }

    @Test
    @DisplayName("Расчет кэшбека через CalculateCashbackService: категория + дефолт")
    void calculateCashback_mixedItems_calculatesCorrectCashback() {
        //given
        String category = "electronics";
        BigDecimal percentage = new BigDecimal("10.0");
        LocalDateTime validFrom = LocalDateTime.now();
        LocalDateTime validTo = LocalDateTime.now().plusDays(1);
        ruleEngineService.createCashbackRule(category, percentage, validFrom, validTo);

        UUID transactionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        // 1000 * 10% = 100.00
        TransactionItemEvent itemWithCategory = new TransactionItemEvent("electronics", "Phone", new BigDecimal("1000.00"));
        // 500 * 1% (дефолт) = 5.00
        TransactionItemEvent itemWithoutCategory = new TransactionItemEvent(null, "Book", new BigDecimal("500.00"));

        TransactionCreatedEvent transaction = new TransactionCreatedEvent(
                transactionId,
                userId,
                new BigDecimal("1500.00"),
                List.of(itemWithCategory, itemWithoutCategory),
                LocalDateTime.now(),
                false
        );

        //when
        CalculatedCashbackEventModel result = calculateCashbackService.calculateCashback(transaction, transactionId);

        //then
        Assertions.assertEquals(new BigDecimal("105.00"), result.amount());
        Assertions.assertEquals(new BigDecimal("1500.00"), result.totalItemPrice());
    }
}
