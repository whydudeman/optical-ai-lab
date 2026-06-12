package io.github.whydudeman.opticailab.labplan;

public record YoutubeVideo(
        String videoId,
        String title,
        String url
) {

    public static YoutubeVideo of(String videoId, String title) {
        return new YoutubeVideo(videoId, title, "https://www.youtube.com/watch?v=" + videoId);
    }
}
