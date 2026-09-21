package com.LoyaltyEngine.RuleEngineService.services.interfaces;

import com.LoyaltyEngine.RuleEngineService.models.CashbackRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RuleEngineRepository extends JpaRepository<CashbackRule, UUID> {
    @Query("""
                    SELECT r.percentage FROM CashbackRule r
                    WHERE r.category = :category
                    AND :now >= r.validFrom
                    AND :now <= r.validTo
                    ORDER BY r.validFrom DESC
                    LIMIT 1
            """)
    Optional<BigDecimal> findActivePercentageByCategory(String category, LocalDateTime now);
}
