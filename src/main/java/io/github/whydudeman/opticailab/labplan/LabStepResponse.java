package io.github.whydudeman.opticailab.labplan;

import java.util.List;

public record LabStepResponse(
        int number,
        String title,
        String description,
        String videoSearchQuery,
        List<YoutubeVideo> videos
) {

    public static LabStepResponse from(LabStep step, List<YoutubeVideo> videos) {
        return new LabStepResponse(step.number(), step.title(), step.description(),
                step.videoSearchQuery(), videos);
    }
}
