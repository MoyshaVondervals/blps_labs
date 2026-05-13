package org.moysha.createorderservice.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.transaction.ChainedKafkaTransactionManager;
import org.springframework.kafka.transaction.KafkaAwareTransactionManager;
import org.springframework.kafka.transaction.KafkaTransactionManager;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DelegatingTransactionDefinition;

@Configuration
public class KafkaTransactionConfig {

    @Bean(name = {"jpaTransactionManager", "transactionManager"})
    public PlatformTransactionManager jpaTransactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    @Bean
    public KafkaTransactionManager<String, Object> kafkaTransactionManager(
            ProducerFactory<String, Object> producerFactory) {
        return new KafkaTransactionManager<>(producerFactory);
    }

    @Bean(name = "kafkaIsolationIgnoringTransactionManager")
    public PlatformTransactionManager kafkaIsolationIgnoringTransactionManager(
            KafkaTransactionManager<String, Object> kafkaTransactionManager) {
        return new IsolationIgnoringTransactionManager(kafkaTransactionManager);
    }

    @Bean(name = "chainedTransactionManager")
    @Primary
    public ChainedKafkaTransactionManager<String, Object> chainedTransactionManager(
            @Qualifier("jpaTransactionManager") PlatformTransactionManager jpaTransactionManager,
            @Qualifier("kafkaIsolationIgnoringTransactionManager") PlatformTransactionManager kafkaTransactionManager) {
        return new ChainedKafkaTransactionManager<>(kafkaTransactionManager, jpaTransactionManager);
    }

    private static class IsolationIgnoringTransactionManager implements KafkaAwareTransactionManager<String, Object> {
        private final KafkaTransactionManager<String, Object> delegate;

        private IsolationIgnoringTransactionManager(KafkaTransactionManager<String, Object> delegate) {
            this.delegate = delegate;
        }

        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            return delegate.getTransaction(new DelegatingTransactionDefinition(definition) {
                @Override
                public int getIsolationLevel() {
                    return TransactionDefinition.ISOLATION_DEFAULT;
                }
            });
        }

        @Override
        public void commit(TransactionStatus status) {
            delegate.commit(status);
        }

        @Override
        public void rollback(TransactionStatus status) {
            delegate.rollback(status);
        }

        @Override
        public ProducerFactory<String, Object> getProducerFactory() {
            return delegate.getProducerFactory();
        }
    }
}
