package com.example.demo.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.dto.AdminLoginRequest;
import com.example.demo.dto.LoginResponse;
import com.example.demo.dto.UserInfoDTO;
import com.example.demo.entity.User;
import com.example.demo.mapper.UserMapper;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 后台管理员登录：只允许角色 0(超级管理员) / 3(管理员)。
 * 使用 Session 鉴权（不引入 Spring Security，方便你先把功能跑通）。
 */
@RestController
@RequestMapping("/admin-api/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final UserMapper userMapper;

    @PostMapping("/login")
    public LoginResponse login(@RequestBody AdminLoginRequest request, HttpSession session) {
        String username = request.getUsername();
        String password = request.getPassword();

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return LoginResponse.builder().success(false).message("请输入用户名和密码").build();
        }

        // 用户是否存在
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (users.isEmpty()) {
            return LoginResponse.builder().success(false).message("用户名不存在").build();
        }

        // 只允许 role=0/3 的管理员登录
        List<User> matched = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, username)
                        .eq(User::getPasswd, password)
                        .and(q -> q.eq(User::getRole, 0).or().eq(User::getRole, 3))
        );

        if (matched.isEmpty()) {
            return LoginResponse.builder().success(false).message("密码错误或无管理员权限").build();
        }

        User user = matched.get(0);

        // Session 标记：后台登录专用
        session.setAttribute("adminLogin", true);
        session.setAttribute("role", user.getRole());
        session.setAttribute("user", user);
        session.setMaxInactiveInterval(30 * 60);

        UserInfoDTO dto = new UserInfoDTO();
        dto.setId(user.getId());
        dto.setUid(user.getUid());
        dto.setUsername(user.getUsername());
        dto.setName(user.getName());
        dto.setRole(user.getRole());
        dto.setRoleName(user.getRoleName());

        return LoginResponse.builder().success(true).message("管理员登录成功").userInfo(dto).build();
    }

    @PostMapping("/logout")
    public LoginResponse logout(HttpSession session) {
        session.invalidate();
        return LoginResponse.builder().success(true).message("退出登录成功").build();
    }

    @GetMapping("/check")
    public LoginResponse check(HttpSession session) {
        Object adminLogin = session.getAttribute("adminLogin");
        Object roleObj = session.getAttribute("role");
        int role = -1;
        try { role = Integer.parseInt(String.valueOf(roleObj)); } catch (Exception ignored) {}

        boolean ok = (adminLogin instanceof Boolean) && (Boolean) adminLogin && (role == 0 || role == 3);
        return ok
                ? LoginResponse.builder().success(true).message("管理员已登录").build()
                : LoginResponse.builder().success(false).message("管理员未登录").build();
    }
}
