package com.LoyaltyEngine.RuleEngineService.services;

import com.LoyaltyEngine.RuleEngineService.exceptions.CashbackRuleNotFoundException;
import com.LoyaltyEngine.RuleEngineService.exceptions.CashbackUpdateException;
import com.LoyaltyEngine.RuleEngineService.models.CashbackRule;
import com.LoyaltyEngine.RuleEngineService.models.CashbackRuleDomain;
import com.LoyaltyEngine.RuleEngineService.models.dto.UpdateCashbackModelDTO;
import com.LoyaltyEngine.RuleEngineService.services.interfaces.RuleEngineRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.tracing.ScopedSpan;
import io.micrometer.tracing.Tracer;
import jakarta.persistence.EntityExistsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RuleEngineService {
    private final RuleEngineRepository ruleEngineRepository;
    private final RuleEngineMapper ruleEngineMapper;

    private final MeterRegistry registry;
    private final Tracer tracer;

    @Cacheable(value = "cashback_rules", key = "#category.toLowerCase().trim()")
    public Optional<BigDecimal> getPercentageForCategory(String category) {
        try {
            LocalDateTime now = LocalDateTime.now();
            return ruleEngineRepository.findActivePercentageByCategory(category.toLowerCase().trim(), now);
        } catch (Exception e) {
            log.error("Error finding rules for category {}: {}", category.toLowerCase().trim(), e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @CacheEvict(value = "cashback_rules", allEntries = true)
    public void createCashbackRule(String category, BigDecimal percentage, LocalDateTime validFrom, LocalDateTime validTo) {
        ScopedSpan span = tracer.startScopedSpan("create-new-rule-span");
        try {
            if (ruleEngineRepository.findByCategory(category).isPresent())
                throw new EntityExistsException("Rule for category [%s] exists".formatted(category));
            CashbackRuleDomain cashbackRule = CashbackRuleDomain.createCashbackRule(category, percentage, validFrom, validTo);
            ruleEngineRepository.save(ruleEngineMapper.domainToEntity(cashbackRule));
            registry.counter("create.new.rule.count", "status", "successful").increment();
            span.tag("status", "SUCCESSFUL");
        } catch (EntityExistsException e) {
            log.error(e.getMessage());
            span.error(e);
            span.tag("error.message", e.getMessage());
            span.tag("status", "FAILED");
            registry.counter("create.new.rule.count", "status", "failed").increment();
            throw e;
        } catch (Exception e) {
            log.error("Error creating new rule: {}", e.getMessage());
            span.error(e);
            span.tag("error.message", e.getMessage());
            span.tag("status", "FAILED");
            registry.counter("create.new.rule.count", "status", "failed").increment();
            throw new RuntimeException(e);
        } finally {
            span.end();
        }
    }

    public List<CashbackRuleDomain> getAllRules() {
        return ruleEngineRepository.findAll().stream().map(ruleEngineMapper::entityToDomain).toList();
    }

    @Transactional
    @CacheEvict(value = "cashback_rules", allEntries = true)
    public void updateCashbackRule(UpdateCashbackModelDTO newValue, UUID id) {
        try {
            CashbackRuleDomain cashbackRuleById = ruleEngineMapper.entityToDomain(ruleEngineRepository.findById(id).orElseThrow(() -> new CashbackRuleNotFoundException("Rule not found: " + id)));

            if (newValue.percentage() != null) cashbackRuleById.updatePercentage(newValue.percentage());
            if (newValue.validTo() != null) cashbackRuleById.updateValidTo(newValue.validTo());

            ruleEngineRepository.save(ruleEngineMapper.domainToEntity(cashbackRuleById));
        } catch (CashbackUpdateException e) {
            throw e;
        } catch (CashbackRuleNotFoundException e) {
            log.error(e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error updating cashback rule: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Transactional
    @CacheEvict(value = "cashback_rules", allEntries = true)
    public void deleteCashbackRule(UUID id) {
        Optional<CashbackRule> cashbackRuleById = ruleEngineRepository.findById(id);
        cashbackRuleById.ifPresent(ruleEngineRepository::delete);
    }
}
