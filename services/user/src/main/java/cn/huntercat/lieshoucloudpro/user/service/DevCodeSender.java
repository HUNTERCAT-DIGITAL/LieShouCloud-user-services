package cn.huntercat.lieshoucloudpro.user.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.huntercat.lieshou.framework.domain.VerificationCode;
import cn.huntercat.lieshou.framework.service.CodeSender;

/**
 * dev 验证码发送器：打印到日志（未配置阿里云短信时的联调旁路 · 由 AliyunSmsConfig 装配）.
 *
 * <p>日志格式固定为 {@code [LSCP-CODE] channel=... target=... code=...}，便于联调抓取。
 */
public class DevCodeSender implements CodeSender {

  private static final Logger log = LoggerFactory.getLogger(DevCodeSender.class);

  @Override
  public void send(VerificationCode.Channel channel, String target, String code) {
    log.info(
        "[LSCP-CODE] channel={} target={} code={} (dev sender - 生产环境接入短信/邮件服务商)",
        channel,
        target,
        code);
  }

  @Override
  public void send(
      VerificationCode.Channel channel,
      String target,
      String code,
      VerificationCode.Purpose purpose) {
    log.info(
        "[LSCP-CODE] channel={} target={} code={} purpose={} (dev sender - 生产环境接入短信/邮件服务商)",
        channel,
        target,
        code,
        purpose);
  }
}
