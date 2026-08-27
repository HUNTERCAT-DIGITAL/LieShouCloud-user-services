package cn.huntercat.lieshoucloudpro.user.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cn.huntercat.lieshoucloudpro.user.PostgresTestSupport;
import cn.huntercat.lieshou.framework.domain.Notification;
import cn.huntercat.lieshou.framework.domain.NotificationRepository;
import java.time.Instant;
import java.util.List;

/**
 * NotificationController 集成测试（@SpringBootTest + MockMvc + Mock repo）.
 *
 * <p>覆盖：接收者上下文（X-Tenant-Id + X-User-Id）校验、列表/未读数/标记已读/全部已读、 发送端点 PLATFORM_ADMIN 权限。
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("NotificationController（站内通知）")
class NotificationControllerTest extends PostgresTestSupport {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private NotificationRepository repo;

  private Notification n(Long id, Long tid, Long uid, boolean read) {
    Notification n =
        Notification.builder()
            .tenantId(tid)
            .userId(uid)
            .type("SYSTEM")
            .title("测试通知")
            .content("内容")
            .createdAt(Instant.now())
            .build();
    // 通过构造器无 id 字段，用反射兜底不必要——直接 mock 返回即可
    return n;
  }

  @Test
  @DisplayName("带租户/用户上下文 → 返回列表")
  void list_withContext() throws Exception {
    when(repo.findByTenantIdAndUserIdOrderByReadAtAscCreatedAtDesc(7L, 3L))
        .thenReturn(List.of(n(1L, 7L, 3L, false)));
    mockMvc
        .perform(get("/api/notifications").header("X-Tenant-Id", "7").header("X-User-Id", "3"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].title").value("测试通知"));
  }

  @Test
  @DisplayName("缺租户/用户上下文 → 400")
  void list_missingContext() throws Exception {
    mockMvc.perform(get("/api/notifications")).andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("未读计数")
  void unreadCount() throws Exception {
    when(repo.countByTenantIdAndUserIdAndReadAtIsNull(7L, 3L)).thenReturn(5L);
    mockMvc
        .perform(
            get("/api/notifications/unread-count")
                .header("X-Tenant-Id", "7")
                .header("X-User-Id", "3"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.unread").value(5));
  }

  @Test
  @DisplayName("标记单条已读：未读 → 200；不存在 → 404")
  void markRead() throws Exception {
    when(repo.markRead(eq(9L), eq(7L), eq(3L), any())).thenReturn(1);
    mockMvc
        .perform(
            post("/api/notifications/9/read").header("X-Tenant-Id", "7").header("X-User-Id", "3"))
        .andExpect(status().isOk());

    when(repo.markRead(eq(9L), eq(7L), eq(3L), any())).thenReturn(0);
    mockMvc
        .perform(
            post("/api/notifications/9/read").header("X-Tenant-Id", "7").header("X-User-Id", "3"))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("全部标记已读")
  void markAllRead() throws Exception {
    when(repo.markAllRead(eq(7L), eq(3L), any())).thenReturn(4);
    mockMvc
        .perform(
            post("/api/notifications/read-all").header("X-Tenant-Id", "7").header("X-User-Id", "3"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.updated").value(4));
  }

  @Test
  @DisplayName("发送：PLATFORM_ADMIN → 201；无权限 → 403")
  void send_permission() throws Exception {
    String body = """
        {"userId":3,"title":"新通知","content":"hi","type":"SYSTEM"}
        """;
    // 无权限
    mockMvc
        .perform(
            post("/api/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .header("X-Tenant-Id", "7"))
        .andExpect(status().isForbidden());

    // 平台管理员（mock repo.save 返回实体）
    when(repo.save(any(Notification.class)))
        .thenReturn(
            Notification.builder()
                .tenantId(7L)
                .userId(3L)
                .title("新通知")
                .createdAt(Instant.now())
                .build());
    mockMvc
        .perform(
            post("/api/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .header("X-Tenant-Id", "7")
                .header("X-User-Roles", "PLATFORM_ADMIN"))
        .andExpect(status().isCreated());
  }
}
