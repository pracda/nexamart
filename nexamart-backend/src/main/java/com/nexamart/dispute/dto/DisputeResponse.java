package com.nexamart.dispute.dto;

import com.nexamart.dispute.Dispute;
import com.nexamart.dispute.DisputeMessage;
import com.nexamart.dispute.DisputeStatus;

import java.time.Instant;
import java.util.List;

public record DisputeResponse(
        Long id,
        Long orderId,
        DisputeStatus status,
        String reason,
        String openedByName,
        String resolutionNote,
        Instant createdAt,
        Instant updatedAt,
        List<MessageResponse> messages
) {
    public record MessageResponse(Long id, String authorName, String authorRole, String body, Instant createdAt) {
        public static MessageResponse from(DisputeMessage m) {
            return new MessageResponse(m.getId(), m.getAuthor().getFullName(), m.getAuthor().getRole().name(),
                    m.getBody(), m.getCreatedAt());
        }
    }

    public static DisputeResponse from(Dispute d) {
        return new DisputeResponse(
                d.getId(), d.getOrder().getId(), d.getStatus(), d.getReason(), d.getOpenedBy().getFullName(),
                d.getResolutionNote(), d.getCreatedAt(), d.getUpdatedAt(),
                d.getMessages().stream().map(MessageResponse::from).toList()
        );
    }
}
