package io.github.whydudeman.opticailab.history;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/history")
public class PlanHistoryController {

    public record MoveToFolderRequest(Long folderId) {
    }

    public record ChatMessageResponse(String role, String text) {

        static ChatMessageResponse from(ChatMessage message) {
            return new ChatMessageResponse(message.getRole(), message.getText());
        }
    }

    private final PlanHistoryRepository planHistoryRepository;
    private final ChatMessageRepository chatMessageRepository;

    public PlanHistoryController(PlanHistoryRepository planHistoryRepository,
                                 ChatMessageRepository chatMessageRepository) {
        this.planHistoryRepository = planHistoryRepository;
        this.chatMessageRepository = chatMessageRepository;
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

    @GetMapping("/{id}/chat")
    public List<ChatMessageResponse> getChat(@PathVariable Long id, Principal principal) {
        requireOwned(id, principal);
        return chatMessageRepository.findByHistoryIdOrderByCreatedAtAsc(id).stream()
                .map(ChatMessageResponse::from)
                .toList();
    }

    @PutMapping("/{id}/folder")
    public Map<String, Object> moveToFolder(@PathVariable Long id,
                                            @RequestBody MoveToFolderRequest request,
                                            Principal principal) {
        PlanHistory history = requireOwned(id, principal);
        history.setFolderId(request.folderId());
        planHistoryRepository.save(history);
        return Map.of("id", history.getId());
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id, Principal principal) {
        PlanHistory history = requireOwned(id, principal);
        chatMessageRepository.deleteAll(chatMessageRepository.findByHistoryIdOrderByCreatedAtAsc(id));
        planHistoryRepository.delete(history);
    }

    private PlanHistory requireOwned(Long id, Principal principal) {
        return planHistoryRepository.findByIdAndUserEmail(id, principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
