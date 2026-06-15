package ru.itmo.ordermanagement.integration.dolibarr;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.time.Duration;

@ConfigurationProperties(prefix = "app.dolibarr")
@Data
public class DolibarrProperties {
    private String baseUrl;
    private String apiKey;
    private String login;
    private String password;
    private BigDecimal defaultVatRate = BigDecimal.ZERO;
    private Duration connectTimeout = Duration.ofSeconds(5);
    private Duration readTimeout = Duration.ofSeconds(20);
    private boolean validateOnCreate = true;
}
