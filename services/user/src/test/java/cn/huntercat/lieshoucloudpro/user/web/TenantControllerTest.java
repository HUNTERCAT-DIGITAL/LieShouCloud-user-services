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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cn.huntercat.lieshou.framework.common.api.BaseException;
import cn.huntercat.lieshou.framework.common.api.ErrorCode;
import org.springframework.beans.factory.ObjectProvider;
import cn.huntercat.lieshou.framework.common.web.GlobalExceptionHandler;
import cn.huntercat.lieshou.framework.i18n.I18nMessages;
import cn.huntercat.lieshou.framework.domain.Tenant;
import cn.huntercat.lieshou.framework.service.AuditService;
import cn.huntercat.lieshou.framework.service.TenantRegistrationService;
import cn.huntercat.lieshou.framework.service.TenantService;

/**
 * TenantController web 层契约测试.
 *
 * <p>锁定错误码契约：FORBIDDEN / TENANT_CODE_TAKEN / INVALID_EDITION / INVALID_STATUS / REGISTER_INVALID /
 * CONFLICT / NOT_FOUND；自助注册公开端点（无鉴权）。
 */
@ExtendWith(MockitoExtension.class)
class TenantControllerTest {

  @Mock TenantService tenantService;

  @Mock ObjectProvider<I18nMessages> provider;
  @Mock AuditService auditService;

  MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new TenantController(tenantService, auditService))
            .setControllerAdvice(new GlobalExceptionHandler(provider))
            .build();
  }

  @Test
  void list_无PLATFORM_ADMIN抛403() throws Exception {
    mockMvc
        .perform(get("/api/tenants").header("X-User-Roles", "USER"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("FORBIDDEN"));
  }

  @Test
  void create_成功返回租户() throws Exception {
    when(tenantService.create("Acme", "acme", null)).thenReturn(new Tenant("Acme", "acme"));

    mockMvc
        .perform(
            post("/api/tenants")
                .header("X-User-Roles", "PLATFORM_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Acme\",\"code\":\"acme\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("acme"));
  }

  @Test
  void create_编码重复映射400_TENANT_CODE_TAKEN() throws Exception {
    when(tenantService.create(anyString(), anyString(), any()))
        .thenThrow(new BaseException("TENANT_CODE_TAKEN", HttpStatus.BAD_REQUEST, "租户编码已存在"));

    mockMvc
        .perform(
            post("/api/tenants")
                .header("X-User-Roles", "PLATFORM_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"重复\",\"code\":\"huntercat\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("TENANT_CODE_TAKEN"));
  }

  @Test
  void create_非法版别映射400_INVALID_EDITION() throws Exception {
    when(tenantService.create(anyString(), anyString(), any()))
        .thenThrow(new BaseException("INVALID_EDITION", HttpStatus.BAD_REQUEST, "租户版别不合法"));

    mockMvc
        .perform(
            post("/api/tenants")
                .header("X-User-Roles", "PLATFORM_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"x\",\"code\":\"acme\",\"edition\":\"NOPE\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("INVALID_EDITION"));
  }

  @Test
  void update_非法状态映射400_INVALID_STATUS() throws Exception {
    when(tenantService.update(any(), any(), any(), any()))
        .thenThrow(new BaseException("INVALID_STATUS", HttpStatus.BAD_REQUEST, "租户状态不合法"));

    mockMvc
        .perform(
            put("/api/tenants/1")
                .header("X-User-Roles", "PLATFORM_ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"PAUSED\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("INVALID_STATUS"));
  }

  @Test
  void delete_有用户映射409_CONFLICT() throws Exception {
    when(tenantService.delete(1L)).thenThrow(new BaseException(ErrorCode.CONFLICT, "租户仍有关联用户"));

    mockMvc
        .perform(delete("/api/tenants/1").header("X-User-Roles", "PLATFORM_ADMIN"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error").value("CONFLICT"));
  }

  @Test
  void register_公开端点且非法输入映射400_REGISTER_INVALID() throws Exception {
    when(tenantService.register(any(), any(), any(), any(), any(), any()))
        .thenThrow(
            new BaseException("REGISTER_INVALID", HttpStatus.BAD_REQUEST, "租户编码已被占用: huntercat"));

    mockMvc
        .perform(
            post("/api/tenants/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"tenantName\":\"x\",\"tenantCode\":\"huntercat\",\"username\":\"a\",\"displayName\":\"a\",\"password\":\"secret123\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("REGISTER_INVALID"));
  }

  @Test
  void register_成功返回租户与管理员() throws Exception {
    TenantRegistrationService.RegistrationResult result =
        new TenantRegistrationService.RegistrationResult(new Tenant("测试", "acme"), "admin", "管理员");
    when(tenantService.register(any(), any(), any(), any(), any(), any())).thenReturn(result);

    mockMvc
        .perform(
            post("/api/tenants/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"tenantName\":\"测试\",\"tenantCode\":\"acme\",\"username\":\"admin\",\"displayName\":\"管理员\",\"password\":\"secret123\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.adminUsername").value("admin"))
        .andExpect(jsonPath("$.tenant.code").value("acme"));
  }
}
