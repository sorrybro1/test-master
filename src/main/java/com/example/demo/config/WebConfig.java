package com.example.demo.config;

import com.example.demo.interceptor.AdminLoginInterceptor;
import com.example.demo.interceptor.LoginInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

    @Configuration
    public class WebConfig implements WebMvcConfigurer {

        private final LoginInterceptor loginInterceptor;
        private final AdminLoginInterceptor adminLoginInterceptor;

        public WebConfig(LoginInterceptor loginInterceptor,
                         AdminLoginInterceptor adminLoginInterceptor) {
            this.loginInterceptor = loginInterceptor;
            this.adminLoginInterceptor = adminLoginInterceptor;
        }

        @Override
        public void addInterceptors(InterceptorRegistry registry) {

            // ===== 后台端：/admin/** + /admin-api/** =====
            registry.addInterceptor(adminLoginInterceptor)
                    .addPathPatterns("/admin/**", "/admin-api/**")
                    .excludePathPatterns(
                            "/admin/adminlogin.html",
                            "/admin-api/auth/login",
                            "/admin-api/auth/logout",
                            "/admin-api/auth/check",

                            // 静态资源
                            "/**/*.css", "/**/*.js", "/**/*.png", "/**/*.jpg",
                            "/**/*.jpeg", "/**/*.gif", "/**/*.ico",
                            "/**/*.woff", "/**/*.woff2", "/**/*.ttf", "/**/*.svg"
                    )
                    .order(1);

            // ===== 学生/老师端：只拦前台相关路径（关键：不要用 /**） =====
            registry.addInterceptor(loginInterceptor)
                    .addPathPatterns(
                            "/pages/**",
                            "/api/**",
                            "/userCtrl/**"  // 如果你项目里还在用旧的 /userCtrl
                    )
                    .excludePathPatterns(
                            "/", "/index.html", "/login.html",
                            "/pages/login.html",
                            "/api/auth/login", "/api/auth/logout", "/api/auth/check",

                            // 静态资源
                            "/**/*.css", "/**/*.js", "/**/*.png", "/**/*.jpg",
                            "/**/*.jpeg", "/**/*.gif", "/**/*.ico",
                            "/**/*.woff", "/**/*.woff2", "/**/*.ttf", "/**/*.svg"
                    )
                    .order(2);
        }
    }
