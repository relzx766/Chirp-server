package com.zyq.chirp.gateway.filter;

import cn.dev33.satoken.reactor.context.SaReactorSyncHolder;
import cn.dev33.satoken.reactor.filter.SaReactorFilter;
import cn.dev33.satoken.reactor.model.SaResponseForReactor;
import cn.dev33.satoken.router.SaHttpMethod;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import com.zyq.chirp.gateway.config.ExcludeAuthPathConfig;
import com.zyq.chirp.gateway.domain.pojo.Permission;
import com.zyq.chirp.gateway.service.PermissionService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.context.refresh.ConfigDataContextRefresher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.server.ServerWebExchange;

import java.util.List;

@Configuration
@RefreshScope
@Slf4j
public class AuthFilter {
    static List<Permission> permissions;
    PermissionService permissionService;
    @Resource
    ExcludeAuthPathConfig excludePaths;

    @Resource
    ConfigDataContextRefresher refresher;

    @Autowired
    public AuthFilter(PermissionService permissionService) {
        this.permissionService = permissionService;
        permissions = permissionService.getAll();

    }

    @Async
    @Scheduled(cron = "*/20 * * * * *")
    public void refreshPermission() {
        permissions = permissionService.getAll();
        refresher.refresh();
    }

    @Bean
    public SaReactorFilter getSaReactorFilter() {
        return new SaReactorFilter()
                .addInclude("/**")
                .setExcludeList(excludePaths.getPaths())
                .setBeforeAuth(obj -> {
                    ServerWebExchange webExchange = SaReactorSyncHolder.getContext();
                    log.info(webExchange.getRequest().getPath().toString());
                    SaResponseForReactor response = new SaResponseForReactor(webExchange.getResponse());
                    response.setHeader("Access-Control-Allow-Origin", "*")
                            .setHeader("Access-Control-Allow-Methods", "*")
                            //允许跨域预检请求结果缓存10分钟
                            .setHeader("Access-Control-Max-Age", "600")
                            .setHeader("Access-Control-Allow-Headers", "*");
                    // 如果是预检请求，则立即返回到前端
                    SaRouter.match(SaHttpMethod.OPTIONS).back();
                })
                .setAuth(obj -> {
                    SaRouter.match("/**", "/auth-service/**", r -> StpUtil.checkLogin());
                    //获取全部需要鉴权的路由
                    permissions.forEach(permission -> {
                        SaRouter.match(permission.getPath(), r -> StpUtil.checkPermission(permission.getPath()));
                    });
                });
    }
}
