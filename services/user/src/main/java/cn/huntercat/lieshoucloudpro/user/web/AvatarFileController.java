package cn.huntercat.lieshoucloudpro.user.web;

import cn.huntercat.lieshoucloudpro.user.service.AvatarStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户头像静态读取（公开 · UUID 不可枚举 · 7 天缓存）.
 *
 * <p>路由：网关 user-files-route 把 {@code /api/user-files/avatars/**} 转发到本端点。
 */
@RestController
@RequestMapping("/api/user-files")
@Tag(name = "User · Avatar Files", description = "用户头像静态文件")
public class AvatarFileController {

  private final AvatarStorageService avatars;

  public AvatarFileController(AvatarStorageService avatars) {
    this.avatars = avatars;
  }

  @Operation(summary = "读取头像文件（公开静态）")
  @GetMapping("/avatars/{tenantId}/{userId}/{filename}")
  public ResponseEntity<Resource> read(
      @PathVariable String tenantId, @PathVariable String userId, @PathVariable String filename) {
    Optional<Path> file = avatars.resolve(tenantId, userId, filename);
    if (file.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    MediaType mediaType = mediaType(filename);
    return ResponseEntity.ok()
        .contentType(mediaType)
        .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
        .body(new FileSystemResource(file.get()));
  }

  private static MediaType mediaType(String filename) {
    String lower = filename.toLowerCase();
    if (lower.endsWith(".png")) return MediaType.IMAGE_PNG;
    if (lower.endsWith(".gif")) return MediaType.IMAGE_GIF;
    if (lower.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
    if (lower.endsWith(".bmp")) return MediaType.parseMediaType("image/bmp");
    return MediaType.IMAGE_JPEG;
  }
}
