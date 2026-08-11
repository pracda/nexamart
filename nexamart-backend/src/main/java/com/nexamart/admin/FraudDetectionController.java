package com.nexamart.admin;

import com.nexamart.admin.dto.FraudScanResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FraudDetectionController {

    private final FraudDetectionService fraudDetectionService;

    public FraudDetectionController(FraudDetectionService fraudDetectionService) {
        this.fraudDetectionService = fraudDetectionService;
    }

    @GetMapping("/api/admin/fraud-scan")
    public FraudScanResult scan() {
        return fraudDetectionService.scan();
    }
}
