package cn.huntercat.lieshoucloudpro.user.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import cn.huntercat.lieshoucloudpro.common.web.TenantHeaders;
import cn.huntercat.lieshoucloudpro.user.domain.Notification;
import cn.huntercat.lieshoucloudpro.user.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;

/**
 * 站内通知端点（开源版消息通知模块）.
 *
 * <p>完整路径含上下文：{@code /api/notifications/**}（由 gateway 转发，需 JWT 鉴权）。
 *
 * <p>接收者 = 当前登录用户（gateway 注入 {@code X-User-Id} / {@code X-Tenant-Id}）； 发送端点要求 {@code
 * PLATFORM_ADMIN}（X-User-Roles header）。
 */
@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notification", description = "站内通知（租户内用户维度）")
public class NotificationController {

  private final NotificationService service;

  public NotificationController(NotificationService service) {
    this.service = service;
  }

  @Operation(summary = "List my notifications", description = "当前用户通知列表（未读优先，新→旧）")
  @ApiResponse(responseCode = "200", description = "Notifications (newest first)")
  @GetMapping
  public ResponseEntity<?> list(
      @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
      @RequestHeader(value = "X-User-Id", required = false) String userHeader,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    Long tid = TenantHeaders.parseLong(tenantHeader);
    Long uid = TenantHeaders.parseLong(userHeader);
    if (tid == null || uid == null) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(Map.of("error", "MISSING_CONTEXT", "message", "缺少租户/用户上下文"));
    }
    return ResponseEntity.ok(service.list(tid, uid, page, size));
  }

  @Operation(summary = "Unread count", description = "当前用户未读通知数")
  @GetMapping("/unread-count")
  public ResponseEntity<?> unreadCount(
      @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
      @RequestHeader(value = "X-User-Id", required = false) String userHeader) {
    Long tid = TenantHeaders.parseLong(tenantHeader);
    Long uid = TenantHeaders.parseLong(userHeader);
    if (tid == null || uid == null) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(Map.of("error", "MISSING_CONTEXT", "message", "缺少租户/用户上下文"));
    }
    return ResponseEntity.ok(Map.of("unread", service.unreadCount(tid, uid)));
  }

  @Operation(summary = "Mark one read")
  @PostMapping("/{id}/read")
  public ResponseEntity<?> markRead(
      @PathVariable Long id,
      @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
      @RequestHeader(value = "X-User-Id", required = false) String userHeader) {
    Long tid = TenantHeaders.parseLong(tenantHeader);
    Long uid = TenantHeaders.parseLong(userHeader);
    if (tid == null || uid == null) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(Map.of("error", "MISSING_CONTEXT", "message", "缺少租户/用户上下文"));
    }
    boolean ok = service.markRead(id, tid, uid);
    if (!ok) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(Map.of("error", "NOT_FOUND", "message", "通知不存在或已读"));
    }
    return ResponseEntity.ok(Map.of("ok", true));
  }

  @Operation(summary = "Mark all read", description = "返回本次标记条数")
  @PostMapping("/read-all")
  public ResponseEntity<?> markAllRead(
      @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
      @RequestHeader(value = "X-User-Id", required = false) String userHeader) {
    Long tid = TenantHeaders.parseLong(tenantHeader);
    Long uid = TenantHeaders.parseLong(userHeader);
    if (tid == null || uid == null) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(Map.of("error", "MISSING_CONTEXT", "message", "缺少租户/用户上下文"));
    }
    return ResponseEntity.ok(Map.of("updated", service.markAllRead(tid, uid)));
  }

  @Operation(
      summary = "Send notification (platform admin)",
      description = "平台管理/业务事件推送；PLATFORM_ADMIN 权限")
  @PostMapping
  public ResponseEntity<?> send(
      @Valid @RequestBody SendNotificationRequest body,
      @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
      @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader) {
    if (!AuthRoles.hasAny(rolesHeader, AuthRoles.PLATFORM_ADMIN)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(Map.of("error", "FORBIDDEN", "message", "需要平台管理员权限"));
    }
    Long tid = TenantHeaders.parseLong(tenantHeader);
    if (tid == null) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(Map.of("error", "MISSING_CONTEXT", "message", "缺少租户上下文"));
    }
    Notification n =
        service.send(
            tid,
            body.userId(),
            body.type(),
            body.title(),
            body.content(),
            body.bizType(),
            body.bizId());
    return ResponseEntity.status(HttpStatus.CREATED).body(n);
  }

  /** 发送请求体（平台管理）。 */
  public record SendNotificationRequest(
      @NotNull Long userId,
      @NotBlank @Size(max = 200) String title,
      String content,
      String type,
      String bizType,
      Long bizId) {}
}
