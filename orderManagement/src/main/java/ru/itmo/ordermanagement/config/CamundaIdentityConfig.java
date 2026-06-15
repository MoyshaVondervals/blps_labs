package ru.itmo.ordermanagement.config;

import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.identity.Group;
import org.camunda.bpm.engine.identity.User;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
@RequiredArgsConstructor
public class CamundaIdentityConfig {

    private static final String DEFAULT_PASSWORD = "2281337";

    @Bean
    @Order(1)
    public CommandLineRunner camundaIdentityInitializer(IdentityService identityService) {
        return args -> {
            ensureGroup(identityService, "ADMIN", "Admin");
            ensureGroup(identityService, "CUSTOMER", "Customer");
            ensureGroup(identityService, "SELLER", "Seller");
            ensureGroup(identityService, "COURIER", "Courier");

            ensureUser(identityService, "moysha", "Admin", "User", "ADMIN");
            ensureUser(identityService, "customer", "Customer", "User", "CUSTOMER");
            ensureUser(identityService, "seller", "Seller", "User", "SELLER");
            ensureUser(identityService, "courier", "Courier", "User", "COURIER");
        };
    }

    private void ensureGroup(IdentityService identityService, String id, String name) {
        if (identityService.createGroupQuery().groupId(id).singleResult() != null) {
            return;
        }
        Group group = identityService.newGroup(id);
        group.setName(name);
        group.setType("WORKFLOW");
        identityService.saveGroup(group);
    }

    private void ensureUser(IdentityService identityService, String id, String firstName,
                            String lastName, String groupId) {
        if (identityService.createUserQuery().userId(id).singleResult() == null) {
            User user = identityService.newUser(id);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setPassword(DEFAULT_PASSWORD);
            identityService.saveUser(user);
        }

        long membershipCount = identityService.createGroupQuery()
                .groupId(groupId)
                .groupMember(id)
                .count();
        if (membershipCount == 0) {
            identityService.createMembership(id, groupId);
        }
    }
}
