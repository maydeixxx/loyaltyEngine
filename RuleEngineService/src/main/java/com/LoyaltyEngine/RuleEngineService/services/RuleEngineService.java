package com.LoyaltyEngine.RuleEngineService.services;

import com.LoyaltyEngine.RuleEngineService.exceptions.CashbackRuleNotFoundException;
import com.LoyaltyEngine.RuleEngineService.models.CashbackRule;
import com.LoyaltyEngine.RuleEngineService.models.CashbackRuleDomain;
import com.LoyaltyEngine.RuleEngineService.models.dto.UpdateCashbackModelDTO;
import com.LoyaltyEngine.RuleEngineService.services.interfaces.RuleEngineMapper;
import com.LoyaltyEngine.RuleEngineService.services.interfaces.RuleEngineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RuleEngineService {
    private final RuleEngineRepository ruleEngineRepository;
    private final RuleEngineMapper ruleEngineMapper;
    private final BigDecimal basePercentage = new BigDecimal("1.0");

    @Cacheable(value = "cashback_rules", key = "#category.toLowerCase().trim()")
    public BigDecimal getPercentageForCategory(String category) {
        LocalDateTime now = LocalDateTime.now();
        Optional<BigDecimal> cashbackRule = ruleEngineRepository.findActivePercentageByCategory(category.toLowerCase().trim(), now);
        return cashbackRule.orElse(basePercentage);
    }

    @CacheEvict(value = "cashback_rules", allEntries = true)
    public void createCashbackRule(String category, BigDecimal percentage, LocalDateTime validFrom, LocalDateTime validTo) {
        CashbackRuleDomain cashbackRule = CashbackRuleDomain.createCashbackRule(category, percentage, validFrom, validTo);
        ruleEngineRepository.save(ruleEngineMapper.domainToEntity(cashbackRule));
    }

    public List<CashbackRuleDomain> getAllRules() {
        return ruleEngineRepository.findAll().stream().map(ruleEngineMapper::entityToDomain).toList();
    }

    @Transactional
    @CacheEvict(value = "cashback_rules", allEntries = true)
    public void updateCashbackRule(UpdateCashbackModelDTO newValue, UUID id) {
        CashbackRuleDomain cashbackRuleById = ruleEngineMapper.entityToDomain(ruleEngineRepository.findById(id).orElseThrow(() -> new CashbackRuleNotFoundException("Rule not found: " + id)));

        if (newValue.category() != null) {
            cashbackRuleById.updateCategory(newValue.category().toLowerCase().trim());
        }

        if (newValue.percentage() != null) {
            cashbackRuleById.updatePercentage(newValue.percentage());
        }

        ruleEngineRepository.save(ruleEngineMapper.domainToEntity(cashbackRuleById));
    }

    @Transactional
    @CacheEvict(value = "cashback_rules", allEntries = true)
    public void deleteCashbackRule(UUID id) {
        Optional<CashbackRule> cashbackRuleById = ruleEngineRepository.findById(id);
        cashbackRuleById.ifPresent(ruleEngineRepository::delete);
    }
}
