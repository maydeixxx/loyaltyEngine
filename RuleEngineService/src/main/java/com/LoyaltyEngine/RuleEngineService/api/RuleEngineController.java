package com.LoyaltyEngine.RuleEngineService.api;

import com.LoyaltyEngine.RuleEngineService.models.dto.CreateRuleDTO;
import com.LoyaltyEngine.RuleEngineService.models.dto.UpdateCashbackModelDTO;
import com.LoyaltyEngine.RuleEngineService.services.RuleEngineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<?> createCashbackRule(@RequestBody @Validated CreateRuleDTO dto) {
        ruleEngineService.createCashbackRule(dto.getCategory(), dto.getPercentage(), dto.getValidFrom(), dto.getValidTo());
        return ResponseEntity.status(201).body(String.format("Новое правило для категории %s успешно создано!", dto.getCategory()));
    }

    @GetMapping()
    public ResponseEntity<?> getAllRules() {
        return ResponseEntity.ok().body(ruleEngineService.getAllRules());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateRule(@PathVariable UUID id, @RequestBody UpdateCashbackModelDTO dto) {
        ruleEngineService.updateCashbackRule(dto, id);
        return ResponseEntity.ok().body(String.format("Правило %s успешно обновлено!", id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRule(@PathVariable UUID id) {
        ruleEngineService.deleteCashbackRule(id);
        return ResponseEntity.status(204).build();
    }
}
