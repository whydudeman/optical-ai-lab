package io.github.whydudeman.opticailab.labplan;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record LabPlanRequest(
        @NotBlank String topic,
        List<String> equipment,
        String language
) {

    public String languageOrDefault() {
        return language == null || language.isBlank() ? "ru" : language;
    }

    public List<String> equipmentOrEmpty() {
        return equipment == null ? List.of() : equipment;
    }
}
