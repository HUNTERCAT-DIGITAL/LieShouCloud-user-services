package cn.huntercat.lieshoucloudpro.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;

/**
 * 猎手云 Pro · User 服务入口。
 *
 * <ul>
 *   <li>{@link EnableDiscoveryClient} —— 注册到 Nacos，让 gateway 能 lb:// 找到
 *   <li>{@link EntityScan} + {@link EnableJpaRepositories} —— 锁在 {@code user.*} 子包，避免与未来其他服务 entity
 *       冲突
 *   <li>{@link OpenAPIDefinition} —— Phase 5 SpringDoc 元信息；前端 openapi-typescript 用此生成 typed client
 * </ul>
 */
@SpringBootApplication
// 显式扫描(含 framework 共享层), 并排除 framework 的 DevCodeSender:
// @Component @ConditionalOnMissingBean 兑底在用户扫描下不生效(Spring 限制),
// 会与消费方本地 CodeSender(DevCodeSender @Profile dev/docker)同名 bean 冲突
@ComponentScan(
    basePackages = {"cn.huntercat.lieshoucloudpro", "cn.huntercat.lieshou.framework"},
    excludeFilters =
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = cn.huntercat.lieshou.framework.service.DevCodeSender.class))
@EntityScan(basePackages = "cn.huntercat.lieshou.framework.domain")
@EnableJpaRepositories(basePackages = "cn.huntercat.lieshou.framework.domain")
@EnableDiscoveryClient
@OpenAPIDefinition(
    info =
        @Info(
            title = "LieShou Cloud · User Service",
            version = "0.0.1",
            description = "User domain API (CRUD, lookup, count)",
            contact = @Contact(name = "FutureWL", email = "624263934@qq.com"),
            license = @License(name = "MIT")),
    servers = {
      @Server(url = "http://localhost:9000", description = "via Gateway (recommended)"),
      @Server(url = "http://localhost:8081", description = "direct (dev only)")
    })
public class UserApplication {

  public static void main(String[] args) {
    SpringApplication.run(UserApplication.class, args);
  }
}
