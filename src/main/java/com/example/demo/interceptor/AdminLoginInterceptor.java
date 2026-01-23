package com.example.demo.interceptor;
//admin
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminLoginInterceptor implements HandlerInterceptor {

    private boolean isStatic(String path) {
        return path.startsWith("/resources/")
                || path.startsWith("/static/")
                || path.matches(".*\\.(css|js|png|jpg|jpeg|gif|ico|woff|woff2|ttf|eot|svg)$");
    }

    private boolean isAdminRole(Object roleObj) {
        if (roleObj == null) return false;
        try {
            int role = Integer.parseInt(roleObj.toString());
            return role == 0 || role == 3;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        String uri = request.getRequestURI();
        String ctx = request.getContextPath();
        String path = uri.substring(ctx.length());

        //  给所有后台页面添加禁止缓存的响应头
        if (path.startsWith("/admin/") && !path.equals("/admin/adminlogin.html")) {
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setDateHeader("Expires", 0);
        }

        // 静态资源直接放行
        if (isStatic(path)) {
            return true;
        }

        // 放行后台登录页 & 登录接口
        if (path.equals("/admin/adminlogin.html")
                || path.startsWith("/admin-api/auth/login")
                || path.startsWith("/admin-api/auth/logout")
                || path.startsWith("/admin-api/auth/check")
                || path.startsWith("/resources/**")) {
            return true;
        }

        HttpSession session = request.getSession(false);
        if (session == null) {
            // 接口：返回 401 JSON（不要 302）
            if (path.startsWith("/admin-api/")) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"success\":false,\"message\":\"管理员未登录\"}");
            } else {
                response.sendRedirect(ctx + "/admin/adminlogin.html");
            }
            return false;
        }

        Object adminLogin = session.getAttribute("adminLogin");
        Object role = session.getAttribute("role");

        if (!(adminLogin instanceof Boolean)
                || !((Boolean) adminLogin)
                || !isAdminRole(role)) {

            // 接口返回 JSON，页面重定向
            if (path.startsWith("/admin-api/")) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"success\":false,\"message\":\"管理员未登录\"}");
            } else {
                response.sendRedirect(ctx + "/admin/adminlogin.html");
            }
            return false;
        }

        return true;
    }
}