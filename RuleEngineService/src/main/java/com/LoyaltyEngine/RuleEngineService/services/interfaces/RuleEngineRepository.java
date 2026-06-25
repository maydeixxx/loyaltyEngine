package com.LoyaltyEngine.RuleEngineService.services.interfaces;

import com.LoyaltyEngine.RuleEngineService.models.CashbackRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RuleEngineRepository extends JpaRepository<CashbackRule, UUID> {
    List<CashbackRule> getCashbackRuleByCategoryOrderByValidFromDesc(String category);
}
