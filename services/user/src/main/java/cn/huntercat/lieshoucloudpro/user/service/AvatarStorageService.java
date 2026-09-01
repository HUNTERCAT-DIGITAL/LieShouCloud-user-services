package cn.huntercat.lieshoucloudpro.user.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

/**
 * 用户头像存储（本地磁盘 + volume 挂载，对齐 iot PhotoStorageService 模式）.
 *
 * <p>目录结构 {@code {avatar-dir}/{tenantId}/{userId}/{uuid}.{ext}}，访问路径 {@code
 * /api/user-files/avatars/{tenantId}/{userId}/{filename}}（经网关 user-files-route 转发，文件名 UUID
 * 随机不可枚举）。路径穿越防护：解析后必须仍在根目录内。
 */
@Service
public class AvatarStorageService {

  private final Path root;

  public AvatarStorageService(@Value("${user.avatar-dir:/data/avatars}") String dir) {
    this.root = Paths.get(dir).toAbsolutePath().normalize();
  }

  /** 保存头像文件 → 返回相对访问路径（/api/user-files/avatars/...） */
  public String store(Long tenantId, Long userId, MultipartFile file) throws IOException {
    String filename = UUID.randomUUID().toString().replace("-", "") + guessExtension(file);
    Path dir = root.resolve(tenantId.toString()).resolve(userId.toString());
    Files.createDirectories(dir);
    Path target = dir.resolve(filename).normalize();
    if (!target.startsWith(root)) {
      throw new IOException("invalid avatar path");
    }
    file.transferTo(target);
    return "/api/user-files/avatars/" + tenantId + "/" + userId + "/" + filename;
  }

  /** 按访问路径删除头像文件（容错：不存在/非法路径静默） */
  public void deleteByUrl(String avatarUrl) {
    if (avatarUrl == null || !avatarUrl.startsWith("/api/user-files/avatars/")) {
      return;
    }
    try {
      // /api/user-files/avatars/{tenantId}/{userId}/{filename}
      String[] parts = avatarUrl.split("/");
      if (parts.length < 6) return;
      Path target = root.resolve(parts[4]).resolve(parts[5]).resolve(parts[6]).normalize();
      if (target.startsWith(root)) Files.deleteIfExists(target);
    } catch (IOException ignored) {
      // 删除失败静默（不影响主流程）
    }
  }

  /** 按访问路径解析文件（静态读取用） */
  public Optional<Path> resolve(String tenantId, String userId, String filename) {
    Path target = root.resolve(tenantId).resolve(userId).resolve(filename).normalize();
    if (!target.startsWith(root) || !Files.exists(target)) {
      return Optional.empty();
    }
    return Optional.of(target);
  }

  private String guessExtension(MultipartFile file) {
    String name = file.getOriginalFilename();
    if (name != null) {
      String lower = name.toLowerCase();
      if (lower.endsWith(".png")) return ".png";
      if (lower.endsWith(".gif")) return ".gif";
      if (lower.endsWith(".webp")) return ".webp";
      if (lower.endsWith(".bmp")) return ".bmp";
    }
    return ".jpg";
  }
}
