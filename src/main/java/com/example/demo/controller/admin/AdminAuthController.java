package com.example.demo.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.demo.dto.AdminLoginRequest;
import com.example.demo.dto.AdminUpdatePasswordRequest;
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
@RequestMapping("/admin-api/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final UserMapper userMapper;

    @PostMapping("/login")
    public LoginResponse login(@RequestBody AdminLoginRequest request, HttpSession session) {
        String username = request.getUsername();
        String rawPassword = request.getPassword();

        if (username == null || username.isBlank() || rawPassword == null || rawPassword.isBlank()) {
            return LoginResponse.builder().success(false).message("请输入用户名和密码").build();
        }

        // 只允许 role=0/3
        List<User> candidates = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, username)
                        .and(q -> q.eq(User::getRole, 0).or().eq(User::getRole, 3))
        );
        if (candidates.isEmpty()) {
            return LoginResponse.builder().success(false).message("密码错误或无管理员权限").build();
        }

        User user = candidates.get(0);

        // MD5 校验（兼容旧明文）
        if (!PasswordUtil.matchesStored(user.getPasswd(), rawPassword)) {
            return LoginResponse.builder().success(false).message("密码错误或无管理员权限").build();
        }

        // 明文自动升级为 hash
        String desiredHash = PasswordUtil.md5Upper(rawPassword);
        if (user.getPasswd() != null && user.getPasswd().equals(rawPassword)) {
            userMapper.update(null,
                    new LambdaUpdateWrapper<User>()
                            .eq(User::getUid, user.getUid())
                            .set(User::getPasswd, desiredHash)
            );
            user.setPasswd(desiredHash);
        }

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
        Object userObj = session.getAttribute("user");

        int role = -1;
        try { role = Integer.parseInt(String.valueOf(roleObj)); } catch (Exception ignored) {}

        boolean ok = (adminLogin instanceof Boolean) && (Boolean) adminLogin && (role == 0 || role == 3);

        if (ok && userObj instanceof User) {
            User user = (User) userObj;

            UserInfoDTO dto = new UserInfoDTO();
            dto.setId(user.getId());
            dto.setUid(user.getUid());
            dto.setUsername(user.getUsername());
            dto.setName(user.getName());
            dto.setRole(user.getRole());
            dto.setRoleName(user.getRoleName());

            return LoginResponse.builder().success(true).message("管理员已登录").userInfo(dto).build();
        }

        return LoginResponse.builder().success(false).message("管理员未登录").build();
    }

    // 修改管理员密码：写入 MD5 hash
    @PostMapping("/updatePassword")
    public LoginResponse updatePassword(@RequestBody AdminUpdatePasswordRequest request, HttpSession session) {
        Object userObj = session.getAttribute("user");
        if (!(userObj instanceof User)) {
            return LoginResponse.builder().success(false).message("请先登录").build();
        }

        User currentUser = (User) userObj;

        if (request.getNewPassword() == null || request.getNewPassword().isBlank()) {
            return LoginResponse.builder().success(false).message("新密码不能为空").build();
        }

        String newPassword = request.getNewPassword().trim();
        if (newPassword.length() < 6) return LoginResponse.builder().success(false).message("密码不能少于6位").build();
        if (newPassword.length() > 16) return LoginResponse.builder().success(false).message("密码不能大于16位").build();

        String newHash = PasswordUtil.md5Upper(newPassword);

        int updated = userMapper.update(null,
                new LambdaUpdateWrapper<User>()
                        .eq(User::getUid, currentUser.getUid())
                        .set(User::getPasswd, newHash)
        );

        if (updated > 0) {
            session.invalidate();
            return LoginResponse.builder().success(true).message("密码修改成功，请重新登录").build();
        }
        return LoginResponse.builder().success(false).message("密码修改失败").build();
    }
}
