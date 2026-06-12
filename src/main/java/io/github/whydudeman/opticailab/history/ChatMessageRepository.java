package io.github.whydudeman.opticailab.history;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByHistoryIdOrderByCreatedAtAsc(Long historyId);
}
