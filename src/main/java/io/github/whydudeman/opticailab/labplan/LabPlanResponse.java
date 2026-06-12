package io.github.whydudeman.opticailab.labplan;

import java.util.List;

public record LabPlanResponse(
        String title,
        String theory,
        List<LabStepResponse> steps,
        List<String> missingEquipment,
        List<String> commonMistakes,
        List<String> expectedResults
) {
}
