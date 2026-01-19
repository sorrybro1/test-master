package com.example.demo.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.demo.entity.Orgrole;
import com.example.demo.entity.User;
import com.example.demo.mapper.AdminUserMapper;
import com.example.demo.mapper.OrgroleMapper;
import com.example.demo.mapper.UserMapper;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin-api/users")
@RequiredArgsConstructor
public class AdminUserController {
    private final AdminUserMapper adminUserMapper;
    private final UserMapper userMapper;
    private final OrgroleMapper orgroleMapper;


    /**
     * 用户管理 - 分页查询（bootstrap-table 服务端分页）
     * POST /admin-api/users/page
     *
     * 参数：
     *   limit, offset（bootstrap-table 标准）
     *   username（模糊）
     *   role（可选）
     *   orgId（可选）
     *
     * 返回：
     *   { total: xxx, rows: [...] }
     */
    @PostMapping("/page")
    public Map<String, Object> pageUsers(
            @RequestParam(value = "limit", defaultValue = "10") int limit,
            @RequestParam(value = "offset", defaultValue = "0") int offset,
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "role", required = false) String role,
            @RequestParam(value = "orgId", required = false) String orgId,
            HttpSession session
    ) {
        // 正常情况下已被 AdminLoginInterceptor 拦截，这里只是兜底
        Object adminLogin = session.getAttribute("adminLogin");
        if (!(adminLogin instanceof Boolean) || !((Boolean) adminLogin)) {
            return Map.of("total", 0, "rows", List.of());
        }

        long total = adminUserMapper.countUsers(username, role, orgId);
        List<User> rows = adminUserMapper.pageUsers(username, role, orgId, limit, offset);

        Map<String, Object> res = new HashMap<>();
        res.put("total", total);
        res.put("rows", rows);
        return res;
    }

    @PostMapping("/one")
    public Map<String, Object> getOne(@RequestParam("uid") String uid, HttpSession session) {
        if (!isAdminLoggedIn(session)) {
            return Map.of("success", false, "msg", "管理员未登录");
        }

        Map<String, Object> one = adminUserMapper.getOneForAdmin(uid);
        if (one == null || one.isEmpty()) {
            return Map.of("success", false, "msg", "用户不存在或无权限查看");
        }

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("one", one);
        return res;
    }

    @PostMapping("/update")
    public Map<String, Object> update(@RequestParam("uid") String uid,
                                      @RequestParam(value = "passwd", required = false) String passwd,
                                      @RequestParam(value = "orgid", required = false) String orgid,
                                      HttpSession session) {
        if (!isAdminLoggedIn(session)) {
            return Map.of("success", false, "msg", "管理员未登录");
        }

        // 只允许学生/老师
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("uid", uid));
        if (user == null || !(user.getRole() == 1 || user.getRole() == 2)) {
            return Map.of("success", false, "msg", "用户不存在或无权限修改（管理员不可被管理）");
        }

        boolean changed = false;

        // 1) 修改密码（若传了）
        if (passwd != null && !passwd.isEmpty()) {
            // 前端已经做了长度校验，这里再兜底一次
            if (passwd.length() < 6 || passwd.length() > 16) {
                return Map.of("success", false, "msg", "密码长度应为6-16位");
            }
            String newHash = md5Upper(passwd);
            int rows = userMapper.update(null,
                    new UpdateWrapper<User>()
                            .eq("uid", uid)
                            .set("passwd", newHash));
            changed = changed || rows > 0;
        }

        // 2) 学生可以改班级（orgid）
        if (user.getRole() == 1 && orgid != null && !orgid.isEmpty()) {
            // 先删旧映射，再插入新映射
            orgroleMapper.delete(new QueryWrapper<Orgrole>().eq("userid", uid));
            Orgrole or = new Orgrole();
            or.setUserid(uid);
            or.setOrgid(orgid);
            orgroleMapper.insert(or);
            changed = true;
        }

        if (!changed) {
            return Map.of("success", false, "msg", "信息未修改");
        }
        return Map.of("success", true, "msg", "修改成功");
    }

    private boolean isAdminLoggedIn(HttpSession session) {
        Object adminLogin = session.getAttribute("adminLogin");
        return (adminLogin instanceof Boolean) && ((Boolean) adminLogin);
    }

    private String md5Upper(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02X", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
