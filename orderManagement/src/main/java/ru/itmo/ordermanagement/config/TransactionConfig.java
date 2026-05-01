package ru.itmo.ordermanagement.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.transaction.jta.JtaTransactionManager;

@Configuration
public class TransactionConfig {

    @Bean
    public BeanPostProcessor jtaTransactionManagerCustomizer() {
        return new BeanPostProcessor() {
            @Override
            public @NonNull Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName)
                    throws BeansException {
                if (bean instanceof JtaTransactionManager jtaTransactionManager) {
                    jtaTransactionManager.setAllowCustomIsolationLevels(true);
                }
                return bean;
            }
        };
    }
}
