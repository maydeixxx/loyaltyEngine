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

    @Cacheable(value = "cashback_rules", key = "#category")
    public BigDecimal getPercentageForCategory(String category) {
        Optional<CashbackRule> cashbackRuleByCategory = ruleEngineRepository.getCashbackRuleByCategory(category);

        if (cashbackRuleByCategory.isPresent()) {
            CashbackRule cashbackRule = cashbackRuleByCategory.get();
            LocalDateTime validFrom = cashbackRule.getValidFrom();
            LocalDateTime validTo = cashbackRule.getValidTo();

            if ((validFrom.isBefore(LocalDateTime.now()) || validFrom.isEqual(LocalDateTime.now())) && ((validTo.isAfter(LocalDateTime.now())) || validTo.isEqual(LocalDateTime.now()))) {
                return cashbackRule.getPercentage();
            }
        }

        return basePercentage;
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
        CashbackRule cashbackRuleById = ruleEngineRepository.findById(id).orElseThrow(() -> new CashbackRuleNotFoundException("Rule not found: " + id));

        if (newValue.category() != null) {
            cashbackRuleById.setCategory(newValue.category());
        }

        if (newValue.percentage() != null) {
            cashbackRuleById.setPercentage(newValue.percentage());
        }

        if (newValue.validFrom() != null) {
            cashbackRuleById.setValidFrom(newValue.validFrom());
        }

        if (newValue.validTo() != null) {
            cashbackRuleById.setValidTo(newValue.validTo());
        }

        ruleEngineRepository.save(cashbackRuleById);
    }

    @Transactional
    @CacheEvict(value = "cashback_rules", allEntries = true)
    public void deleteCashbackRule(UUID id) {
        Optional<CashbackRule> cashbackRuleById = ruleEngineRepository.findById(id);
        cashbackRuleById.ifPresent(ruleEngineRepository::delete);
    }
}
