package io.github.whydudeman.opticailab.labplan;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PlanChatRequest(
        @NotNull Long historyId,
        @NotBlank String question,
        LlmProvider provider
) {

    public LlmProvider providerOrDefault() {
        return provider == null ? LlmProvider.ANTHROPIC : provider;
    }
}
