package org.moysha.createorderservice.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.transaction.ChainedKafkaTransactionManager;
import org.springframework.kafka.transaction.KafkaTransactionManager;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class KafkaTransactionConfig {

    @Bean(name = "jpaTransactionManager")
    public PlatformTransactionManager jpaTransactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    @Bean
    public KafkaTransactionManager<String, Object> kafkaTransactionManager(
            ProducerFactory<String, Object> producerFactory) {
        return new KafkaTransactionManager<>(producerFactory);
    }

    @Bean(name = "chainedTransactionManager")
    @Primary
    public ChainedKafkaTransactionManager<String, Object> chainedTransactionManager(
            @Qualifier("jpaTransactionManager") PlatformTransactionManager jpaTransactionManager,
            KafkaTransactionManager<String, Object> kafkaTransactionManager) {
        return new ChainedKafkaTransactionManager<>(kafkaTransactionManager, jpaTransactionManager);
    }
}
