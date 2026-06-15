package ru.itmo.ordermanagement.config;

import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.AuthorizationService;
import org.camunda.bpm.engine.authorization.Authorization;
import org.camunda.bpm.engine.authorization.Permission;
import org.camunda.bpm.engine.authorization.Permissions;
import org.camunda.bpm.engine.authorization.Resource;
import org.camunda.bpm.engine.authorization.Resources;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * Раздаёт права группам Camunda после включения авторизации.
 * <p>
 * ADMIN — полный доступ ко всему. SELLER/COURIER/CUSTOMER получают доступ к
 * Tasklist и чтение определения процесса; видимость и выполнение конкретных
 * задач разграничиваются по candidateGroups через {@link CandidateTaskAuthorizationPlugin}.
 */
@Configuration
@RequiredArgsConstructor
public class CamundaAuthorizationConfig {

    private static final String ADMIN_GROUP = "ADMIN";
    private static final String[] WORKER_GROUPS = {"SELLER", "COURIER", "CUSTOMER"};
    private static final String PROCESS_KEY = "orderProcess";

    @Bean
    @Order(2)
    public CommandLineRunner camundaAuthorizationInitializer(AuthorizationService authorizationService) {
        return args -> {
            // ADMIN-группа: всё на всех ресурсах
            for (Resources resource : Resources.values()) {
                grant(authorizationService, ADMIN_GROUP, resource, "*", Permissions.ALL);
            }

            // Рабочие группы: доступ к веб-приложению Tasklist и фильтрам
            for (String group : WORKER_GROUPS) {
                grant(authorizationService, group, Resources.APPLICATION, "tasklist", Permissions.ACCESS);
                grant(authorizationService, group, Resources.FILTER, "*", Permissions.READ);
            }

            // SELLER/COURIER: чтение определения процесса (нужно Tasklist для форм и метаданных)
            for (String group : new String[]{"SELLER", "COURIER"}) {
                grant(authorizationService, group, Resources.PROCESS_DEFINITION, PROCESS_KEY,
                        Permissions.READ, Permissions.READ_INSTANCE, Permissions.READ_HISTORY);
            }
        };
    }

    private void grant(AuthorizationService authorizationService, String groupId,
                       Resource resource, String resourceId, Permission... permissions) {
        Authorization authorization = authorizationService.createAuthorizationQuery()
                .authorizationType(Authorization.AUTH_TYPE_GRANT)
                .groupIdIn(groupId)
                .resourceType(resource)
                .resourceId(resourceId)
                .singleResult();

        if (authorization == null) {
            authorization = authorizationService.createNewAuthorization(Authorization.AUTH_TYPE_GRANT);
            authorization.setGroupId(groupId);
            authorization.setResource(resource);
            authorization.setResourceId(resourceId);
        }

        for (Permission permission : permissions) {
            authorization.addPermission(permission);
        }
        authorizationService.saveAuthorization(authorization);
    }
}
