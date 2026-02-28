package com.sqmusicplus.v3.config;

import cn.dev33.satoken.stp.SaLoginConfig;
import cn.dev33.satoken.stp.SaLoginModel;
import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 基于可信反向代理透传的请求头进行自动登录
 */
@Slf4j
@Component
public class AuthentikHeaderAuthInterceptor implements HandlerInterceptor {

    @Value("${sqmusic.auth.header.enabled:false}")
    private boolean headerAuthEnabled;

    @Value("${sqmusic.auth.header.user-header:X-authentik-username}")
    private String userHeader;

    @Value("${sqmusic.auth.header.required-header:X-authentik-authenticated}")
    private String requiredHeader;

    @Value("${sqmusic.auth.header.required-header-value:true}")
    private String requiredHeaderValue;

    @Value("${sqmusic.auth.header.login-id-prefix:authentik:}")
    private String loginIdPrefix;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!headerAuthEnabled || "OPTIONS".equalsIgnoreCase(request.getMethod()) || StpUtil.isLogin()) {
            return true;
        }

        if (!matchRequiredHeader(request)) {
            return true;
        }

        String username = request.getHeader(userHeader);
        if (!StringUtils.hasText(username)) {
            return true;
        }

        String loginId = loginIdPrefix + username.trim();
        SaLoginModel loginConfig = SaLoginConfig
                .setExtra("device", "authentik")
                .setExtra("authType", "header")
                .setExtra("username", username.trim())
                .setIsLastingCookie(false);
        StpUtil.login(loginId, loginConfig);
        log.debug("Header auth success, auto login id: {}", loginId);
        return true;
    }

    private boolean matchRequiredHeader(HttpServletRequest request) {
        if (!StringUtils.hasText(requiredHeader)) {
            return true;
        }
        String value = request.getHeader(requiredHeader);
        if (!StringUtils.hasText(requiredHeaderValue)) {
            return StringUtils.hasText(value);
        }
        return requiredHeaderValue.equalsIgnoreCase(value);
    }
}