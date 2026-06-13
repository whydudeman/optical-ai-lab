package io.github.whydudeman.opticailab.labplan;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.whydudeman.opticailab.history.ChatMessage;
import io.github.whydudeman.opticailab.history.ChatMessageRepository;
import io.github.whydudeman.opticailab.history.PlanHistory;
import io.github.whydudeman.opticailab.history.PlanHistoryRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LabReportService {

    private static final String SYSTEM_PROMPT = """
            You are an assistant that writes a university laboratory work report for a student.
            You are given the lab work plan the student followed and the dialogue the student
            had with the assistant while performing the work. Write a concise academic report:
            objective, equipment actually used, summary of the procedure as performed,
            results section (use concrete values from the dialogue when the student mentioned
            measurements; otherwise describe the expected character of the results),
            conclusions, and a short list of the questions the student worked through
            during the session (empty list if there was no dialogue).
            Write the report in the language with ISO code: %s.
            """;

    private final Map<LlmProvider, ChatClient> chatClients;
    private final PlanHistoryRepository planHistoryRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ObjectMapper objectMapper;

    public LabReportService(Map<LlmProvider, ChatClient> chatClients,
                            PlanHistoryRepository planHistoryRepository,
                            ChatMessageRepository chatMessageRepository,
                            ObjectMapper objectMapper) {
        this.chatClients = chatClients;
        this.planHistoryRepository = planHistoryRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.objectMapper = objectMapper;
    }

    public LabReport generateAndComplete(Long historyId, LlmProvider provider, String userEmail) {
        PlanHistory history = requireOwned(historyId, userEmail);
        if (history.getReportJson() != null) {
            return readReport(history);
        }
        String dialogue = chatMessageRepository.findByHistoryIdOrderByCreatedAtAsc(historyId).stream()
                .map(message -> message.getRole() + ": " + message.getText())
                .collect(Collectors.joining("\n\n"));
        LabReport report = chatClients.get(provider).prompt()
                .system(SYSTEM_PROMPT.formatted(history.getLanguage()))
                .user("Lab plan:\n%s\n\nStudent dialogue during the work:\n%s"
                        .formatted(history.getPlanJson(), dialogue.isBlank() ? "(no dialogue)" : dialogue))
                .call()
                .entity(LabReport.class);
        history.setReportJson(toJson(report));
        history.setCompletedAt(Instant.now());
        planHistoryRepository.save(history);
        return report;
    }

    public LabReport getReport(Long historyId, String userEmail) {
        PlanHistory history = requireOwned(historyId, userEmail);
        if (history.getReportJson() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not generated yet");
        }
        return readReport(history);
    }

    private PlanHistory requireOwned(Long id, String userEmail) {
        return planHistoryRepository.findByIdAndUserEmail(id, userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found"));
    }

    private LabReport readReport(PlanHistory history) {
        try {
            return objectMapper.readValue(history.getReportJson(), LabReport.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to read stored report", e);
        }
    }

    private String toJson(LabReport report) {
        try {
            return objectMapper.writeValueAsString(report);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize report", e);
        }
    }
}
