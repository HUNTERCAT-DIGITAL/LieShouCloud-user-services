package cn.huntercat.lieshoucloudpro.user.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cn.huntercat.lieshou.framework.common.api.BaseException;
import cn.huntercat.lieshou.framework.common.web.GlobalExceptionHandler;
import cn.huntercat.lieshou.framework.domain.Role;
import cn.huntercat.lieshou.framework.service.RoleService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * RoleController web 层契约测试.
 *
 * <p>锁定错误码契约：ROLE_CODE_TAKEN / SYSTEM_ROLE_READONLY / NOT_FOUND。
 * 注：@RequiresPermission / @Audited 为 Spring AOP 注解（standalone MockMvc 不织入），
 * 权限行为由 RequiresPermissionAspectTest / 运行集成验证覆盖。
 */
@ExtendWith(MockitoExtension.class)
class RoleControllerTest {

  @Mock RoleService roleService;

  MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new RoleController(roleService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  void create_成功返回角色() throws Exception {
    when(roleService.create("ops", "运维", null, null))
        .thenReturn(new Role("ops", "运维", Role.Scope.TENANT, null, false));

    mockMvc
        .perform(
            post("/api/roles")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"ops\",\"name\":\"运维\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("ops"));
  }

  @Test
  void create_编码重复映射400_ROLE_CODE_TAKEN() throws Exception {
    when(roleService.create(anyString(), anyString(), any(), any()))
        .thenThrow(new BaseException("ROLE_CODE_TAKEN", HttpStatus.BAD_REQUEST, "角色编码已存在"));

    mockMvc
        .perform(
            post("/api/roles")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"USER\",\"name\":\"重复\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("ROLE_CODE_TAKEN"));
  }

  @Test
  void update_system角色映射400_SYSTEM_ROLE_READONLY() throws Exception {
    when(roleService.update(any(), any(), any(), any()))
        .thenThrow(new BaseException("SYSTEM_ROLE_READONLY", HttpStatus.BAD_REQUEST, "系统角色不可修改"));

    mockMvc
        .perform(
            put("/api/roles/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"x\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("SYSTEM_ROLE_READONLY"));
  }

  @Test
  void delete_不存在映射404_NOT_FOUND() throws Exception {
    org.mockito.Mockito.doThrow(new BaseException("NOT_FOUND", HttpStatus.NOT_FOUND, "角色不存在"))
        .when(roleService).delete(99L);

    mockMvc
        .perform(delete("/api/roles/99"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("NOT_FOUND"));
  }

  @Test
  void delete_成功返回204() throws Exception {
    mockMvc.perform(delete("/api/roles/1")).andExpect(status().isNoContent());
  }
}
