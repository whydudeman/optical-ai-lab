package io.github.whydudeman.opticailab.labplan;

import io.github.whydudeman.opticailab.history.LabJournal;
import io.github.whydudeman.opticailab.history.LabJournalRepository;
import io.github.whydudeman.opticailab.history.PlanHistoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.Instant;

@RestController
@RequestMapping("/api/lab-plans/{historyId}/journal")
public class JournalController {

    public record JournalRequest(String results, String conclusions) {
    }

    public record JournalResponse(String results, String conclusions) {
    }

    private final LabJournalRepository labJournalRepository;
    private final PlanHistoryRepository planHistoryRepository;

    public JournalController(LabJournalRepository labJournalRepository,
                             PlanHistoryRepository planHistoryRepository) {
        this.labJournalRepository = labJournalRepository;
        this.planHistoryRepository = planHistoryRepository;
    }

    @GetMapping
    public JournalResponse get(@PathVariable Long historyId, Principal principal) {
        requireOwned(historyId, principal);
        return labJournalRepository.findByHistoryId(historyId)
                .map(j -> new JournalResponse(j.getResults(), j.getConclusions()))
                .orElse(new JournalResponse(null, null));
    }

    @PutMapping
    public JournalResponse save(@PathVariable Long historyId,
                                @RequestBody JournalRequest request,
                                Principal principal) {
        requireOwned(historyId, principal);
        LabJournal journal = labJournalRepository.findByHistoryId(historyId)
                .orElseGet(() -> new LabJournal(historyId));
        journal.setResults(request.results());
        journal.setConclusions(request.conclusions());
        journal.setUpdatedAt(Instant.now());
        labJournalRepository.save(journal);
        return new JournalResponse(journal.getResults(), journal.getConclusions());
    }

    private void requireOwned(Long historyId, Principal principal) {
        planHistoryRepository.findByIdAndUserEmail(historyId, principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found"));
    }
}
