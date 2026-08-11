package com.nexamart.dispute;

import com.nexamart.auth.Role;
import com.nexamart.auth.User;
import com.nexamart.common.ApiException;
import com.nexamart.dispute.dto.DisputeResponse;
import com.nexamart.dispute.dto.OpenDisputeRequest;
import com.nexamart.dispute.dto.ResolveDisputeRequest;
import com.nexamart.order.Order;
import com.nexamart.order.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class DisputeService {

    private final DisputeRepository disputeRepository;
    private final OrderRepository orderRepository;

    public DisputeService(DisputeRepository disputeRepository, OrderRepository orderRepository) {
        this.disputeRepository = disputeRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public DisputeResponse open(OpenDisputeRequest req, User buyer) {
        Order order = orderRepository.findByIdAndBuyer_Id(req.orderId(), buyer.getId())
                .orElseThrow(() -> ApiException.notFound("Order not found: " + req.orderId()));

        Dispute dispute = new Dispute(order, buyer, req.reason());
        dispute.addMessage(new DisputeMessage(buyer, req.message()));
        return DisputeResponse.from(disputeRepository.save(dispute));
    }

    public List<DisputeResponse> listForUser(User user) {
        List<Dispute> disputes = switch (user.getRole()) {
            case BUYER -> disputeRepository.findByOpenedBy_IdOrderByCreatedAtDesc(user.getId());
            case SELLER -> disputeRepository.findForSeller(user.getId());
            case ADMIN -> disputeRepository.findAllByOrderByCreatedAtDesc();
            default -> List.of();
        };
        return disputes.stream().map(DisputeResponse::from).toList();
    }

    public DisputeResponse getForUser(Long disputeId, User user) {
        return DisputeResponse.from(authorize(findOrThrow(disputeId), user));
    }

    @Transactional
    public DisputeResponse addMessage(Long disputeId, String body, User user) {
        Dispute dispute = authorize(findOrThrow(disputeId), user);
        dispute.addMessage(new DisputeMessage(user, body));
        dispute.setUpdatedAt(Instant.now());
        return DisputeResponse.from(disputeRepository.save(dispute));
    }

    @Transactional
    public DisputeResponse resolve(Long disputeId, ResolveDisputeRequest req) {
        Dispute dispute = findOrThrow(disputeId);
        dispute.setStatus(req.status());
        dispute.setResolutionNote(req.resolutionNote());
        dispute.setUpdatedAt(Instant.now());
        return DisputeResponse.from(disputeRepository.save(dispute));
    }

    Dispute findOrThrow(Long disputeId) {
        return disputeRepository.findById(disputeId)
                .orElseThrow(() -> ApiException.notFound("Dispute not found: " + disputeId));
    }

    private Dispute authorize(Dispute dispute, User user) {
        if (user.getRole() == Role.ADMIN) {
            return dispute;
        }
        if (user.getRole() == Role.BUYER && dispute.getOpenedBy().getId().equals(user.getId())) {
            return dispute;
        }
        if (user.getRole() == Role.SELLER) {
            boolean ownsAnItem = dispute.getOrder().getItems().stream()
                    .anyMatch(item -> item.getProduct().getSeller().getId().equals(user.getId()));
            if (ownsAnItem) {
                return dispute;
            }
        }
        throw ApiException.forbidden("You do not have access to this dispute");
    }
}
