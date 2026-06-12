package io.github.whydudeman.opticailab.history;

import java.time.Instant;

public record PlanHistoryItemResponse(
        Long id,
        String topic,
        String language,
        String provider,
        Instant createdAt
) {

    public static PlanHistoryItemResponse from(PlanHistory history) {
        return new PlanHistoryItemResponse(history.getId(), history.getTopic(),
                history.getLanguage(), history.getProvider(), history.getCreatedAt());
    }
}
