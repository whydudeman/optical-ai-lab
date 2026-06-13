package io.github.whydudeman.opticailab.labplan;

import java.util.List;

public record LabReport(
        String title,
        String objective,
        String theory,
        List<String> equipmentUsed,
        List<String> procedure,
        String results,
        String conclusions,
        List<String> questionsDiscussed
) {
}
