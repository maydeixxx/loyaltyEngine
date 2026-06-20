package com.LoyaltyEngine.RuleEngineService;

import com.LoyaltyEngine.RuleEngineService.models.CashbackRuleDomain;
import com.LoyaltyEngine.RuleEngineService.models.dto.UpdateCashbackModelDTO;
import com.LoyaltyEngine.RuleEngineService.services.RuleEngineService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@SpringBootTest
@Testcontainers
@Transactional
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
        UUID id = ruleEngineService.getAllRules().getFirst().getId();
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

        UpdateCashbackModelDTO newRule = new UpdateCashbackModelDTO("new", null, null, null);
        UUID id = ruleEngineService.getAllRules().getFirst().getId();

        //when
        ruleEngineService.updateCashbackRule(newRule, id);
        CashbackRuleDomain updatedRule = ruleEngineService.getAllRules().getFirst();

        //then
        Assertions.assertEquals("new", updatedRule.getCategory());
        Assertions.assertEquals(new BigDecimal("12.5"), updatedRule.getPercentage());
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
        BigDecimal percentage1 = ruleEngineService.getPercentageForCategory("electronics");

        //then
        Assertions.assertEquals(new BigDecimal("12.5"), percentage1);
    }
}
