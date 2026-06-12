package io.github.whydudeman.opticailab.history;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long historyId;

    @Column(nullable = false)
    private String role;

    @Lob
    @Column(nullable = false)
    private String text;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected ChatMessage() {
    }

    public ChatMessage(Long historyId, String role, String text) {
        this.historyId = historyId;
        this.role = role;
        this.text = text;
    }

    public Long getId() {
        return id;
    }

    public Long getHistoryId() {
        return historyId;
    }

    public String getRole() {
        return role;
    }

    public String getText() {
        return text;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
