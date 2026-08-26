package cn.huntercat.lieshoucloudpro.user.security;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

import cn.huntercat.lieshoucloudpro.common.security.PermissionChecker;

/**
 * 权限校验 SPI 实现（L2-1 注解接入 · user-service）.
 *
 * <p>从 gateway 透传的 {@code X-User-Roles} header（来自 JWT roles claim）解析角色， 映射为权限码（与前端 access.ts
 * 角色推导语义对齐，ADR-0024 权限码驱动）：
 *
 * <ul>
 *   <li>{@code tenant:manage}（平台级：租户/角色管理）→ PLATFORM_ADMIN
 *   <li>{@code user:manage}（租户内用户管理）→ PLATFORM_ADMIN 或 TENANT_ADMIN
 * </ul>
 *
 * <p>未识别的权限码 → 拒绝（默认最小权限）。header 缺失 → 拒绝。
 */
@Component
public class RoleHeaderPermissionChecker implements PermissionChecker {

  static final String HDR_USER_ROLES = "X-User-Roles";
  private static final String PLATFORM_ADMIN = "PLATFORM_ADMIN";
  private static final String TENANT_ADMIN = "TENANT_ADMIN";

  @Override
  public boolean hasPermission(String permissionCode) {
    String roles = currentRolesHeader();
    if (roles == null || roles.isBlank()) {
      return false;
    }
    return switch (permissionCode) {
      case "tenant:manage" -> hasRole(roles, PLATFORM_ADMIN);
      case "user:manage" -> hasRole(roles, PLATFORM_ADMIN, TENANT_ADMIN);
      default -> false;
    };
  }

  private static String currentRolesHeader() {
    ServletRequestAttributes attrs =
        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if (attrs == null) {
      return null;
    }
    HttpServletRequest req = attrs.getRequest();
    return req.getHeader(HDR_USER_ROLES);
  }

  private static boolean hasRole(String rolesHeader, String... required) {
    for (String r : rolesHeader.split(",")) {
      String role = r.trim();
      for (String req : required) {
        if (role.equals(req)) {
          return true;
        }
      }
    }
    return false;
  }
}
