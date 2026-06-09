package com.LoyaltyEngine.RuleEngineService.services;

import com.LoyaltyEngine.RuleEngineService.models.CashbackRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RuleEngineRepository extends JpaRepository<CashbackRule, UUID> {
    Optional<CashbackRule> getCashbackRuleByCategory(String category);
}
