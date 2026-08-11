package com.nexamart.dispute;

import com.nexamart.auth.User;
import com.nexamart.common.ApiException;
import com.nexamart.dispute.dto.AddMessageRequest;
import com.nexamart.dispute.dto.DisputeResponse;
import com.nexamart.dispute.dto.OpenDisputeRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/disputes")
public class DisputeController {

    private final DisputeService disputeService;

    public DisputeController(DisputeService disputeService) {
        this.disputeService = disputeService;
    }

    @PostMapping
    public ResponseEntity<DisputeResponse> open(@Valid @RequestBody OpenDisputeRequest req,
                                                 @AuthenticationPrincipal User user) {
        if (user.getRole() != com.nexamart.auth.Role.BUYER) {
            throw ApiException.forbidden("Only buyers can open a dispute");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(disputeService.open(req, user));
    }

    @GetMapping
    public List<DisputeResponse> list(@AuthenticationPrincipal User user) {
        return disputeService.listForUser(user);
    }

    @GetMapping("/{id}")
    public DisputeResponse getOne(@PathVariable Long id, @AuthenticationPrincipal User user) {
        return disputeService.getForUser(id, user);
    }

    @PostMapping("/{id}/messages")
    public DisputeResponse addMessage(@PathVariable Long id, @Valid @RequestBody AddMessageRequest req,
                                       @AuthenticationPrincipal User user) {
        return disputeService.addMessage(id, req.body(), user);
    }
}
