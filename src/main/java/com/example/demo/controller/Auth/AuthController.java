package com.example.demo.controller.Auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;
import com.example.demo.dto.UserInfoDTO;
import com.example.demo.entity.User;
import com.example.demo.mapper.UserMapper;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserMapper userMapper;

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request, HttpSession session) {

        // 第一步：根据用户名查询用户是否存在
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername())
        );

        // 用户不存在
        if (users.isEmpty()) {
            return LoginResponse.builder()
                    .success(false)
                    .message("用户名不存在，请检查用户名是否正确！")
                    .build();
        }

        // 第二步：验证密码和角色
        List<User> matchedUsers = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername())
                .eq(User::getPasswd, request.getPassword())
                .eq(User::getRole, request.getRole())
        );

        // 密码或角色不正确
        if (matchedUsers.isEmpty()) {
            return LoginResponse.builder()
                    .success(false)
                    .message("密码不正确，请重新输入！")
                    .build();
        }

        // 登录成功
        User user = matchedUsers.get(0);

        // 将用户信息存入Session
        session.setAttribute("user", user);
        session.setAttribute("isLogin", true);
        session.setAttribute("userId", user.getId());
        session.setAttribute("username", user.getUsername());
        session.setAttribute("role", user.getRole());

        // 设置Session超时时间（30分钟）
        session.setMaxInactiveInterval(30 * 60);

        // 组装返回信息
        UserInfoDTO dto = new UserInfoDTO();
        dto.setId(user.getId());
        dto.setUid(user.getUid());
        dto.setUsername(user.getUsername());
        dto.setName(user.getName());
        dto.setRole(user.getRole());
        dto.setRoleName(user.getRoleName());
        dto.setSex(user.getSex());
        dto.setCollege(user.getCollege());
        dto.setProfessional(user.getProfessional());
        dto.setProf(user.getProf());
        dto.setPhone(user.getPhone());
        dto.setUserClass(user.getU_class());

        return LoginResponse.builder()
                .success(true)
                .message("登录成功")
                .token(null)
                .userInfo(dto)
                .build();
    }

    // 添加退出登录接口
    @PostMapping("/logout")
    public LoginResponse logout(HttpSession session) {
        session.invalidate(); // 清除所有Session
        return LoginResponse.builder()
                .success(true)
                .message("退出登录成功")
                .build();
    }

    // 添加检查登录状态的接口
    @GetMapping("/check")
    public LoginResponse checkLogin(HttpSession session) {
        Object isLogin = session.getAttribute("isLogin");
        if (isLogin != null && (Boolean) isLogin) {
            User user = (User) session.getAttribute("user");
            // 组装用户信息
            UserInfoDTO dto = new UserInfoDTO();
            dto.setId(user.getId());
            dto.setUid(user.getUid());
            dto.setUsername(user.getUsername());
            dto.setName(user.getName());
            dto.setRole(user.getRole());
            dto.setRoleName(user.getRoleName());
            dto.setSex(user.getSex());
            dto.setCollege(user.getCollege());
            dto.setProfessional(user.getProf());
            dto.setPhone(user.getPhone());
            dto.setUserClass(user.getU_class());

            return LoginResponse.builder()
                    .success(true)
                    .message("用户已登录")
                    .userInfo(dto)
                    .build();
        }

        return LoginResponse.builder()
                .success(false)
                .message("用户未登录")
                .build();
    }
}