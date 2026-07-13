package com.example.billingsimulator.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "knowledge_article",
       uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "kind", "key_code"}))
public class KnowledgeArticle {

    @Id
    @Column(name = "article_id")
    private UUID articleId;

    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "kind", nullable = false, length = 20)
    private String kind;

    @Column(name = "key_code", nullable = false, length = 60)
    private String keyCode;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    // Stored as string for pgvector — cast to vector(768) in native queries
    @Column(name = "embedding", nullable = false, columnDefinition = "vector(768)")
    private String embedding;

    @Column(name = "business_reason", columnDefinition = "TEXT")
    private String businessReason;

    @Column(name = "source_doc", length = 120)
    private String sourceDoc;

    @Column(name = "token_count")
    private Integer tokenCount;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (articleId == null) articleId = UUID.randomUUID();
        if (createdAt == null) createdAt = OffsetDateTime.now();
        if (updatedAt == null) updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    // Getters and Setters

    public UUID getArticleId() { return articleId; }
    public void setArticleId(UUID articleId) { this.articleId = articleId; }

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }

    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }

    public String getKeyCode() { return keyCode; }
    public void setKeyCode(String keyCode) { this.keyCode = keyCode; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getEmbedding() { return embedding; }
    public void setEmbedding(String embedding) { this.embedding = embedding; }

    public String getBusinessReason() { return businessReason; }
    public void setBusinessReason(String businessReason) { this.businessReason = businessReason; }

    public String getSourceDoc() { return sourceDoc; }
    public void setSourceDoc(String sourceDoc) { this.sourceDoc = sourceDoc; }

    public Integer getTokenCount() { return tokenCount; }
    public void setTokenCount(Integer tokenCount) { this.tokenCount = tokenCount; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
