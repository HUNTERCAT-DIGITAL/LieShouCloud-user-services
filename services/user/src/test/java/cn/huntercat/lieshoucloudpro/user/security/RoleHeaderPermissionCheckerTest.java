package cn.huntercat.lieshoucloudpro.user.security;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** RoleHeaderPermissionChecker 单测（L2-1 权限注解接入 · user-service）. */
class RoleHeaderPermissionCheckerTest {

  private final RoleHeaderPermissionChecker checker = new RoleHeaderPermissionChecker();

  @AfterEach
  void resetContext() {
    RequestContextHolder.resetRequestAttributes();
  }

  private void withRoles(String rolesHeader) {
    MockHttpServletRequest req = new MockHttpServletRequest();
    if (rolesHeader != null) {
      req.addHeader("X-User-Roles", rolesHeader);
    }
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));
  }

  @Test
  void platformAdmin_hasTenantManage() {
    withRoles("PLATFORM_ADMIN");
    assertThat(checker.hasPermission("tenant:manage")).isTrue();
  }

  @Test
  void tenantAdmin_noTenantManage_hasUserManage() {
    withRoles("TENANT_ADMIN");
    assertThat(checker.hasPermission("tenant:manage")).isFalse();
    assertThat(checker.hasPermission("user:manage")).isTrue();
  }

  @Test
  void plainUser_denied() {
    withRoles("USER");
    assertThat(checker.hasPermission("tenant:manage")).isFalse();
    assertThat(checker.hasPermission("user:manage")).isFalse();
  }

  @Test
  void multiRoleHeader_parsed() {
    withRoles("TENANT_ADMIN, DUTY_OFFICER");
    assertThat(checker.hasPermission("user:manage")).isTrue();
  }

  @Test
  void missingHeader_denied() {
    withRoles(null);
    assertThat(checker.hasPermission("user:manage")).isFalse();
  }

  @Test
  void unknownPermissionCode_denied() {
    withRoles("PLATFORM_ADMIN");
    assertThat(checker.hasPermission("legal:use")).isFalse();
  }
}
