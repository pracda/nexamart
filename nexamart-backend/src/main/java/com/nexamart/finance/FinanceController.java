package com.nexamart.finance;

import com.nexamart.finance.dto.AnomalyAlert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FinanceController {

    private final AnomalyAlertService anomalyAlertService;

    public FinanceController(AnomalyAlertService anomalyAlertService) {
        this.anomalyAlertService = anomalyAlertService;
    }

    @GetMapping("/api/finance/anomaly-scan")
    public AnomalyAlert anomalyScan() {
        return anomalyAlertService.checkRevenueAnomaly();
    }
}
