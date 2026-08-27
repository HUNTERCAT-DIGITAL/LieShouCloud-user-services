package cn.huntercat.lieshoucloudpro.user.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.huntercat.lieshou.framework.common.audit.AuditEvent;
import cn.huntercat.lieshou.framework.domain.AuditLog;
import cn.huntercat.lieshou.framework.domain.AuditLogRepository;
import java.time.Instant;

/** AuditService 实现 AuditRecorder SPI 的适配测试（L2-1 审计注解接入）. */
@ExtendWith(MockitoExtension.class)
class AuditServiceRecorderTest {

  @Mock private AuditLogRepository repo;

  @InjectMocks private AuditService auditService;

  private AuditLog captureSaved() {
    ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
    verify(repo).save(captor.capture());
    return captor.getValue();
  }

  @Test
  void record_mapsEventToAuditLog() {
    when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

    auditService.record(
        new AuditEvent(
            7L,
            42L,
            "CREATE",
            "role",
            9L,
            AuditEvent.Outcome.SUCCESS,
            "创建角色 OPERATOR",
            "10.0.0.9",
            "test-agent",
            Instant.now()));

    AuditLog saved = captureSaved();
    assertThat(saved.getTenantId()).isEqualTo(7L);
    assertThat(saved.getUserId()).isEqualTo(42L);
    assertThat(saved.getAction()).isEqualTo(AuditLog.Action.CREATE);
    assertThat(saved.getResourceType()).isEqualTo("role");
    assertThat(saved.getResourceId()).isEqualTo(9L);
    assertThat(saved.getOutcome()).isEqualTo(AuditLog.Outcome.SUCCESS);
  }

  @Test
  void record_mapsFailureToErrorOutcome() {
    when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

    auditService.record(
        new AuditEvent(
            null,
            null,
            "DELETE",
            "role",
            null,
            AuditEvent.Outcome.FAILURE,
            "boom",
            null,
            null,
            Instant.now()));

    assertThat(captureSaved().getOutcome()).isEqualTo(AuditLog.Outcome.ERROR);
  }

  @Test
  void record_unknownActionFallsBackToRead() {
    when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

    auditService.record(
        new AuditEvent(
            null,
            null,
            "APPROVE",
            "approval",
            null,
            AuditEvent.Outcome.SUCCESS,
            null,
            null,
            null,
            Instant.now()));

    assertThat(captureSaved().getAction()).isEqualTo(AuditLog.Action.READ);
  }
}
