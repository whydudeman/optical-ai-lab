package io.github.whydudeman.opticailab.labplan;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Service
public class YoutubeSearchService {

    private static final Logger log = LoggerFactory.getLogger(YoutubeSearchService.class);

    private final RestClient restClient;
    private final String apiKey;

    public YoutubeSearchService(RestClient.Builder restClientBuilder,
                                @Value("${youtube.api-key}") String apiKey) {
        this.restClient = restClientBuilder.baseUrl("https://www.googleapis.com/youtube/v3").build();
        this.apiKey = apiKey;
    }

    public List<YoutubeVideo> search(String query, int maxResults) {
        if (apiKey == null || apiKey.isBlank() || "not-set".equals(apiKey)) {
            return List.of();
        }
        try {
            JsonNode response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/search")
                            .queryParam("part", "snippet")
                            .queryParam("type", "video")
                            .queryParam("maxResults", maxResults)
                            .queryParam("q", query)
                            .queryParam("key", apiKey)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
            List<YoutubeVideo> videos = new ArrayList<>();
            for (JsonNode item : response.path("items")) {
                videos.add(YoutubeVideo.of(
                        item.path("id").path("videoId").asText(),
                        item.path("snippet").path("title").asText()));
            }
            return videos;
        } catch (Exception e) {
            log.warn("YouTube search failed for query '{}': {}", query, e.getMessage());
            return List.of();
        }
    }
}
