package com.LoyaltyEngine.RuleEngineService.services;

import com.LoyaltyEngine.RuleEngineService.exceptions.CashbackRuleNotFoundException;
import com.LoyaltyEngine.RuleEngineService.models.CashbackRule;
import com.LoyaltyEngine.RuleEngineService.models.CashbackRuleDomain;
import com.LoyaltyEngine.RuleEngineService.models.dto.CashbackRuleDTO;
import com.LoyaltyEngine.RuleEngineService.models.dto.UpdateCashbackModelDTO;
import com.LoyaltyEngine.RuleEngineService.services.interfaces.RuleEngineRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.tracing.Tracer;
import jakarta.persistence.EntityExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuleEngineServiceTest {

    @Mock
    private RuleEngineRepository ruleEngineRepository;
    @Mock
    private RuleEngineMapper ruleEngineMapper;

    @Captor
    private ArgumentCaptor<CashbackRule> ruleCaptor;

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final Tracer tracer = Tracer.NOOP;

    private RuleEngineService ruleEngineService;

    private final UUID ruleId = UUID.randomUUID();
    private final LocalDateTime now = LocalDateTime.now();
    private final LocalDateTime validFrom = now.minusDays(1);
    private final LocalDateTime validTo = now.plusDays(30);

    @BeforeEach
    void setUp() {
        ruleEngineService = new RuleEngineService(ruleEngineRepository, ruleEngineMapper, registry, tracer);
    }

    @Test
    @DisplayName("getPercentageForCategory: returns active percentage when category rule exists")
    void getPercentageForCategory_categoryExists_returnsPercentage() {
        //given
        when(ruleEngineRepository.findActivePercentageByCategory(eq("electronics"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(new BigDecimal("10.00")));

        //when
        Optional<BigDecimal> result = ruleEngineService.getPercentageForCategory("  EleCtroNics ");

        //then
        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("10.00"), result.get());
    }

    @Test
    @DisplayName("getPercentageForCategory: returns empty Optional when rule does not exist")
    void getPercentageForCategory_categoryDoesNotExist_returnsEmpty() {
        //given
        when(ruleEngineRepository.findActivePercentageByCategory(eq("books"), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        //when
        Optional<BigDecimal> result = ruleEngineService.getPercentageForCategory("books");

        //then
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("createCashbackRule: saves mapped rule when category does not exist")
    void createCashbackRule_newCategory_savesRule() {
        //given
        String category = "electronics";
        BigDecimal percentage = new BigDecimal("10.00");
        CashbackRule entity = new CashbackRule();
        entity.setId(ruleId);
        entity.setCategory("electronics");
        entity.setPercentage(percentage);

        when(ruleEngineRepository.findByCategory("electronics")).thenReturn(Optional.empty());
        when(ruleEngineMapper.domainToEntity(any(CashbackRuleDomain.class))).thenReturn(entity);

        //when
        ruleEngineService.createCashbackRule(category, percentage, validFrom, validTo);

        //then
        verify(ruleEngineRepository).save(entity);
    }

    @Test
    @DisplayName("createCashbackRule: throws EntityExistsException when rule for category already exists")
    void createCashbackRule_existingCategory_throwsEntityExistsException() {
        //given
        CashbackRule existingRule = new CashbackRule();
        when(ruleEngineRepository.findByCategory("electronics")).thenReturn(Optional.of(existingRule));

        //when & then
        EntityExistsException ex = assertThrows(
                EntityExistsException.class,
                () -> ruleEngineService.createCashbackRule("electronics", new BigDecimal("10.00"), validFrom, validTo)
        );
        assertEquals("Rule for category [electronics] exists", ex.getMessage());
        verify(ruleEngineRepository, never()).save(any());
    }

    @Test
    @DisplayName("getAllRules: returns list of CashbackRuleDTO")
    void getAllRules_rulesExist_returnsDtoList() {
        //given
        CashbackRule ruleEntity = new CashbackRule();
        ruleEntity.setId(ruleId);
        ruleEntity.setCategory("electronics");
        ruleEntity.setPercentage(new BigDecimal("10.00"));
        ruleEntity.setValidFrom(validFrom);
        ruleEntity.setValidTo(validTo);

        CashbackRuleDTO dto = new CashbackRuleDTO(ruleId, "electronics", new BigDecimal("10.00"), validFrom, validTo);

        when(ruleEngineRepository.findAll()).thenReturn(List.of(ruleEntity));
        when(ruleEngineMapper.entityToDto(ruleEntity)).thenReturn(dto);

        //when
        List<CashbackRuleDTO> result = ruleEngineService.getAllRules();

        //then
        assertEquals(1, result.size());
        assertEquals(dto, result.getFirst());
    }

    @Test
    @DisplayName("updateCashbackRule: updates percentage and validTo on existing rule")
    void updateCashbackRule_existingRule_updatesFieldsAndSaves() {
        //given
        CashbackRule entity = new CashbackRule();
        entity.setId(ruleId);
        CashbackRuleDomain domain = CashbackRuleDomain.restoreFromExisting(
                ruleId, "electronics", new BigDecimal("5.00"), validFrom, validTo
        );
        LocalDateTime newValidTo = now.plusDays(60);
        UpdateCashbackModelDTO updateDto = new UpdateCashbackModelDTO(new BigDecimal("12.00"), newValidTo);

        when(ruleEngineRepository.findById(ruleId)).thenReturn(Optional.of(entity));
        when(ruleEngineMapper.entityToDomain(entity)).thenReturn(domain);
        when(ruleEngineMapper.domainToEntity(domain)).thenReturn(entity);

        //when
        ruleEngineService.updateCashbackRule(updateDto, ruleId);

        //then
        verify(ruleEngineRepository).save(entity);
        assertAll(
                () -> assertEquals(new BigDecimal("12.00"), domain.getPercentage()),
                () -> assertEquals(newValidTo, domain.getValidTo())
        );
    }

    @Test
    @DisplayName("updateCashbackRule: throws CashbackRuleNotFoundException when rule does not exist")
    void updateCashbackRule_nonExistingRule_throwsNotFoundException() {
        //given
        when(ruleEngineRepository.findById(ruleId)).thenReturn(Optional.empty());
        UpdateCashbackModelDTO updateDto = new UpdateCashbackModelDTO(new BigDecimal("12.00"), null);

        //when & then
        CashbackRuleNotFoundException ex = assertThrows(
                CashbackRuleNotFoundException.class,
                () -> ruleEngineService.updateCashbackRule(updateDto, ruleId)
        );
        assertEquals("Rule not found: " + ruleId, ex.getMessage());
        verify(ruleEngineRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteCashbackRule: deletes rule when found")
    void deleteCashbackRule_existingRule_deletesRule() {
        //given
        CashbackRule entity = new CashbackRule();
        entity.setId(ruleId);
        when(ruleEngineRepository.findById(ruleId)).thenReturn(Optional.of(entity));

        //when
        ruleEngineService.deleteCashbackRule(ruleId);

        //then
        verify(ruleEngineRepository).delete(entity);
    }

    @Test
    @DisplayName("deleteCashbackRule: does not call delete when rule is not found")
    void deleteCashbackRule_nonExistingRule_doesNothing() {
        //given
        when(ruleEngineRepository.findById(ruleId)).thenReturn(Optional.empty());

        //when
        ruleEngineService.deleteCashbackRule(ruleId);

        //then
        verify(ruleEngineRepository, never()).delete(any());
    }
}
