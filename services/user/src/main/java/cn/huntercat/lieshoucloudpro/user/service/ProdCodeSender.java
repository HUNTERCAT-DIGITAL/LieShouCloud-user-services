package cn.huntercat.lieshoucloudpro.user.service;

import org.springframework.beans.factory.ObjectProvider;

import cn.huntercat.lieshou.framework.domain.VerificationCode;
import cn.huntercat.lieshou.framework.service.CodeSender;

/**
 * 生产验证码发送器（ADR-0023 Phase 2 落地）.
 *
 * <p>通道分发（由 {@code AliyunSmsConfig} 按配置装配，不区分环境）：
 *
 * <ul>
 *   <li><b>SMS</b> —— 阿里云短信（{@link AliyunSmsSender}，dysmsapi 真实调用）；
 *   <li><b>EMAIL</b> —— 飞书邮箱 SMTP（{@link SmtpEmailSender}，smtp.feishu.cn；未配置则抛业务异常）。
 * </ul>
 *
 * <p>安全纪律：生产**绝不**把验证码打进日志（与 {@link DevCodeSender} 相反——那是未配短信时的联调旁路）。
 */
public class ProdCodeSender implements CodeSender {

  private final AliyunSmsSender smsSender;
  private final ObjectProvider<SmtpEmailSender> emailSenderProvider;

  public ProdCodeSender(
      AliyunSmsSender smsSender, ObjectProvider<SmtpEmailSender> emailSenderProvider) {
    this.smsSender = smsSender;
    this.emailSenderProvider = emailSenderProvider;
  }

  @Override
  public void send(VerificationCode.Channel channel, String target, String code) {
    // 旧接口（无 purpose）：默认按登录验证码处理（向后兼容）
    send(channel, target, code, VerificationCode.Purpose.LOGIN);
  }

  @Override
  public void send(
      VerificationCode.Channel channel,
      String target,
      String code,
      VerificationCode.Purpose purpose) {
    switch (channel) {
      case SMS:
        smsSender.sendSms(target, code, purpose);
        return;
      case EMAIL:
        SmtpEmailSender emailSender = emailSenderProvider.getIfAvailable();
        if (emailSender == null) {
          throw new IllegalStateException("邮件通道未配置（需 EMAIL_SMTP_HOST 等 SMTP 配置）");
        }
        emailSender.sendVerificationEmail(target, code);
        return;
      default:
        throw new UnsupportedOperationException("未知验证码通道: " + channel);
    }
  }
}
