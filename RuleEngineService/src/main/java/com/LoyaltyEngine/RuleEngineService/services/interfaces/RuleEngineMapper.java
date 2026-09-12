package com.LoyaltyEngine.RuleEngineService.services.interfaces;

import com.LoyaltyEngine.RuleEngineService.models.CashbackRule;
import com.LoyaltyEngine.RuleEngineService.models.CashbackRuleDomain;
import org.springframework.stereotype.Component;

@Component
public class RuleEngineMapper {
    public CashbackRuleDomain entityToDomain(CashbackRule entity) {
        return CashbackRuleDomain.restoreFromExisting(
                entity.getId(),
                entity.getCategory(),
                entity.getPercentage(),
                entity.getValidFrom(),
                entity.getValidTo()
        );
    }

    public CashbackRule domainToEntity(CashbackRuleDomain domain) {
        CashbackRule cashbackRule = new CashbackRule();
        cashbackRule.setId(domain.getId().value());
        cashbackRule.setCategory(domain.getCategory());
        cashbackRule.setPercentage(domain.getPercentage());
        cashbackRule.setValidFrom(domain.getValidFrom());
        cashbackRule.setValidTo(domain.getValidTo());

        return cashbackRule;
    }
}
