package io.github.whydudeman.opticailab.labplan;

import java.util.List;

public record LabPlan(
        String title,
        String theory,
        List<LabStep> steps,
        List<String> missingEquipment,
        List<String> commonMistakes,
        List<String> expectedResults
) {
}
