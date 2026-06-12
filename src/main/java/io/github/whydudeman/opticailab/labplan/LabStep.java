package io.github.whydudeman.opticailab.labplan;

public record LabStep(
        int number,
        String title,
        String description,
        String videoSearchQuery
) {
}
