package cn.huntercat.lieshoucloudpro.user.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;

import cn.huntercat.lieshou.framework.domain.VerificationCode;

/**
 * 阿里云短信发送器（生产验证码通道 · ADR-0023 Phase 2）.
 *
 * <p>封装 dysmsapi 2017-05-25 真实调用，模板变量固定 {@code ${code}}（当前账号模板：登录确认验证码）。
 *
 * <p>安全约定（ADR-0028 / 生产纪律）：
 *
 * <ul>
 *   <li>成功仅记日志 requestId，**不落验证码**；失败仅记 masked 手机号 + 阿里云错误 message，同样不落验证码；
 *   <li>非 OK 响应码 → {@link IllegalStateException}（调用方按业务失败处理，验证码不落库）。
 * </ul>
 *
 * <p>由 {@code AliyunSmsConfig} 按配置装配（配了 AccessKey 即生效，不区分环境）。
 */
public class AliyunSmsSender {

  private static final Logger log = LoggerFactory.getLogger(AliyunSmsSender.class);

  private static final String OK_CODE = "OK";

  private final Client client;
  private final String signName;
  private final String templateCode;
  private final String resetTemplateCode;

  public AliyunSmsSender(
      Client client, String signName, String templateCode, String resetTemplateCode) {
    this.client = client;
    this.signName = signName;
    this.templateCode = templateCode;
    this.resetTemplateCode = resetTemplateCode;
  }

  /**
   * 发送短信验证码（按 purpose 选模板：登录 → templateCode，改密/重置 → resetTemplateCode）。
   *
   * @param phone 国内手机号（11 位）
   * @param code 6 位数字验证码
   * @throws IllegalStateException 配置缺失 / 阿里云返回非 OK / 网络异常
   */
  public void sendSms(String phone, String code, VerificationCode.Purpose purpose) {
    // 改密/重置密码用独立模板（SMS_152461726）；未配置回退登录模板（向后兼容）
    String tpl =
        purpose == VerificationCode.Purpose.RESET_PASSWORD && !resetTemplateCode.isBlank()
            ? resetTemplateCode
            : templateCode;
    if (signName.isBlank() || tpl.isBlank()) {
      throw new IllegalStateException(
          "阿里云短信未完整配置：需要 ALIYUN_SMS_SIGN_NAME + ALIYUN_SMS_TEMPLATE_CODE"
              + "（+ ALIYUN_SMS_TEMPLATE_RESET_CODE 用于改密）");
    }
    SendSmsRequest request =
        new SendSmsRequest()
            .setPhoneNumbers(phone)
            .setSignName(signName)
            .setTemplateCode(tpl)
            .setTemplateParam("{\"code\":\"" + code + "\"}");
    try {
      SendSmsResponse response = client.sendSms(request);
      String respCode =
          response == null || response.getBody() == null ? null : response.getBody().getCode();
      if (!OK_CODE.equals(respCode)) {
        String message =
            response != null && response.getBody() != null
                ? response.getBody().getMessage()
                : "空响应";
        log.error("阿里云短信发送失败 phone={} respCode={} message={}", maskPhone(phone), respCode, message);
        throw new IllegalStateException("阿里云短信发送失败: " + message);
      }
      log.info(
          "阿里云短信发送成功 phone={} requestId={}", maskPhone(phone), response.getBody().getRequestId());
    } catch (IllegalStateException e) {
      throw e;
    } catch (Exception e) {
      log.error("阿里云短信调用异常 phone={}", maskPhone(phone), e);
      throw new IllegalStateException("阿里云短信调用异常: " + e.getMessage(), e);
    }
  }

  /** 手机号脱敏：138****8000。 */
  static String maskPhone(String phone) {
    if (phone == null || phone.length() < 7) {
      return "***";
    }
    return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
  }
}
