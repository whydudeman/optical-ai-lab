package io.github.whydudeman.opticailab.labplan;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class LabPlanService {

    private static final String SYSTEM_PROMPT = """
            You are an expert assistant for university optics and laser physics laboratory work.
            Given a lab work topic and the equipment the student has available, produce a practical plan.
            Identify the experiment type, check whether the listed equipment is sufficient,
            and list any missing equipment. Steps must be concrete and executable on the listed equipment.
            For each step provide a short YouTube search query (in English) that would find
            a demonstration video of that operation.
            Warn about typical student mistakes and describe the expected results.
            Respond in the language with ISO code: %s.
            """;

    private final Map<LlmProvider, ChatClient> chatClients;

    public LabPlanService(Map<LlmProvider, ChatClient> chatClients) {
        this.chatClients = chatClients;
    }

    public LabPlan generate(LabPlanRequest request) {
        return chatClients.get(request.providerOrDefault()).prompt()
                .system(SYSTEM_PROMPT.formatted(request.languageOrDefault()))
                .user("Lab work topic: %s%nAvailable equipment: %s"
                        .formatted(request.topic(), String.join(", ", request.equipmentOrEmpty())))
                .call()
                .entity(LabPlan.class);
    }
}
