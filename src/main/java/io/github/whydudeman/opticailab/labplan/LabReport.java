package io.github.whydudeman.opticailab.labplan;

import java.util.List;

public record LabReport(
        String title,
        String objective,
        List<String> equipmentUsed,
        String procedureSummary,
        String results,
        List<String> conclusions,
        List<String> questionsDiscussed
) {
}
