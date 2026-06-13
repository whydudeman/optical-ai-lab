package io.github.whydudeman.opticailab.history;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomReportRepository extends JpaRepository<CustomReport, Long> {

    Optional<CustomReport> findByHistoryId(Long historyId);
}
