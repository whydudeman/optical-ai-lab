package io.github.whydudeman.opticailab.labplan;

import io.github.whydudeman.opticailab.history.ChatMessage;
import io.github.whydudeman.opticailab.history.ChatMessageRepository;
import io.github.whydudeman.opticailab.history.PlanHistory;
import io.github.whydudeman.opticailab.history.PlanHistoryRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Service
public class PlanChatService {

    private static final String SYSTEM_PROMPT = """
            You are an assistant helping a student perform an optics laboratory work.
            The student is following the lab plan below. Answer their question concretely
            and practically in the context of this plan and its equipment.
            Answer in the same language as the question.

            Lab plan:
            %s
            """;

    private final Map<LlmProvider, ChatClient> chatClients;
    private final PlanHistoryRepository planHistoryRepository;
    private final ChatMessageRepository chatMessageRepository;

    public PlanChatService(Map<LlmProvider, ChatClient> chatClients,
                           PlanHistoryRepository planHistoryRepository,
                           ChatMessageRepository chatMessageRepository) {
        this.chatClients = chatClients;
        this.planHistoryRepository = planHistoryRepository;
        this.chatMessageRepository = chatMessageRepository;
    }

    public String answer(PlanChatRequest request, String userEmail) {
        PlanHistory history = planHistoryRepository.findByIdAndUserEmail(request.historyId(), userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found"));
        String answer = chatClients.get(request.providerOrDefault()).prompt()
                .system(SYSTEM_PROMPT.formatted(history.getPlanJson()))
                .user(request.question())
                .call()
                .content();
        chatMessageRepository.save(new ChatMessage(history.getId(), "user", request.question()));
        chatMessageRepository.save(new ChatMessage(history.getId(), "assistant", answer));
        return answer;
    }
}
