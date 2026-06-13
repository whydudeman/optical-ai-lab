package io.github.whydudeman.opticailab.labplan;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.whydudeman.opticailab.history.ChatMessageRepository;
import io.github.whydudeman.opticailab.history.LabJournal;
import io.github.whydudeman.opticailab.history.LabJournalRepository;
import io.github.whydudeman.opticailab.history.PlanHistory;
import io.github.whydudeman.opticailab.history.PlanHistoryRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class LabReportService {

    private static final String CONCLUSIONS_PROMPT = """
            You write a short conclusions paragraph for a student lab report,
            based only on the stated objective and the student's own results.
            Do not invent measurements the student did not provide.
            Write 2-4 sentences in the language with ISO code: %s. Output only the text.
            """;

    private final Map<LlmProvider, ChatClient> chatClients;
    private final PlanHistoryRepository planHistoryRepository;
    private final LabJournalRepository labJournalRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ObjectMapper objectMapper;

    public LabReportService(Map<LlmProvider, ChatClient> chatClients,
                            PlanHistoryRepository planHistoryRepository,
                            LabJournalRepository labJournalRepository,
                            ChatMessageRepository chatMessageRepository,
                            ObjectMapper objectMapper) {
        this.chatClients = chatClients;
        this.planHistoryRepository = planHistoryRepository;
        this.labJournalRepository = labJournalRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.objectMapper = objectMapper;
    }

    public LabReport generateAndComplete(Long historyId, LlmProvider provider, String userEmail) {
        PlanHistory history = requireOwned(historyId, userEmail);
        if (history.getReportJson() != null) {
            return readReport(history.getReportJson());
        }
        LabJournal journal = labJournalRepository.findByHistoryId(historyId).orElse(null);
        String results = journal == null ? null : journal.getResults();
        if (results == null || results.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Fill in the results in the lab journal before finishing");
        }

        LabPlanResponse plan = parsePlan(history.getPlanJson());
        List<String> equipment = splitLines(history.getEquipment());
        List<String> procedure = plan.steps().stream()
                .map(step -> step.number() + ". " + step.title())
                .toList();
        List<String> questions = chatMessageRepository.findByHistoryIdOrderByCreatedAtAsc(historyId).stream()
                .filter(message -> "user".equals(message.getRole()))
                .map(message -> message.getText())
                .toList();
        String conclusions = journal.getConclusions() != null && !journal.getConclusions().isBlank()
                ? journal.getConclusions()
                : draftConclusions(plan.title(), results, history.getLanguage(), provider);

        LabReport report = new LabReport(plan.title(), plan.title(), plan.theory(),
                equipment, procedure, results, conclusions, questions);

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
        return readReport(history.getReportJson());
    }

    private String draftConclusions(String objective, String results, String language, LlmProvider provider) {
        return chatClients.get(provider).prompt()
                .system(CONCLUSIONS_PROMPT.formatted(language))
                .user("Objective: %s%n%nStudent results:%n%s".formatted(objective, results))
                .call()
                .content();
    }

    private PlanHistory requireOwned(Long id, String userEmail) {
        return planHistoryRepository.findByIdAndUserEmail(id, userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found"));
    }

    private List<String> splitLines(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split("\\R"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList();
    }

    private LabPlanResponse parsePlan(String json) {
        try {
            return objectMapper.readValue(json, LabPlanResponse.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to parse stored plan", e);
        }
    }

    private LabReport readReport(String json) {
        try {
            return objectMapper.readerFor(LabReport.class)
                    .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .readValue(json);
        } catch (Exception e) {
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
