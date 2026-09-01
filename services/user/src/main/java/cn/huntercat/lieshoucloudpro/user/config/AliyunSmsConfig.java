package cn.huntercat.lieshoucloudpro.user.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.teaopenapi.models.Config;

import cn.huntercat.lieshou.framework.service.CodeSender;
import cn.huntercat.lieshoucloudpro.user.service.AliyunSmsSender;
import cn.huntercat.lieshoucloudpro.user.service.DevCodeSender;
import cn.huntercat.lieshoucloudpro.user.service.ProdCodeSender;
import cn.huntercat.lieshoucloudpro.user.service.SmtpEmailSender;

/**
 * 验证码发送器装配（2026-08 对齐 iot-service「不区分环境」模式）.
 *
 * <p>**不区分环境**：任何 profile 下，配置了 {@code ALIYUN_SMS_ACCESS_KEY_ID/SECRET} 即真实发送 （ProdCodeSender →
 * AliyunSmsSender 阿里云短信），未配置则降级日志旁路（DevCodeSender）—— 开发/测试环境配 key 后同样可真实发短信（H5 验证码登录），不配也不会启动失败。
 *
 * <p>读取环境变量：
 *
 * <ul>
 *   <li>{@code ALIYUN_SMS_ACCESS_KEY_ID} / {@code ALIYUN_SMS_ACCESS_KEY_SECRET} —— 可选；缺失 → 旁路；
 *   <li>{@code ALIYUN_SMS_SIGN_NAME} —— 签名（阿基皕科技）；
 *   <li>{@code ALIYUN_SMS_TEMPLATE_CODE} —— 登录验证码模板（SMS_152461729，code）；
 *   <li>{@code ALIYUN_SMS_TEMPLATE_RESET_CODE} —— 改密/重置密码模板（SMS_152461726，code）；
 *   <li>{@code ALIYUN_SMS_ENDPOINT} —— 可选，默认国内版 {@code dysmsapi.aliyuncs.com}。
 * </ul>
 */
@Configuration
public class AliyunSmsConfig {

  private static final Logger log = LoggerFactory.getLogger(AliyunSmsConfig.class);

  @Bean
  public CodeSender codeSender(
      @Value("${ALIYUN_SMS_ACCESS_KEY_ID:}") String accessKeyId,
      @Value("${ALIYUN_SMS_ACCESS_KEY_SECRET:}") String accessKeySecret,
      @Value("${ALIYUN_SMS_ENDPOINT:dysmsapi.aliyuncs.com}") String endpoint,
      @Value("${ALIYUN_SMS_SIGN_NAME:}") String signName,
      @Value("${ALIYUN_SMS_TEMPLATE_CODE:}") String templateCode,
      @Value("${ALIYUN_SMS_TEMPLATE_RESET_CODE:}") String resetTemplateCode,
      ObjectProvider<SmtpEmailSender> emailSenderProvider) {
    if (accessKeyId.isBlank() || accessKeySecret.isBlank()) {
      log.warn("阿里云短信未配置（ALIYUN_SMS_ACCESS_KEY_ID/SECRET 缺失），验证码发送走日志旁路（DevCodeSender）");
      return new DevCodeSender();
    }
    Config config =
        new Config()
            .setAccessKeyId(accessKeyId)
            .setAccessKeySecret(accessKeySecret)
            .setEndpoint(endpoint);
    try {
      Client client = new Client(config);
      AliyunSmsSender smsSender =
          new AliyunSmsSender(client, signName, templateCode, resetTemplateCode);
      SmtpEmailSender emailSender = emailSenderProvider.getIfAvailable();
      log.info("阿里云短信已配置（真实发送），emailSender 装配={}", emailSender != null ? "是" : "否（邮件通道降级）");
      return new ProdCodeSender(smsSender, emailSenderProvider);
    } catch (Exception e) {
      throw new IllegalStateException("初始化阿里云短信客户端失败: " + e.getMessage(), e);
    }
  }
}
