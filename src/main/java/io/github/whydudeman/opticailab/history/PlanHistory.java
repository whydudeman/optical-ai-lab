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

    private Long folderId;

    @Column(nullable = false, length = 4000)
    private String topic;

    private String language;

    private String provider;

    @Lob
    private String equipment;

    @Lob
    @Column(nullable = false)
    private String planJson;

    @Lob
    private String reportJson;

    private Instant completedAt;

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

    public String getEquipment() {
        return equipment;
    }

    public void setEquipment(String equipment) {
        this.equipment = equipment;
    }

    public String getPlanJson() {
        return planJson;
    }

    public void setPlanJson(String planJson) {
        this.planJson = planJson;
    }

    public String getReportJson() {
        return reportJson;
    }

    public void setReportJson(String reportJson) {
        this.reportJson = reportJson;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Long getFolderId() {
        return folderId;
    }

    public void setFolderId(Long folderId) {
        this.folderId = folderId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
