package io.github.whydudeman.opticailab.labplan;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.whydudeman.opticailab.history.PlanHistory;
import io.github.whydudeman.opticailab.history.PlanHistoryRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class LabPlanService {

    private static final int VIDEOS_PER_STEP = 2;

    private static final String SYSTEM_PROMPT = """
            You are an expert assistant for university optics and laser physics laboratory work.
            Given a lab work topic and the equipment the student has available, produce a practical plan.
            Identify the experiment type, check whether the listed equipment is sufficient,
            and list any missing equipment. Steps must be concrete and executable on the listed equipment.
            For each practical step provide a short YouTube search query (in English) that would find
            a demonstration video of that physical operation; for organizational steps
            (briefing, cleanup, report writing) leave the query empty.
            Warn about typical student mistakes and describe the expected results.
            Respond in the language with ISO code: %s.
            """;

    private final Map<LlmProvider, ChatClient> chatClients;
    private final YoutubeSearchService youtubeSearchService;
    private final PlanHistoryRepository planHistoryRepository;
    private final ObjectMapper objectMapper;

    public LabPlanService(Map<LlmProvider, ChatClient> chatClients,
                          YoutubeSearchService youtubeSearchService,
                          PlanHistoryRepository planHistoryRepository,
                          ObjectMapper objectMapper) {
        this.chatClients = chatClients;
        this.youtubeSearchService = youtubeSearchService;
        this.planHistoryRepository = planHistoryRepository;
        this.objectMapper = objectMapper;
    }

    public LabPlanResponse generate(LabPlanRequest request, String userEmail) {
        LabPlan plan = chatClients.get(request.providerOrDefault()).prompt()
                .system(SYSTEM_PROMPT.formatted(request.languageOrDefault()))
                .user("Lab work topic: %s%nAvailable equipment: %s"
                        .formatted(request.topic(), String.join(", ", request.equipmentOrEmpty())))
                .call()
                .entity(LabPlan.class);
        LabPlanResponse response = attachVideos(plan);
        return saveHistory(response, request, userEmail);
    }

    private LabPlanResponse attachVideos(LabPlan plan) {
        List<LabStepResponse> steps = plan.steps().stream()
                .map(step -> LabStepResponse.from(step, searchVideos(step)))
                .toList();
        return new LabPlanResponse(null, plan.title(), plan.theory(), steps,
                plan.missingEquipment(), plan.commonMistakes(), plan.expectedResults());
    }

    private List<YoutubeVideo> searchVideos(LabStep step) {
        if (step.videoSearchQuery() == null || step.videoSearchQuery().isBlank()) {
            return List.of();
        }
        return youtubeSearchService.search(step.videoSearchQuery(), VIDEOS_PER_STEP);
    }

    private LabPlanResponse saveHistory(LabPlanResponse response, LabPlanRequest request, String userEmail) {
        PlanHistory saved = new PlanHistory(
                userEmail, request.topic(), request.languageOrDefault(),
                request.providerOrDefault().name(), toJson(response));
        saved.setEquipment(String.join("\n", request.equipmentOrEmpty()));
        saved = planHistoryRepository.save(saved);
        LabPlanResponse withId = response.withHistoryId(saved.getId());
        saved.setPlanJson(toJson(withId));
        planHistoryRepository.save(saved);
        return withId;
    }

    private String toJson(LabPlanResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize lab plan", e);
        }
    }
}
