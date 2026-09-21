package com.LoyaltyEngine.RuleEngineService.api;

import com.LoyaltyEngine.RuleEngineService.models.dto.CreateRuleDTO;
import com.LoyaltyEngine.RuleEngineService.models.dto.UpdateCashbackModelDTO;
import com.LoyaltyEngine.RuleEngineService.services.RuleEngineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/rules")
public class RuleEngineController {
    private final RuleEngineService ruleEngineService;

    @PostMapping()
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createCashbackRule(@RequestBody @Validated CreateRuleDTO dto) {
        ruleEngineService.createCashbackRule(dto.category().toLowerCase(), dto.percentage(), dto.validFrom(), dto.validTo());
        return ResponseEntity.status(201).body(String.format("New rule by category %s successfully created!", dto.category()));
    }

    @GetMapping()
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllRules() {
        return ResponseEntity.ok().body(ruleEngineService.getAllRules());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateRule(@PathVariable UUID id, @RequestBody @Validated UpdateCashbackModelDTO dto) {
        ruleEngineService.updateCashbackRule(dto, id);
        return ResponseEntity.ok().body(String.format("Rule %s successfully updated!", id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteRule(@PathVariable UUID id) {
        ruleEngineService.deleteCashbackRule(id);
        return ResponseEntity.status(204).build();
    }
}
