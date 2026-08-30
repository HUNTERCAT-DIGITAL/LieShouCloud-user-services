package cn.huntercat.lieshoucloudpro.user.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.dysmsapi20170525.models.SendSmsResponseBody;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cn.huntercat.lieshou.framework.domain.VerificationCode;

/** AliyunSmsSender 单元测试（mock dysmsapi Client，不启动 Spring 上下文）. */
@DisplayName("AliyunSmsSender（阿里云短信发送）")
class AliyunSmsSenderTest {

  private static final String SIGN = "阿基皕科技";
  private static final String TEMPLATE = "SMS_152461729";
  private static final String RESET_TEMPLATE = "SMS_152461726";

  private static SendSmsResponse response(String code, String message) {
    return new SendSmsResponse()
        .setBody(new SendSmsResponseBody().setCode(code).setMessage(message).setRequestId("req-1"));
  }

  @Test
  @DisplayName("阿里云返回 OK → 发送成功不抛异常")
  void ok() throws Exception {
    Client client = mock(Client.class);
    when(client.sendSms(any())).thenReturn(response("OK", "OK"));
    AliyunSmsSender sender = new AliyunSmsSender(client, SIGN, TEMPLATE, RESET_TEMPLATE);

    sender.sendSms("13800138000", "123456", VerificationCode.Purpose.LOGIN);
  }

  @Test
  @DisplayName("LOGIN → 登录验证码模板；RESET_PASSWORD → 改密验证码模板")
  void templateByPurpose() throws Exception {
    Client client = mock(Client.class);
    when(client.sendSms(any())).thenReturn(response("OK", "OK"));
    AliyunSmsSender sender = new AliyunSmsSender(client, SIGN, TEMPLATE, RESET_TEMPLATE);

    sender.sendSms("13800138000", "123456", VerificationCode.Purpose.LOGIN);
    sender.sendSms("13800138000", "654321", VerificationCode.Purpose.RESET_PASSWORD);

    org.mockito.Mockito.verify(client)
        .sendSms(argThat((SendSmsRequest r) -> TEMPLATE.equals(r.getTemplateCode())));
    org.mockito.Mockito.verify(client)
        .sendSms(argThat((SendSmsRequest r) -> RESET_TEMPLATE.equals(r.getTemplateCode())));
  }

  @Test
  @DisplayName("阿里云返回业务错误码 → IllegalStateException 且含 message")
  void businessError() throws Exception {
    Client client = mock(Client.class);
    when(client.sendSms(any())).thenReturn(response("isv.SMS_SIGNATURE_ILLEGAL", "签名不合法"));
    AliyunSmsSender sender = new AliyunSmsSender(client, SIGN, TEMPLATE, RESET_TEMPLATE);

    assertThatThrownBy(() -> sender.sendSms("13800138000", "123456", VerificationCode.Purpose.LOGIN))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("签名不合法");
  }

  @Test
  @DisplayName("签名或模板未配置 → fail-fast IllegalStateException")
  void missingConfig() {
    Client client = mock(Client.class);
    AliyunSmsSender sender = new AliyunSmsSender(client, "", TEMPLATE, RESET_TEMPLATE);

    assertThatThrownBy(() -> sender.sendSms("13800138000", "123456", VerificationCode.Purpose.LOGIN))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("ALIYUN_SMS_SIGN_NAME");
  }

  @Test
  @DisplayName("SDK 网络异常 → 包装为 IllegalStateException")
  void sdkException() throws Exception {
    Client client = mock(Client.class);
    when(client.sendSms(any())).thenThrow(new RuntimeException("connect timeout"));
    AliyunSmsSender sender = new AliyunSmsSender(client, SIGN, TEMPLATE, RESET_TEMPLATE);

    assertThatThrownBy(() -> sender.sendSms("13800138000", "123456", VerificationCode.Purpose.LOGIN))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("connect timeout");
  }

  @Test
  @DisplayName("手机号脱敏：138****8000")
  void maskPhone() {
    assertThat(AliyunSmsSender.maskPhone("13800138000")).isEqualTo("138****8000");
    assertThat(AliyunSmsSender.maskPhone("123")).isEqualTo("***");
  }
}
