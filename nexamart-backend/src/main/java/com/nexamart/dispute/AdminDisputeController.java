package com.nexamart.dispute;

import com.nexamart.dispute.dto.DisputeResponse;
import com.nexamart.dispute.dto.DisputeSummary;
import com.nexamart.dispute.dto.ResolveDisputeRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/disputes")
public class AdminDisputeController {

    private final DisputeService disputeService;
    private final DisputeSummaryService disputeSummaryService;

    public AdminDisputeController(DisputeService disputeService, DisputeSummaryService disputeSummaryService) {
        this.disputeService = disputeService;
        this.disputeSummaryService = disputeSummaryService;
    }

    @PatchMapping("/{id}/resolve")
    public DisputeResponse resolve(@PathVariable Long id, @Valid @RequestBody ResolveDisputeRequest req) {
        return disputeService.resolve(id, req);
    }

    @PostMapping("/{id}/ai-summary")
    public DisputeSummary aiSummary(@PathVariable Long id) {
        return disputeSummaryService.summarize(id);
    }
}
