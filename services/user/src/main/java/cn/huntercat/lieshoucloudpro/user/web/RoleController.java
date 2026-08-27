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
import cn.huntercat.lieshou.framework.domain.RoleRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import java.util.Optional;

/**
 * 角色管理端点（RBAC · ADR-0024）.
 *
 * <p>读：PLATFORM_ADMIN 或 TENANT_ADMIN（租户内管理员需要角色选项）；写：PLATFORM_ADMIN。
 *
 * <p>鉴权与审计注解化（L2-1 接入）：权限码由 {@code RoleHeaderPermissionChecker} （X-User-Roles header）校验，写操作经
 * {@code @Audited} 走 AuditLog 落库 （AuditService 实现 common AuditRecorder SPI）。
 */
@RestController
@RequestMapping("/api/roles")
@Tag(name = "Role", description = "Role definitions (RBAC)")
public class RoleController {

  private final RoleRepository repo;

  public RoleController(RoleRepository repo) {
    this.repo = repo;
  }

  @Operation(summary = "List roles", description = "PLATFORM_ADMIN or TENANT_ADMIN can read.")
  @GetMapping
  @RequiresPermission("user:manage")
  public ResponseEntity<?> list() {
    return ResponseEntity.ok(repo.findByOrderByScopeAscIdAsc());
  }

  @Operation(summary = "Create custom role", description = "PLATFORM_ADMIN only.")
  @PostMapping
  @RequiresPermission("tenant:manage")
  @Audited(action = "CREATE", resource = "role")
  public ResponseEntity<?> create(@Valid @RequestBody CreateRoleRequest body) {
    if (repo.findByCode(body.code()).isPresent()) {
      return ResponseEntity.badRequest().body(Map.of("error", "ROLE_CODE_TAKEN"));
    }
    Role.Scope scopeRole =
        body.scope() == null ? Role.Scope.TENANT : Role.Scope.valueOf(body.scope());
    Role saved =
        repo.save(new Role(body.code(), body.name(), scopeRole, body.description(), false));
    return ResponseEntity.ok(saved);
  }

  @Operation(
      summary = "Update role (name/description/scope)",
      description = "PLATFORM_ADMIN only; code immutable; system roles read-only.")
  @PutMapping("/{id}")
  @RequiresPermission("tenant:manage")
  @Audited(action = "UPDATE", resource = "role")
  public ResponseEntity<?> update(
      @PathVariable Long id, @Valid @RequestBody UpdateRoleRequest body) {
    return repo.findById(id)
        .map(
            r -> {
              if (r.isSystem()) {
                return ResponseEntity.badRequest().body(Map.of("error", "SYSTEM_ROLE_READONLY"));
              }
              if (body.name() != null && !body.name().isBlank()) r.setName(body.name());
              if (body.description() != null) r.setDescription(body.description());
              if (body.scope() != null && !body.scope().isBlank()) {
                r.setScope(Role.Scope.valueOf(body.scope()));
              }
              return ResponseEntity.ok(repo.save(r));
            })
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @Operation(
      summary = "Delete custom role",
      description = "PLATFORM_ADMIN only; system roles cannot be deleted.")
  @DeleteMapping("/{id}")
  @RequiresPermission("tenant:manage")
  @Audited(action = "DELETE", resource = "role")
  public ResponseEntity<?> delete(@PathVariable Long id) {
    Optional<Role> opt = repo.findById(id);
    if (opt.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    if (opt.get().isSystem()) {
      return ResponseEntity.badRequest().body(Map.of("error", "SYSTEM_ROLE_READONLY"));
    }
    repo.delete(opt.get());
    return ResponseEntity.noContent().build();
  }

  public record CreateRoleRequest(
      @NotBlank String code, @NotBlank String name, String scope, String description) {}

  public record UpdateRoleRequest(String name, String scope, String description) {}
}
