package cn.huntercat.lieshoucloudpro.user.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import cn.huntercat.lieshou.framework.common.web.TenantHeaders;
import cn.huntercat.lieshou.framework.domain.TenantInvite;
import cn.huntercat.lieshou.framework.service.TenantInviteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;

/**
 * 邀请码端点（ADR-0023 Phase 2 · 薄壳装配）.
 *
 * <p>完整路径：{@code /api/tenants/{tenantId}/invites/**}（由 gateway 转发，需 JWT——平台运营操作）。
 * 授权检查（PLATFORM_ADMIN / 本租户 TENANT_ADMIN）保留在 HTTP 层；业务（唯一码生成 /
 * 租户校验 / role 白名单 / revoke）在 framework-service {@link TenantInviteService}。
 */
@RestController
@RequestMapping("/api/tenants/{tenantId}/invites")
@Tag(name = "TenantInvite", description = "Invite codes for tenant self-registration")
public class TenantInviteController {

  private final TenantInviteService inviteService;

  public TenantInviteController(TenantInviteService inviteService) {
    this.inviteService = inviteService;
  }

  /** RBAC（ADR-0024）：平台管理员 或 本租户管理员；否则 403 */
  private boolean authorized(String rolesHeader, String tenantHeader, Long tenantId) {
    if (AuthRoles.hasAny(rolesHeader, AuthRoles.PLATFORM_ADMIN)) return true;
    Long headerTenant = TenantHeaders.parseLong(tenantHeader);
    return AuthRoles.hasAny(rolesHeader, AuthRoles.TENANT_ADMIN)
        && headerTenant != null
        && headerTenant.equals(tenantId);
  }

  private ResponseEntity<Object> forbidden() {
    return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
        .body(
            Map.of(
                "error", "FORBIDDEN", "message", "requires PLATFORM_ADMIN or tenant TENANT_ADMIN"));
  }

  @Operation(summary = "Generate invite code for tenant")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Invite created with code"),
    @ApiResponse(responseCode = "404", description = "Tenant not found")
  })
  @PostMapping
  public ResponseEntity<?> create(
      @Parameter(description = "Tenant id", example = "1") @PathVariable Long tenantId,
      @Valid @RequestBody CreateInviteRequest body,
      @org.springframework.web.bind.annotation.RequestHeader(
              value = "X-User-Roles",
              required = false)
          String rolesHeader,
      @org.springframework.web.bind.annotation.RequestHeader(
              value = "X-Tenant-Id",
              required = false)
          String tenantHeader) {
    if (!authorized(rolesHeader, tenantHeader, tenantId)) {
      return forbidden();
    }
    TenantInvite invite =
        inviteService.create(tenantId, body.role(), body.expiresInDays(), body.maxUses());
    return ResponseEntity.ok(invite);
  }

  @Operation(summary = "List invites of tenant")
  @ApiResponse(responseCode = "200", description = "List of invites (newest first)")
  @GetMapping
  public ResponseEntity<?> list(
      @Parameter(description = "Tenant id", example = "1") @PathVariable Long tenantId,
      @org.springframework.web.bind.annotation.RequestHeader(
              value = "X-User-Roles",
              required = false)
          String rolesHeader,
      @org.springframework.web.bind.annotation.RequestHeader(
              value = "X-Tenant-Id",
              required = false)
          String tenantHeader) {
    if (!authorized(rolesHeader, tenantHeader, tenantId)) {
      return forbidden();
    }
    List<TenantInvite> invites = inviteService.list(tenantId);
    return ResponseEntity.ok(invites);
  }

  @Operation(summary = "Revoke invite code")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Revoked"),
    @ApiResponse(responseCode = "404", description = "Invite not found / belongs to other tenant")
  })
  @PostMapping("/{id}/revoke")
  public ResponseEntity<?> revoke(
      @Parameter(description = "Tenant id", example = "1") @PathVariable Long tenantId,
      @Parameter(description = "Invite id", example = "1") @PathVariable Long id,
      @org.springframework.web.bind.annotation.RequestHeader(
              value = "X-User-Roles",
              required = false)
          String rolesHeader,
      @org.springframework.web.bind.annotation.RequestHeader(
              value = "X-Tenant-Id",
              required = false)
          String tenantHeader) {
    if (!authorized(rolesHeader, tenantHeader, tenantId)) {
      return forbidden();
    }
    inviteService.revoke(tenantId, id);
    return ResponseEntity.noContent().build();
  }

  /** Phase 8: CreateInviteRequest DTO（内联；全部可选） */
  public record CreateInviteRequest(String role, Integer expiresInDays, Integer maxUses) {}
}
