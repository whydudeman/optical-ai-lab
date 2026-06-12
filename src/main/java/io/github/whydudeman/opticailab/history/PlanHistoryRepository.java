package io.github.whydudeman.opticailab.history;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlanHistoryRepository extends JpaRepository<PlanHistory, Long> {

    List<PlanHistory> findByUserEmailOrderByCreatedAtDesc(String userEmail);

    Optional<PlanHistory> findByIdAndUserEmail(Long id, String userEmail);
}
