package cn.huntercat.lieshoucloudpro.user.web;

import cn.huntercat.lieshou.framework.common.web.TenantHeaders;
import cn.huntercat.lieshou.framework.domain.User;
import cn.huntercat.lieshou.framework.service.UserService;
import cn.huntercat.lieshoucloudpro.user.service.AvatarStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户头像（文件上传 · 2026-09 头像功能）.
 *
 * <p>上传走 JWT + 租户校验；读取 {@code /api/user-files/avatars/**} 为公开静态（&lt;img&gt; 无法带
 * header，文件名 UUID 随机不可枚举，私有部署可接受）。存储本地磁盘 volume，见 {@link
 * AvatarStorageService}。默认头像为前端 data URL（存 avatarUrl 字段），上传图片走本端点。
 */
@RestController
@RequestMapping("/api/users")
@Tag(name = "User · Avatar", description = "用户头像（文件上传/清除/读取）")
public class AvatarController {

  private static final long MAX_AVATAR_BYTES = 5L * 1024 * 1024;

  private final UserService userService;
  private final AvatarStorageService avatars;

  public AvatarController(UserService userService, AvatarStorageService avatars) {
    this.userService = userService;
    this.avatars = avatars;
  }

  @Operation(summary = "上传用户头像（multipart file，≤5MB，image/*）→ 更新 avatarUrl 并返回")
  @PostMapping("/{id}/avatar")
  public ResponseEntity<?> upload(
      @PathVariable Long id,
      @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
      @RequestParam("file") MultipartFile file) {
    Long tid = TenantHeaders.parseLong(tenantHeader);
    if (file == null || file.isEmpty()) {
      return ResponseEntity.badRequest().body(Map.of("error", "NO_FILE"));
    }
    if (file.getSize() > MAX_AVATAR_BYTES) {
      return ResponseEntity.badRequest().body(Map.of("error", "AVATAR_TOO_LARGE"));
    }
    String ct = file.getContentType();
    if (ct == null || !ct.startsWith("image/")) {
      return ResponseEntity.badRequest().body(Map.of("error", "INVALID_AVATAR_TYPE"));
    }
    try {
      String url = avatars.store(tid, id, file);
      User saved = userService.setAvatarUrl(id, tid, url);
      return ResponseEntity.ok(Map.of("avatarUrl", saved.getAvatarUrl()));
    } catch (IOException e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "AVATAR_STORE_FAILED"));
    }
  }

  @Operation(summary = "清除用户头像（avatarUrl 置空 + 删文件）")
  @DeleteMapping("/{id}/avatar")
  public ResponseEntity<?> remove(
      @PathVariable Long id,
      @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader) {
    Long tid = TenantHeaders.parseLong(tenantHeader);
    User u = userService.get(id, tid);
    avatars.deleteByUrl(u.getAvatarUrl());
    User saved = userService.setAvatarUrl(id, tid, null);
    return ResponseEntity.ok(Map.of("avatarUrl", saved.getAvatarUrl() == null ? "" : saved.getAvatarUrl()));
  }

  @Operation(summary = "直接设置头像 URL（默认头像 data URL / 内置标识 · 2026-09）")
  @org.springframework.web.bind.annotation.PatchMapping("/{id}/avatar-url")
  public ResponseEntity<?> setAvatarUrl(
      @PathVariable Long id,
      @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
      @RequestBody Map<String, String> body) {
    Long tid = TenantHeaders.parseLong(tenantHeader);
    String url = body == null ? null : body.get("avatarUrl");
    if (url == null || url.isBlank()) {
      return ResponseEntity.badRequest().body(Map.of("error", "AVATAR_URL_REQUIRED"));
    }
    if (url.length() > 2048) {
      return ResponseEntity.badRequest().body(Map.of("error", "AVATAR_URL_TOO_LONG"));
    }
    User saved = userService.setAvatarUrl(id, tid, url);
    return ResponseEntity.ok(Map.of("avatarUrl", saved.getAvatarUrl()));
  }


}
