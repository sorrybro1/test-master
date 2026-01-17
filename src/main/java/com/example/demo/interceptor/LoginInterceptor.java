package com.example.demo.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    private boolean isStatic(String path) {
        return path.startsWith("/resources/")
                || path.startsWith("/static/")
                || path.matches(".*\\.(css|js|png|jpg|jpeg|gif|ico|woff|woff2|ttf|eot|svg)$");
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = uri.substring(contextPath.length());
        String path2 = request.getRequestURI();

        //  给所有学生/教师页面添加禁止缓存的响应头
        if (path.startsWith("/pages/") && !path.equals("/pages/login.html")) {
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setDateHeader("Expires", 0);
        }

        // 优先放行后台管理相关路径
        if (path.startsWith("/admin/") || path.startsWith("/admin-api/")) {
            return true;
        }

        // 静态资源放行
        if (isStatic(path2)) {
            return true;
        }

        // 放行登录页面和登录相关接口
        if (path.equals("/") ||
                path.equals("/pages/login.html") ||
                path.startsWith("/api/auth/login") ||
                path.startsWith("/api/auth/logout") ||
                path.startsWith("/api/auth/check")) {
            return true;
        }

        HttpSession session = request.getSession(false);

        // 检查是否登录
        if (session == null) {
            if (path.startsWith("/api/")) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"success\":false,\"message\":\"用户未登录，请先登录！\"}");
            } else {
                response.sendRedirect(contextPath + "/pages/login.html");
            }
            return false;
        }

        Object isLogin = session.getAttribute("isLogin");
        if (isLogin == null || !(Boolean) isLogin) {
            if (path.startsWith("/api/")) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"success\":false,\"message\":\"用户未登录，请先登录！\"}");
            } else {
                response.sendRedirect(contextPath + "/pages/login.html");
            }
            return false;
        }

        return true;
    }
}