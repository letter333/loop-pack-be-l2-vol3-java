package com.loopers.domain.outbox;

import lombok.Getter;

import java.time.ZonedDateTime;

@Getter
public class OutboxEvent {

    private Long id;
    private String aggregateType;
    private String aggregateId;
    private String eventType;
    private String payload;
    private ZonedDateTime createdAt;
    private ZonedDateTime publishedAt;

    public OutboxEvent(String aggregateType, String aggregateId, String eventType, String payload) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.createdAt = ZonedDateTime.now();
    }

    public OutboxEvent(Long id, String aggregateType, String aggregateId, String eventType,
                       String payload, ZonedDateTime createdAt, ZonedDateTime publishedAt) {
        this.id = id;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.createdAt = createdAt;
        this.publishedAt = publishedAt;
    }

    public void markPublished() {
        this.publishedAt = ZonedDateTime.now();
    }

    public boolean isPublished() {
        return publishedAt != null;
    }
}
