package ru.itmo.ordermanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider;
import org.springframework.security.authentication.jaas.AbstractJaasAuthenticationProvider;
import org.springframework.security.authentication.jaas.AuthorityGranter;
import org.springframework.security.authentication.jaas.DefaultJaasAuthenticationProvider;
import org.springframework.security.authentication.jaas.memory.InMemoryConfiguration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import ru.itmo.ordermanagement.security.Role;
import ru.itmo.ordermanagement.security.RolePrincipal;

import javax.security.auth.login.AppConfigurationEntry;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authz -> authz
                        .anyRequest()
                        .authenticated())
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AbstractJaasAuthenticationProvider jaasAuthenticationProvider) {
        return new ProviderManager(jaasAuthenticationProvider);
    }

    @Bean
    public AbstractJaasAuthenticationProvider jaasAuthenticationProvider() {
        DefaultJaasAuthenticationProvider provider = new DefaultJaasAuthenticationProvider();

        provider.setLoginContextName("SPRINGSECURITY");

        provider.setAuthorityGranters(new AuthorityGranter[]{
                principal -> {
                    if (principal instanceof RolePrincipal) {
                        String roleName = principal.getName();
                        Set<String> authorities = new HashSet<>();

                        authorities.add(roleName);

                        try {
                            String enumName = roleName.replaceFirst("^ROLE_", "");
                            Role role = Role.valueOf(enumName);

                            authorities.addAll(role.getPrivileges());
                        } catch (IllegalArgumentException e) {
                            System.err.println("Неизвестная роль в XML: " + roleName);
                        }

                        return authorities;
                    }
                    return null;
                }
        });

        AppConfigurationEntry entry = new AppConfigurationEntry(
                "ru.itmo.ordermanagement.security.XmlLoginModule",
                AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                new HashMap<>()
        );

        InMemoryConfiguration config = new InMemoryConfiguration(
                Collections.singletonMap(
                        "SPRINGSECURITY",
                        new AppConfigurationEntry[]{entry}
                )
        );

        provider.setConfiguration(config);

        return provider;
    }
}