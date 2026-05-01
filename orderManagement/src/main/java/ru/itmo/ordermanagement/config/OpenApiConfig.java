package ru.itmo.ordermanagement.config;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenApiCustomizer customisePageable() {
        return openApi -> {
            if (openApi.getPaths() == null) return;

            openApi.getPaths().values().forEach(pathItem ->
                    pathItem.readOperations().forEach(operation -> {
                        if (operation.getParameters() != null) {
                            operation.getParameters()
                                    .removeIf(p -> "sort".equals(p.getName()));
                        }
                    })
            );
        };
    }
}
