package io.github.whydudeman.opticailab.history;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/history")
public class PlanHistoryController {

    private final PlanHistoryRepository planHistoryRepository;

    public PlanHistoryController(PlanHistoryRepository planHistoryRepository) {
        this.planHistoryRepository = planHistoryRepository;
    }

    @GetMapping
    public List<PlanHistoryItemResponse> getAll(Principal principal) {
        return planHistoryRepository.findByUserEmailOrderByCreatedAtDesc(principal.getName()).stream()
                .map(PlanHistoryItemResponse::from)
                .toList();
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getOne(@PathVariable Long id, Principal principal) {
        return planHistoryRepository.findByIdAndUserEmail(id, principal.getName())
                .map(history -> ResponseEntity.ok(history.getPlanJson()))
                .orElse(ResponseEntity.notFound().build());
    }
}
