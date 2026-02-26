package com.loopers.infrastructure.product;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Profile("test")
@RequiredArgsConstructor
public class ProductFullTextIndexInitializer {

    private final EntityManager entityManager;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initFullTextIndex() {
        try {
            entityManager.createNativeQuery(
                "ALTER TABLE products ADD FULLTEXT INDEX idx_products_name_fulltext (name) WITH PARSER ngram"
            ).executeUpdate();
            log.info("FULLTEXT index created successfully on products.name");
        } catch (Exception e) {
            log.debug("FULLTEXT index already exists or creation failed: {}", e.getMessage());
        }
    }
}
