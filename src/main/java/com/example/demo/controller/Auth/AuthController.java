package com.example.demo.controller.Auth;
//admin
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;
import com.example.demo.dto.UserInfoDTO;
import com.example.demo.entity.User;
import com.example.demo.mapper.UserMapper;
import com.example.demo.util.PasswordUtil;
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

        String username = request.getUsername();
        String rawPassword = request.getPassword();
        Integer role = request.getRole();

        // 1) 先查是否存在
        List<User> users = userMapper.selectList(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username)
        );
        if (users.isEmpty()) {
            return LoginResponse.builder()
                    .success(false)
                    .message("用户名不存在，请检查用户名是否正确！")
                    .build();
        }

        // 2) 按 username + role 找用户（不要再用 passwd 作为查询条件）
        List<User> candidates = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, username)
                        .eq(User::getRole, role)
        );
        if (candidates.isEmpty()) {
            return LoginResponse.builder()
                    .success(false)
                    .message("密码不正确，请重新输入！")
                    .build();
        }

        User user = candidates.get(0);

        // 3) 校验密码：统一 MD5（兼容旧明文）
        if (!PasswordUtil.matchesStored(user.getPasswd(), rawPassword)) {
            return LoginResponse.builder()
                    .success(false)
                    .message("密码不正确，请重新输入！")
                    .build();
        }

        // 4) 如果库里还是明文，登录成功后自动升级成 MD5 hash
        String desiredHash = PasswordUtil.md5Upper(rawPassword);
        if (user.getPasswd() != null && user.getPasswd().equals(rawPassword)) {
            userMapper.update(null,
                    new LambdaUpdateWrapper<User>()
                            .eq(User::getId, user.getId())
                            .set(User::getPasswd, desiredHash)
            );
            user.setPasswd(desiredHash);
        }

        // 5) Session 登录态
        session.setAttribute("user", user);
        session.setAttribute("isLogin", true);
        session.setAttribute("userId", user.getId());
        session.setAttribute("username", user.getUsername());
        session.setAttribute("role", user.getRole());
        session.setMaxInactiveInterval(30 * 60);

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

    @PostMapping("/logout")
    public LoginResponse logout(HttpSession session) {
        session.invalidate();
        return LoginResponse.builder().success(true).message("退出登录成功").build();
    }

    @GetMapping("/check")
    public LoginResponse checkLogin(HttpSession session) {
        Object isLogin = session.getAttribute("isLogin");
        if (isLogin != null && (Boolean) isLogin) {
            User user = (User) session.getAttribute("user");

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

            return LoginResponse.builder().success(true).message("用户已登录").userInfo(dto).build();
        }
        return LoginResponse.builder().success(false).message("用户未登录").build();
    }
}
