package cn.huntercat.lieshoucloudpro.user.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import cn.huntercat.lieshou.framework.common.audit.Audited;
import cn.huntercat.lieshou.framework.common.security.RequiresPermission;
import cn.huntercat.lieshou.framework.domain.Role;
import cn.huntercat.lieshou.framework.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 角色管理端点（RBAC · ADR-0024，薄壳装配）.
 *
 * <p>读：PLATFORM_ADMIN 或 TENANT_ADMIN（租户内管理员需要角色选项）；写：PLATFORM_ADMIN。 业务（查重 / system 保护 / scope 解析）在
 * framework-service {@link RoleService}； 鉴权与审计注解化（{@code @RequiresPermission} +
 * {@code @Audited}）保留在 HTTP 层。
 */
@RestController
@RequestMapping("/api/roles")
@Tag(name = "Role", description = "Role definitions (RBAC)")
public class RoleController {

  private final RoleService roleService;

  public RoleController(RoleService roleService) {
    this.roleService = roleService;
  }

  @Operation(summary = "List roles", description = "PLATFORM_ADMIN or TENANT_ADMIN can read.")
  @GetMapping
  @RequiresPermission("user:manage")
  public ResponseEntity<?> list() {
    return ResponseEntity.ok(roleService.list());
  }

  @Operation(summary = "Create custom role", description = "PLATFORM_ADMIN only.")
  @PostMapping
  @RequiresPermission("tenant:manage")
  @Audited(action = "CREATE", resource = "role")
  public ResponseEntity<?> create(@Valid @RequestBody CreateRoleRequest body) {
    return ResponseEntity.ok(
        roleService.create(body.code(), body.name(), parseScope(body.scope()), body.description()));
  }

  @Operation(
      summary = "Update role (name/description/scope)",
      description = "PLATFORM_ADMIN only; code immutable; system roles read-only.")
  @PutMapping("/{id}")
  @RequiresPermission("tenant:manage")
  @Audited(action = "UPDATE", resource = "role")
  public ResponseEntity<?> update(
      @PathVariable Long id, @Valid @RequestBody UpdateRoleRequest body) {
    return ResponseEntity.ok(roleService.update(id, body.name(), body.scope(), body.description()));
  }

  @Operation(
      summary = "Delete custom role",
      description = "PLATFORM_ADMIN only; system roles cannot be deleted.")
  @DeleteMapping("/{id}")
  @RequiresPermission("tenant:manage")
  @Audited(action = "DELETE", resource = "role")
  public ResponseEntity<?> delete(@PathVariable Long id) {
    roleService.delete(id);
    return ResponseEntity.noContent().build();
  }

  private static Role.Scope parseScope(String scope) {
    if (scope == null || scope.isBlank()) {
      return null;
    }
    return Role.Scope.valueOf(scope);
  }

  public record CreateRoleRequest(
      @NotBlank String code, @NotBlank String name, String scope, String description) {}

  public record UpdateRoleRequest(String name, String scope, String description) {}
}
