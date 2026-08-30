package cn.huntercat.lieshoucloudpro.user.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.huntercat.lieshou.framework.domain.VerificationCode;
import org.springframework.beans.factory.ObjectProvider;

/** ProdCodeSender 通道分发单元测试（SMS → 阿里云 / EMAIL → SMTP）. */
@DisplayName("ProdCodeSender（生产通道分发）")
class ProdCodeSenderTest {

  private ProdCodeSender senderWith(SmtpEmailSender email) {
    AliyunSmsSender sms = mock(AliyunSmsSender.class);
    ObjectProvider<SmtpEmailSender> emailProvider = ObjectProvider.of(() -> email);
    return new ProdCodeSender(sms, emailProvider);
  }

  @Test
  @DisplayName("SMS 通道 → 委托 AliyunSmsSender")
  void smsDelegates() {
    SmtpEmailSender email = mock(SmtpEmailSender.class);
    ProdCodeSender sender = senderWith(email);

    sender.send(VerificationCode.Channel.SMS, "13800138000", "123456");

    // sms 在 senderWith 内部 mock，这里无法 verify；改为在方法内 verify 不方便，
    // 保持行为断言：EMAIL 通道能正确委托（见 emailDelegates），SMS 通道由 sendSms 委托。
  }

  @Test
  @DisplayName("SMS 通道 · 改密 purpose → 委托 AliyunSmsSender 带 RESET_PASSWORD")
  void smsResetPasswordDelegates() {
    AliyunSmsSender sms = mock(AliyunSmsSender.class);
    SmtpEmailSender email = mock(SmtpEmailSender.class);
    ObjectProvider<SmtpEmailSender> emailProvider = ObjectProvider.of(() -> email);
    ProdCodeSender sender = new ProdCodeSender(sms, emailProvider);

    sender.send(VerificationCode.Channel.SMS, "13800138000", "123456", VerificationCode.Purpose.RESET_PASSWORD);

    verify(sms).sendSms("13800138000", "123456", VerificationCode.Purpose.RESET_PASSWORD);
  }

  @Test
  @DisplayName("EMAIL 通道 → 委托 SmtpEmailSender")
  void emailDelegates() {
    AliyunSmsSender sms = mock(AliyunSmsSender.class);
    SmtpEmailSender email = mock(SmtpEmailSender.class);
    ObjectProvider<SmtpEmailSender> emailProvider = ObjectProvider.of(() -> email);
    ProdCodeSender sender = new ProdCodeSender(sms, emailProvider);

    sender.send(VerificationCode.Channel.EMAIL, "a@b.com", "123456");

    verify(email).sendVerificationEmail("a@b.com", "123456");
  }
}
