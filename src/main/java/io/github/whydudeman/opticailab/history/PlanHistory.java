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
@Table(name = "plan_history")
public class PlanHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userEmail;

    @Column(nullable = false)
    private String topic;

    private String language;

    private String provider;

    @Lob
    @Column(nullable = false)
    private String planJson;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected PlanHistory() {
    }

    public PlanHistory(String userEmail, String topic, String language, String provider, String planJson) {
        this.userEmail = userEmail;
        this.topic = topic;
        this.language = language;
        this.provider = provider;
        this.planJson = planJson;
    }

    public Long getId() {
        return id;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public String getTopic() {
        return topic;
    }

    public String getLanguage() {
        return language;
    }

    public String getProvider() {
        return provider;
    }

    public String getPlanJson() {
        return planJson;
    }

    public void setPlanJson(String planJson) {
        this.planJson = planJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
