package com.gcerp.erp.auth;

import com.gcerp.erp.audit.OperationLogService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AuthServicePermissionTest {

    @Test
    void businessRolesCanEnterNewOrderFlowApi() {
        AuthService service = new AuthService(mock(JdbcTemplate.class), mock(OperationLogService.class));

        for (String role : new String[]{"SERVICE", "ENGINEER", "FINANCE", "DIRECTOR"}) {
            CurrentUser user = new CurrentUser();
            user.setRoleCode(role);
            assertThat(service.hasApiPermission(user, "GET", "/api/order-flow/work-queues")).isTrue();
        }
    }

    @Test
    void productionRoleDoesNotReceivePreFinanceOrderFlowAccess() {
        AuthService service = new AuthService(mock(JdbcTemplate.class), mock(OperationLogService.class));
        CurrentUser user = new CurrentUser();
        user.setRoleCode("PRODUCTION");

        assertThat(service.hasApiPermission(user, "GET", "/api/order-flow/work-queues")).isFalse();
    }
}
