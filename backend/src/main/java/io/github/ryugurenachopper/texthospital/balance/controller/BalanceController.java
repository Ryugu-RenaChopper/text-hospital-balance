package io.github.ryugurenachopper.texthospital.balance.controller;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.ryugurenachopper.texthospital.balance.dto.AnalysisResponse;
import io.github.ryugurenachopper.texthospital.balance.dto.GovernanceRequest;
import io.github.ryugurenachopper.texthospital.balance.dto.GovernanceResponse;
import io.github.ryugurenachopper.texthospital.balance.governance.RandomResamplingService;
import io.github.ryugurenachopper.texthospital.balance.service.BalanceAnalysisService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/balance")
public class BalanceController {
    private final BalanceAnalysisService analysisService;
    private final RandomResamplingService resamplingService;

    public BalanceController(
            BalanceAnalysisService analysisService,
            RandomResamplingService resamplingService
    ) {
        this.analysisService = analysisService;
        this.resamplingService = resamplingService;
    }

    @PostMapping("/analyze")
    public ResponseEntity<AnalysisResponse> analyze(
            @RequestBody JsonNode dataset,
            @RequestParam(required = false) Double imbalanceRatioThreshold,
            @RequestParam(required = false) Double giniThreshold,
            @RequestParam(required = false) String thresholdMode
    ) {
        return ResponseEntity.ok(analysisService.analyze(
                dataset, imbalanceRatioThreshold, giniThreshold, thresholdMode));
    }

    @PostMapping("/govern")
    public ResponseEntity<GovernanceResponse> govern(@RequestBody GovernanceRequest request) {
        return ResponseEntity.ok(resamplingService.govern(request));
    }
}
