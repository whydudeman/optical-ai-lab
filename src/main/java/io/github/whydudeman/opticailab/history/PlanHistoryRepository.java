package io.github.whydudeman.opticailab.history;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlanHistoryRepository extends JpaRepository<PlanHistory, Long> {

    List<PlanHistory> findByUserEmailOrderByCreatedAtDesc(String userEmail);

    Optional<PlanHistory> findByIdAndUserEmail(Long id, String userEmail);

    @Modifying
    @Query("update PlanHistory h set h.folderId = null where h.folderId = :folderId")
    void clearFolder(@Param("folderId") Long folderId);
}
