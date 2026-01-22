package com.example.demo.config;

import com.example.demo.interceptor.AdminLoginInterceptor;
import com.example.demo.interceptor.LoginInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

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
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadPath = Paths.get("uploads").toAbsolutePath().toUri().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadPath);

        // 让 /resources/** 映射到 /static/
        // 这样 CKEditor 请求 /resources/ckeditor/xxx 时，
        // Spring Boot 会去找 /static/ckeditor/xxx
        registry.addResourceHandler("/resources/**")
                .addResourceLocations("classpath:/static/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(adminLoginInterceptor)
                .addPathPatterns("/admin/**", "/admin-api/**")
                .excludePathPatterns("/uploads/**")
                .excludePathPatterns(
                        "/admin/adminlogin.html",
                        "/admin-api/auth/login",
                        "/admin-api/auth/logout",
                        "/admin-api/auth/check",
                        "/uploads/**",

                        // 排除 /resources/**
                        "/resources/**",
                        "/ckeditor/**",
                        "/vendor/**",
                        "/css/**",
                        "/js/**",

                        "/**/*.css", "/**/*.js", "/**/*.png", "/**/*.jpg",
                        "/**/*.jpeg", "/**/*.gif", "/**/*.ico",
                        "/**/*.woff", "/**/*.woff2", "/**/*.ttf", "/**/*.svg",
                        "/**/*.eot"
                )
                .order(1);

        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/pages/**", "/api/**", "/userCtrl/**")
                .excludePathPatterns(
                        "/", "/index.html", "/login.html",
                        "/pages/login.html",
                        "/api/auth/login", "/api/auth/logout", "/api/auth/check","/uploads/**",

                        //排除 /resources/**
                        "/resources/**",
                        "/ckeditor/**",
                        "/vendor/**",
                        "/css/**",
                        "/js/**",

                        "/**/*.css", "/**/*.js", "/**/*.png", "/**/*.jpg",
                        "/**/*.jpeg", "/**/*.gif", "/**/*.ico",
                        "/**/*.woff", "/**/*.woff2", "/**/*.ttf", "/**/*.svg",
                        "/**/*.eot"
                )
                .order(2);
    }
}