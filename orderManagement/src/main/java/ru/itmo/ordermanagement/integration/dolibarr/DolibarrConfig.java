package ru.itmo.ordermanagement.integration.dolibarr;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DolibarrProperties.class)
public class DolibarrConfig {

    @Bean
    public DolibarrConnectionFactory dolibarrConnectionFactory(DolibarrProperties properties) {
        DolibarrManagedConnectionFactory managedConnectionFactory = new DolibarrManagedConnectionFactory(properties);
        return new DolibarrConnectionFactoryImpl(managedConnectionFactory, new DolibarrLocalConnectionManager());
    }
}
