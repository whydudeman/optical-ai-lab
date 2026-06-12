package io.github.whydudeman.opticailab.labplan;

import java.util.List;

public record LabPlanResponse(
        Long historyId,
        String title,
        String theory,
        List<LabStepResponse> steps,
        List<String> missingEquipment,
        List<String> commonMistakes,
        List<String> expectedResults
) {

    public LabPlanResponse withHistoryId(Long id) {
        return new LabPlanResponse(id, title, theory, steps, missingEquipment, commonMistakes, expectedResults);
    }
}
