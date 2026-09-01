package cn.huntercat.lieshoucloudpro.user.web;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cn.huntercat.lieshou.framework.common.api.BaseException;
import cn.huntercat.lieshou.framework.common.web.GlobalExceptionHandler;
import cn.huntercat.lieshou.framework.domain.TenantInvite;
import cn.huntercat.lieshou.framework.i18n.I18nMessages;
import cn.huntercat.lieshou.framework.service.TenantInviteService;

/**
 * TenantInviteController web 层契约测试.
 *
 * <p>锁定：授权（PLATFORM_ADMIN / 本租户 TENANT_ADMIN → 403 FORBIDDEN）、 错误码契约 INVALID_ROLE / NOT_FOUND。
 */
@ExtendWith(MockitoExtension.class)
class TenantInviteControllerTest {

  @Mock TenantInviteService inviteService;

  @Mock ObjectProvider<I18nMessages> provider;

  MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new TenantInviteController(inviteService))
            .setControllerAdvice(new GlobalExceptionHandler(provider))
            .build();
  }

  @Test
  void create_无权限抛403_FORBIDDEN() throws Exception {
    mockMvc
        .perform(
            post("/api/tenants/1/invites")
                .header("X-User-Roles", "USER")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("FORBIDDEN"));
  }

  @Test
  void create_平台管理员成功() throws Exception {
    when(inviteService.create(1L, null, null, null))
        .thenReturn(new TenantInvite(1L, "CODE1234", "USER", null, null, null));

    mockMvc
        .perform(
            post("/api/tenants/1/invites")
                .header("X-User-Roles", "PLATFORM_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("CODE1234"));
  }

  @Test
  void create_本租户管理员成功_跨租户拒绝() throws Exception {
    when(inviteService.create(1L, "USER", 7, 10))
        .thenReturn(new TenantInvite(1L, "CODE5678", "USER", null, null, null));

    // 本租户管理员（X-Tenant-Id 匹配路径 tenantId）→ 200
    mockMvc
        .perform(
            post("/api/tenants/1/invites")
                .header("X-User-Roles", "TENANT_ADMIN")
                .header("X-Tenant-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"USER\",\"expiresInDays\":7,\"maxUses\":10}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("CODE5678"));

    // 跨租户（X-Tenant-Id=2 ≠ 路径 1）→ 403
    mockMvc
        .perform(
            post("/api/tenants/1/invites")
                .header("X-User-Roles", "TENANT_ADMIN")
                .header("X-Tenant-Id", "2")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("FORBIDDEN"));
  }

  @Test
  void create_非法角色映射400_INVALID_ROLE() throws Exception {
    when(inviteService.create(any(), any(), any(), any()))
        .thenThrow(
            new BaseException("INVALID_ROLE", HttpStatus.BAD_REQUEST, "邀请码角色仅支持 USER / ADMIN"));

    mockMvc
        .perform(
            post("/api/tenants/1/invites")
                .header("X-User-Roles", "PLATFORM_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"SUPER\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("INVALID_ROLE"));
  }

  @Test
  void list_租户不存在映射404_NOT_FOUND() throws Exception {
    when(inviteService.list(99L))
        .thenThrow(new BaseException("NOT_FOUND", HttpStatus.NOT_FOUND, "租户不存在"));

    mockMvc
        .perform(get("/api/tenants/99/invites").header("X-User-Roles", "PLATFORM_ADMIN"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("NOT_FOUND"));
  }
}
