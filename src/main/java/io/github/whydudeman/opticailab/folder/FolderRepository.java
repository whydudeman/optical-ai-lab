package io.github.whydudeman.opticailab.folder;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FolderRepository extends JpaRepository<Folder, Long> {

    List<Folder> findByUserEmailOrderByNameAsc(String userEmail);

    Optional<Folder> findByIdAndUserEmail(Long id, String userEmail);
}
