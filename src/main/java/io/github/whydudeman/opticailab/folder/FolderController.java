package io.github.whydudeman.opticailab.folder;

import io.github.whydudeman.opticailab.history.PlanHistoryRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/folders")
public class FolderController {

    public record FolderRequest(@NotBlank String name) {
    }

    public record FolderResponse(Long id, String name) {

        static FolderResponse from(Folder folder) {
            return new FolderResponse(folder.getId(), folder.getName());
        }
    }

    private final FolderRepository folderRepository;
    private final PlanHistoryRepository planHistoryRepository;

    public FolderController(FolderRepository folderRepository,
                            PlanHistoryRepository planHistoryRepository) {
        this.folderRepository = folderRepository;
        this.planHistoryRepository = planHistoryRepository;
    }

    @GetMapping
    public List<FolderResponse> getAll(Principal principal) {
        return folderRepository.findByUserEmailOrderByNameAsc(principal.getName()).stream()
                .map(FolderResponse::from)
                .toList();
    }

    @PostMapping
    public FolderResponse create(@Valid @RequestBody FolderRequest request, Principal principal) {
        return FolderResponse.from(folderRepository.save(new Folder(principal.getName(), request.name())));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public void delete(@PathVariable Long id, Principal principal) {
        Folder folder = folderRepository.findByIdAndUserEmail(id, principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        planHistoryRepository.clearFolder(folder.getId());
        folderRepository.delete(folder);
    }
}
