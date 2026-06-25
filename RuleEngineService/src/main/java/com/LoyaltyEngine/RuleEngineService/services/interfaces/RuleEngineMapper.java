package com.LoyaltyEngine.RuleEngineService.services.interfaces;

import com.LoyaltyEngine.RuleEngineService.models.CashbackRule;
import com.LoyaltyEngine.RuleEngineService.models.CashbackRuleDomain;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RuleEngineMapper {
    CashbackRuleDomain entityToDomain(CashbackRule cashbackRule);
    CashbackRule domainToEntity(CashbackRuleDomain cashbackRule);
}
