package com.example.demo.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false); // 获取现有session，不创建新的

        // 获取请求的URI和上下文路径
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();

        // 移除上下文路径，获取相对路径
        String path = uri.substring(contextPath.length());

        // 放行登录页面和登录相关接口
        if (path.equals("/") ||
                path.equals("/pages/login.html") ||
                //path.equals("/pages/index.html") || // 如果是直接访问首页，也放行（前端会检查登录状态）
                path.startsWith("/api/auth/login") ||
                path.startsWith("/api/auth/logout") ||
                path.startsWith("/api/auth/check")) {
            return true;
        }

        // 放行静态资源
        if (path.startsWith("/css/") ||
                path.startsWith("/js/") ||
                path.startsWith("/images/") ||
                path.startsWith("/fonts/") ||
                path.startsWith("/favicon.ico") ||
                path.endsWith(".css") ||
                path.endsWith(".js") ||
                path.endsWith(".png") ||
                path.endsWith(".jpg") ||
                path.endsWith(".jpeg") ||
                path.endsWith(".gif") ||
                path.endsWith(".ico") ||
                path.startsWith("/error")) {
            return true;
        }

        // 检查是否登录
        if (session == null) {
            // 如果是API请求，返回JSON错误
            if (path.startsWith("/api/")) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"success\":false,\"message\":\"用户未登录，请先登录！\"}");
            } else {
                // 页面请求，重定向到登录页
                response.sendRedirect(contextPath + "/pages/login.html");
            }
            return false;
        }

        Object isLogin = session.getAttribute("isLogin");
        if (isLogin == null || !(Boolean) isLogin) {
            // 如果是API请求，返回JSON错误
            if (path.startsWith("/api/")) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"success\":false,\"message\":\"用户未登录，请先登录！\"}");
            } else {
                // 页面请求，重定向到登录页
                response.sendRedirect(contextPath + "/pages/login.html");
            }
            return false;
        }

        return true;
    }
}