package io.github.whydudeman.opticailab.history;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LabJournalRepository extends JpaRepository<LabJournal, Long> {

    Optional<LabJournal> findByHistoryId(Long historyId);
}
