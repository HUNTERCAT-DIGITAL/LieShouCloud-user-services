package cn.huntercat.lieshoucloudpro.user.web;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cn.huntercat.lieshou.framework.common.api.BaseException;
import cn.huntercat.lieshou.framework.common.api.ErrorCode;
import cn.huntercat.lieshou.framework.common.dto.UserAuthView;
import org.springframework.beans.factory.ObjectProvider;
import cn.huntercat.lieshou.framework.common.web.GlobalExceptionHandler;
import cn.huntercat.lieshou.framework.i18n.I18nMessages;
import cn.huntercat.lieshou.framework.domain.Tenant;
import cn.huntercat.lieshou.framework.domain.User;
import cn.huntercat.lieshou.framework.service.AuditService;
import cn.huntercat.lieshou.framework.service.UserService;
import java.util.List;

/**
 * UserController web 层契约测试（MockMvc standalone + GlobalExceptionHandler）.
 *
 * <p>锁定错误码契约：USERNAME_TAKEN / INVALID_INVITE / INVALID_STATUS / NOT_FOUND / TENANT_DISABLED /
 * FORBIDDEN；正常路径响应结构。
 */
@ExtendWith(MockitoExtension.class)
class UserControllerTest {

  @Mock UserService userService;

  @Mock ObjectProvider<I18nMessages> provider;
  @Mock AuditService auditService;

  MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new UserController(userService, auditService))
            .setControllerAdvice(new GlobalExceptionHandler(provider))
            .build();
  }

  private static User user() {
    User u = new User();
    u.setId(1L);
    u.setTenantId(1L);
    u.setUsername("admin");
    u.setDisplayName("管理员");
    return u;
  }

  private static Tenant tenant() {
    Tenant t = new Tenant("猎手云", "huntercat");
    t.setEdition(Tenant.Edition.GENERIC);
    return t;
  }

  @Test
  void list_租户内请求返回租户用户() throws Exception {
    when(userService.list(1L)).thenReturn(List.of(user()));

    mockMvc
        .perform(get("/api/users").header("X-Tenant-Id", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].username").value("admin"));
  }

  @Test
  void list_跨租户无PLATFORM_ADMIN抛403() throws Exception {
    mockMvc
        .perform(get("/api/users").header("X-User-Roles", "USER"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("FORBIDDEN"));
  }

  @Test
  void create_成功返回租户信息() throws Exception {
    User u = user();
    u.setPasswordHash("hash");
    when(userService.create(any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(new UserService.CreateResult(u, tenant()));

    mockMvc
        .perform(
            post("/api/users")
                .header("X-Tenant-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"username\":\"admin\",\"displayName\":\"管理员\",\"password\":\"x12345\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("admin"))
        .andExpect(jsonPath("$.tenantCode").value("huntercat"))
        .andExpect(jsonPath("$.tenantEdition").value("GENERIC"));
  }

  @Test
  void create_用户名重复映射400_USERNAME_TAKEN() throws Exception {
    when(userService.create(any(), any(), any(), any(), any(), any(), any(), any()))
        .thenThrow(new BaseException("USERNAME_TAKEN", HttpStatus.BAD_REQUEST, "用户名已被占用"));

    mockMvc
        .perform(
            post("/api/users")
                .header("X-Tenant-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"displayName\":\"x\",\"password\":\"x12345\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("USERNAME_TAKEN"));
  }

  @Test
  void create_邀请码无效映射400_INVALID_INVITE() throws Exception {
    when(userService.create(any(), any(), any(), any(), any(), any(), any(), any()))
        .thenThrow(new BaseException("INVALID_INVITE", HttpStatus.BAD_REQUEST, "邀请码无效"));

    mockMvc
        .perform(
            post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"username\":\"a\",\"displayName\":\"a\",\"password\":\"x12345\",\"inviteCode\":\"BAD\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("INVALID_INVITE"));
  }

  @Test
  void update_非法状态映射400_INVALID_STATUS() throws Exception {
    when(userService.update(any(), any(), any(), any(), any(), any(), any(), any()))
        .thenThrow(new BaseException("INVALID_STATUS", HttpStatus.BAD_REQUEST, "用户状态不合法"));

    mockMvc
        .perform(
            put("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"PAUSED\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("INVALID_STATUS"));
  }

  @Test
  void get_不存在映射404_NOT_FOUND() throws Exception {
    when(userService.get(99L, null)).thenThrow(new BaseException(ErrorCode.NOT_FOUND, "用户不存在"));

    mockMvc
        .perform(get("/api/users/99"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("NOT_FOUND"));
  }

  @Test
  void authView_租户停用映射403_TENANT_DISABLED() throws Exception {
    when(userService.authViewByTenantAndUsername("acme", "admin"))
        .thenThrow(new BaseException("TENANT_DISABLED", HttpStatus.FORBIDDEN, "租户已停用"));

    mockMvc
        .perform(get("/api/users/auth/by-tenant/acme/admin"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("TENANT_DISABLED"));
  }

  @Test
  void authView_成功返回视图() throws Exception {
    UserAuthView view =
        new UserAuthView(
            1L,
            1L,
            "huntercat",
            "猎手云",
            "GENERIC",
            "admin",
            "管理员",
            "hash",
            List.of("PLATFORM_ADMIN"),
            "ACTIVE");
    when(userService.authViewByTenantAndUsername("huntercat", "admin")).thenReturn(view);

    mockMvc
        .perform(get("/api/users/auth/by-tenant/huntercat/admin"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.roles[0]").value("PLATFORM_ADMIN"))
        .andExpect(jsonPath("$.tenantCode").value("huntercat"));
  }
}
